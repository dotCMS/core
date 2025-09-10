package com.dotcms.rest;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.RelationshipUtil;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.transform.ContentletRelationshipsTransformer;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.collections.CollectionUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.apache.tools.ant.util.ReaderInputStream;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.VARIANT_ID;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ASSIGN_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_COMMENTS_KEY;
import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.COMMA;

/**
 * Complete populator to populate a contentlet from a map (from a resources form) using all logic needed
 *
 * @author jsanca
 */
public class MapToContentletPopulator  {

    public  static final MapToContentletPopulator INSTANCE = new MapToContentletPopulator();
    private static final SecurityUtils securityUtils = new SecurityUtils();
    private static final String RELATIONSHIP_KEY           = Contentlet.RELATIONSHIP_KEY;
    private static final String LANGUAGE_ID                = "languageId";
    private static final String IDENTIFIER                 = "identifier";
    private static final String INDEX_POLICY = "indexPolicy";

    @CloseDBIfOpened
    public Contentlet populate(final Contentlet contentlet, final Map<String, Object> stringObjectMap) {

        try {

            this.processMap(contentlet, stringObjectMap);
        } catch (DotDataException | DotSecurityException e) {

            throw new DotStateException(e);
        }

        return contentlet;
    }

    protected String getStInode (final Map<String, Object> map, final Contentlet contentlet) {

        String stInode = getContentTypeInode(map);

        if (!UtilMethods.isSet(stInode) && null != contentlet.getContentType()) {

            stInode = contentlet.getContentType().inode();
        }

        return stInode;
    }

    @CloseDBIfOpened
    public String getContentTypeInode (final Map<String, Object> map) {

        String stInode = (String) map.get(Contentlet.STRUCTURE_INODE_KEY);

        if (!UtilMethods.isSet(stInode)) {

            final String stName = (String) map.get(Contentlet.STRUCTURE_NAME_KEY);
            if (UtilMethods.isSet(stName)) {
                stInode = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(stName)
                        .getInode();
            } else {
                final String contentType = (String) map.get(Contentlet.CONTENT_TYPE_KEY);
                if (UtilMethods.isSet(contentType)) {
                    stInode = CacheLocator.getContentTypeCache()
                            .getStructureByVelocityVarName(contentType).getInode();
                }
            }
        }

        return stInode;
    }

    private void processMap(final Contentlet contentlet,
                              final Map<String, Object> map) throws DotDataException, DotSecurityException {

        final String stInode = this.getStInode(map, contentlet);

        if (UtilMethods.isSet(stInode)) {

            final ContentType type = APILocator.getContentTypeAPI
                    (APILocator.systemUser()).find(stInode);

            if (type != null && InodeUtils.isSet(type.inode())) {
                // basic data
                contentlet.setContentTypeId(type.inode());

                contentlet.setLanguageId(map.containsKey(LANGUAGE_ID) ?
                        Long.parseLong(map.get(LANGUAGE_ID).toString()) :
                        fallbackLanguage(contentlet)
                    );

                this.processIdentifier(contentlet, map);

                this.processWorkflow(contentlet, map);

                // build a field map for easy lookup
                final Map<String, Field> fieldMap = new HashMap<>();
                for (final Field field : new LegacyFieldTransformer(type.fields()).asOldFieldList()) {
                    fieldMap.put(field.getVelocityVarName(), field);
                }

                // look for relationships
                contentlet.setProperty(RELATIONSHIP_KEY, this.getContentletRelationships(map, contentlet));

                // fill fields
                this.fillFields(contentlet, map, type, fieldMap);

                contentlet.setVariantId(map.get(VARIANT_ID) != null ? map.get(VARIANT_ID).toString() : contentlet.getVariantId());

                //fill up disabledWYSIWYG
                contentlet.setDisabledWysiwyg(map.get("disabledWYSIWYG") != null ? (List<String>) map.get("disabledWYSIWYG") : new ArrayList<>());
            }


            this.setIndexPolicy (contentlet, map);
        }
    } // processMap.

    /**
     * if we fail to establish a language param then we need to supply a fallback
     * @param contentlet
     * @return
     */
    private long fallbackLanguage(Contentlet contentlet) {
        return Try.of(() -> {
            if(contentlet.getLanguageId() > 0){
                return contentlet.getLanguageId();
            }
            if (UtilMethods.isSet(contentlet.getInode())) {
                return APILocator.getContentletAPI().find(contentlet.getInode(), APILocator.systemUser(), false).getLanguageId();
            }
            //We shouldn't be relying on the default lang if we know the inode (which has a lang of its own)
            //That's why we're trying other options first as fallback
            return APILocator.getLanguageAPI().getDefaultLanguage().getId();
        }).getOrElse(()->0L);
    }

    private void setIndexPolicy(final Contentlet contentlet, final Map<String, Object> map) {

        contentlet.setIndexPolicy(recoverIndexPolicy(map,
                IndexPolicyProvider.getInstance().forSingleContent(),
                HttpServletRequestThreadLocal.INSTANCE.getRequest()));
    }

    public static IndexPolicy recoverIndexPolicy (final Map<String, Object> map, final IndexPolicy defaultIndexPolicy,
                                                  final HttpServletRequest request) {

        Object indexPolicyValue          = null;
        if (null != request) {

            indexPolicyValue = request.getParameter(INDEX_POLICY);
        }

        indexPolicyValue  = null != indexPolicyValue? indexPolicyValue:
                map.getOrDefault(INDEX_POLICY, defaultIndexPolicy);

        return IndexPolicy.parseIndexPolicy(indexPolicyValue);
    }


    private void processWorkflow(final Contentlet contentlet, final Map<String,Object> map) {
        if(map.containsKey(WORKFLOW_ASSIGN_KEY)) {
            contentlet.setStringProperty(WORKFLOW_ASSIGN_KEY, String.valueOf(map.get(WORKFLOW_ASSIGN_KEY)));
        }
        if(map.containsKey(WORKFLOW_COMMENTS_KEY)) {
            contentlet.setStringProperty(WORKFLOW_COMMENTS_KEY, String.valueOf(map.get(WORKFLOW_COMMENTS_KEY)));
        }
    }

    private void fillFields(final Contentlet contentlet,
                            final Map<String, Object> map,
                            final ContentType type,
                            final Map<String, Field> fieldMap) {

        for (final Map.Entry<String, Object> entry : map.entrySet()) {

            final String key = entry.getKey();
            final Object value = entry.getValue();
            final Field field = fieldMap.get(key);

            if (field != null) {
                if (field.getFieldType().equals(FieldType.HOST_OR_FOLDER.toString())) {
                    // it can be hostId, folderId, hostname, hostname:/folder/path
                    this.processHostOrFolderField(contentlet, type, value);
                } else if (field.getFieldType().equals(FieldType.CATEGORY.toString())) {
                   if(value instanceof String){
                       contentlet.setStringProperty(field.getVelocityVarName(), value.toString());
                   } else {
                       contentlet.setProperty(field.getVelocityVarName(), value);
                   }
                } else if (
                         (field.getFieldType().equals(FieldType.FILE.toString()) || field.getFieldType().equals(FieldType.IMAGE.toString()))
                                 && (value != null && value.toString().startsWith("//"))
                       ) {

                    this.processFileOrImageField(contentlet, value, field);
                } else if ((BinaryField.class.getName().equals(field.getFieldType()) ||
                        LegacyFieldTypes.BINARY.legacyValue().equals(field.getFieldType()))
                        && null != value && value instanceof Map) {

                    this.processPlainValueForBinaryField(map, field, value, contentlet);
                } else {
                    APILocator.getContentletAPI()
                            .setContentletProperty(contentlet, field, value);
                }
            }
        }

    } // fillFields.

    private static void processPlainValueForBinaryField(final Map<String, Object> map,
                                                        final Field field,
                                                        final Object value,
                                                        final Contentlet contentlet) {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null != request) {

            try {

                final Map valueMap = (Map) value;

                if (valueMap.containsKey("content")) {

                    final String fileName = FileUtil.sanitizeFileName((String) valueMap.getOrDefault("fileName", map.getOrDefault("fileName",
                            map.getOrDefault("title", "unknown"))));
                    if (fileName == null || fileName.startsWith(".") || fileName.contains("/.")) {

                        throw new IllegalArgumentException("Invalid FileName: " + fileName);
                    }

                    securityUtils.validateFile(fileName);

                    final String content  = valueMap.get("content").toString(); // todo: we need to discuss how to validate the file content
                    final DotTempFile dotTempFile = APILocator.getTempFileAPI().createTempFile(FileUtil.sanitizeFileName(fileName), request,
                            new ReaderInputStream(new StringReader(content), UtilMethods.getCharsetConfiguration()));
                    if (null != dotTempFile) {
                        APILocator.getContentletAPI()
                                .setContentletProperty(contentlet, field, dotTempFile.id);
                    } else {

                        Logger.debug(MapToContentletPopulator.class, ()-> "The file: " + fileName + "couldn't be created");
                    }
                }
            } catch (DotSecurityException e) {

                Logger.debug(MapToContentletPopulator.class, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void processFileOrImageField(final Contentlet contentlet,
                                         final Object value,
                                         final Field field) {
        try {

            final String str      = value.toString().substring(2);
            final String hostname = str.substring(0, str.indexOf('/'));
            final String uri      = str.substring(str.indexOf('/'));
            final Host host       = APILocator.getHostAPI().findByName(hostname,
                    APILocator.getUserAPI().getSystemUser(), false);

            if (host != null && InodeUtils.isSet(host.getIdentifier())) {

                final Identifier ident = APILocator.getIdentifierAPI().find(host, uri);

                if (ident != null && InodeUtils.isSet(ident.getId())) {

                    contentlet.setStringProperty(field.getVelocityVarName(),
                            ident.getId());
                    return;
                }
            }

            throw new Exception("asset " + value + " not found");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    } // processFileOrImageField.

    private void processHostOrFolderField(final Contentlet contentlet,
                                          final ContentType type,
                                          final Object value) {

        try {

            final User systemUser = APILocator.getUserAPI().getSystemUser();
            Host host             = APILocator.getHostAPI()
                    .find(value.toString(), systemUser, false);

            if (host != null && InodeUtils.isSet(host.getIdentifier())) {

                contentlet.setHost(host.getIdentifier());
            } else {

                Folder folder = null;
                try {
                    folder = APILocator.getFolderAPI()
                            .find(value.toString(), systemUser, false);
                } catch (Exception ex) { /*Quiet*/ }

                if (folder != null && InodeUtils.isSet(folder.getInode())) {
                    contentlet.setFolder(folder.getInode());
                    contentlet.setHost(folder.getHostId());
                } else {
                    if (value.toString().contains(":")) {

                        this.processSeveralHostsOrFolders(contentlet, type, value, systemUser);
                    } else {

                        host = APILocator.getHostAPI()
                                .findByName(value.toString(), systemUser, false);

                        if (host != null && InodeUtils.isSet(host.getIdentifier())) {

                            contentlet.setHost(host.getIdentifier());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // just pass
        }
    }

    private void processSeveralHostsOrFolders(final Contentlet contentlet,
                                              final ContentType type,
                                              final Object value,
                                              final User systemUser) throws DotDataException, DotSecurityException {
        Folder folder         = null;
        final String[] split  = value.toString().split(":");
        Host     host   = APILocator.getHostAPI()
                .findByName(split[0], systemUser, false);

        if (null == host || !InodeUtils.isSet(host.getIdentifier())) {

            host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        }

        if (host != null && InodeUtils.isSet(host.getIdentifier())) {

            folder = APILocator.getFolderAPI()
                    .findFolderByPath(split[1], host, systemUser,
                            false);
            if (folder != null && InodeUtils.isSet(folder.getInode())) {

                contentlet.setHost(host.getIdentifier());
                contentlet.setFolder(folder.getInode());

                if (BaseContentType.FILEASSET.equals(type.baseType())) {

                    final StringBuilder fileUri = new StringBuilder()
                            .append(split[1].endsWith("/") ? split[1]: split[1] + "/")
                            .append(contentlet.getMap().get("fileName").toString());

                    final Identifier existingIdent = APILocator
                            .getIdentifierAPI()
                            .find(host, fileUri.toString());

                    if (existingIdent != null && UtilMethods.isSet(existingIdent.getId())
                            && UtilMethods.isSet(contentlet.getIdentifier())) {

                        contentlet.setIdentifier(existingIdent.getId());
                    }
                }
            }
        }
    } // processSeveralHostsOrFolders.

    /**
     * @deprecated Use {@link MapToContentletPopulator#fetchCategories(Contentlet, User, boolean)} instead
     * @param contentlet
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Deprecated
    public List<Category> getCategories (final Contentlet contentlet, final User user,
                                         final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        DotPreconditions.checkNotNull(contentlet, IllegalArgumentException.class, "Invalid Contentlet");
        DotPreconditions.checkNotNull(contentlet.getContentType(), IllegalArgumentException.class, "Invalid Content Type");

        return internalFetchCategories(contentlet, user, respectFrontendRoles).orElseGet(
                ImmutableList::of);
    }

    /**
     * The List of Categories generated by this method is what will be set directly into the checkin method
     * Meaning that if this method generates an empty list, The categories in the contentlet will be wiped-out. An empty Optional means nothing should be sent down into the checkin pipeline
     * @param contentlet
     * @param user
     * @param respectFrontendRoles
     * @return Empty Optional means no categories were found if something comes back within the Optional that's what must be applied there.
     * If an empty Collection of categories is returned that means we need to wipe out the categories from the contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public Optional<List<Category>> fetchCategories (final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        DotPreconditions.checkNotNull(contentlet, IllegalArgumentException.class, "Invalid Contentlet");
        DotPreconditions.checkNotNull(contentlet.getIdentifier(), IllegalArgumentException.class, "A valid identifier is mandatory");
        DotPreconditions.checkNotNull(contentlet.getContentType(), IllegalArgumentException.class, "A valid Content Type or stInode is mandatory.");

        return internalFetchCategories(contentlet, user, respectFrontendRoles);
    }

    /**
     * This basically works at the contentlet level to extract the respective field value that represents the category
     * @param contentlet
     * @param user
     * @param respectFrontendRoles
     * @return
     */
    @CloseDBIfOpened
    private Optional<List<Category>> internalFetchCategories(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final ImmutableMap.Builder<CategoryField, Set<Category>> builder = ImmutableMap.builder();
        final Set<CategoryField> fields = contentlet.getContentType().fields(CategoryField.class)
                .stream().map(field -> (CategoryField) field)
                .collect(Collectors.toSet());
        if (fields.isEmpty()) {
            return Optional.empty();
        }

        for (final CategoryField field : fields) {
            final Map<String, Object> map = contentlet.getMap();
            final Object object = map.get(field.variable());
            if (object instanceof Collection) {
                final Collection<?> list = (Collection<?>) object;
                if(list.isEmpty()){
                    //upfront en empty list must be interpreted as an attempt to wipe out categories from the current field.
                    builder.put(field, ImmutableSet.of());
                } else {
                    final String joinedCategories = list.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(COMMA));
                    builder.put(field, getCategoriesFromStringValue(joinedCategories, user, respectFrontendRoles));
                }
                continue;
            }
            if (object instanceof String) {
                final String value =  (String) object;
                if(!"null".equalsIgnoreCase(value)){
                   builder.put(field, getCategoriesFromStringValue(value, user, respectFrontendRoles));
                }
                continue;
            }
            if (object == null && map.containsKey(field.variable())) {
                //something was set to null on purpose
                //The requirement says that null must not even be considered
                //We should only remove a category if an empty array is passed
                map.remove(field.variable());
                continue;
            }
            if (null == object || JSONObject.NULL == object) {
                //the value wasn't set. Just ignore it.
                continue;
            }

            throw new IllegalArgumentException(String.format(
                    "Attempt to set unrecognized object [%s] as category. This field only accepts arrays like ['cat-key','cat-var','cat-inode'] or a string like 'cat-key','cat-var','cat-inode' ",
                    object));
        }
        final Map<CategoryField, Set<Category>> existingCategories = fetchExistingCategories(contentlet, user);
        final Map<CategoryField, Set<Category>> fetchedCategories = builder.build();
        final Map<CategoryField, Set<Category>> merged = merge(existingCategories, fetchedCategories);

        if(merged.isEmpty()){
            return Optional.empty();
        }
        final List<Category> collect = merged.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        return Optional.of(collect);
    }

    /**
     * here we have two maps first one comes from the db the second one comes from the request passed
     * if a category isn't include on the request. it should remind the same. it doesnt not need to be touched or removed
     * therefore it must be included on the result of this maps operation
     * @param existing saved categories
     * @param fetched fetched from the user request
     * @return
     */
    private final Map<CategoryField, Set<Category>> merge(final Map<CategoryField, Set<Category>> existing, final Map<CategoryField, Set<Category>> fetched){
        if(existing.isEmpty()){
            return fetched;
        }
        final Map<CategoryField, Set<Category>> builder = new HashMap<>(existing);
        existing.forEach((categoryField, categories) -> {
            if(fetched.containsKey(categoryField)){
               builder.put(categoryField,fetched.get(categoryField));
            }
        });

        return builder;
    }

    /**
     * given a List and a string representation of a category
     * This will try to find different criterion to get the respective category
     * @param stringValue
     * @param user
     * @param respectFrontendRoles
     */
    private Set<Category> getCategoriesFromStringValue(final String stringValue, final User user, boolean respectFrontendRoles) {

        if (UtilMethods.isNotSet(stringValue)) {
            throw new IllegalArgumentException(String.format("Unable to resolve the raw string value [%s] as a valid form of category name or identifier.",stringValue));
        }

            final Set<Category> categories = new HashSet<>();
            final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
            final String[] parts = stringValue.split("\\s*,\\s*");
            for (final String categoryValue : parts) {

                // try it as catId
                Category category = Try.of(
                                () -> categoryAPI.find(categoryValue, user, respectFrontendRoles))
                        .getOrNull();
                if (category != null && InodeUtils.isSet(category.getCategoryId())) {
                    categories.add(category);
                    Logger.debug(MapToContentletPopulator.class,
                            String.format(" value [%s] resolved as a valid Category-Inode.",
                                    categoryValue));
                    continue;
                }

                // try it as catKey
                category = Try.of(
                                () -> categoryAPI.findByKey(categoryValue, user, respectFrontendRoles))
                        .getOrNull();
                if (category != null && InodeUtils.isSet(category.getCategoryId())) {
                    categories.add(category);
                    Logger.debug(MapToContentletPopulator.class,
                            String.format(" value [%s] resolved as a valid Category-Key.",
                                    categoryValue));
                    continue;
                }

                // try it as variable
                category = Try.of(
                                () -> categoryAPI.findByVariable(categoryValue, user, respectFrontendRoles))
                        .getOrNull();
                if (category != null && InodeUtils.isSet(category.getCategoryId())) {
                    categories.add(category);
                    Logger.debug(MapToContentletPopulator.class,
                            String.format(" value [%s] resolved as a valid Category-Variable.",
                                    categoryValue));
                    continue;
                }

                throw new IllegalArgumentException(String.format("Unable to resolve the string value [%s] as a valid form of category name/var/key or identifier.", categoryValue));
            }
        return categories;
    }

    /**
     * Let's say we have a CT with 2 Category fields we pass an empty list (to erase the first field) and we ignore the second field
     * We need to be able to reconstruct the contents on the second field. Cuz the list of categories expected by the checkin method
     * Persists what is passed to it. It doesn't do any internal arithmetic to reconstruct the other categories.
     * We need to do such logic here. And that is why we need to be able to fetch the exising categories
     * @param contentlet
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private Map<CategoryField, Set<Category>> fetchExistingCategories(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {

        final ImmutableMap.Builder<CategoryField, Set<Category>> builder = ImmutableMap.builder();
        //Since we can not rely there will always be a inode on the incoming request  we must lookup the current working contentlet
        final Contentlet workingContentlet = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(), false,
                        contentlet.getLanguageId(), APILocator.systemUser(), false);
        if(null != workingContentlet){
            final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
            //Parents are the categories assigned to the contentlet. But they this does not tell you how they are assigned through the fields
            final List<Category> parents = categoryAPI.getParents(workingContentlet, user, false);
            final List<CategoryField> categoryFields = workingContentlet.getContentType()
                    .fields(CategoryField.class).stream().map(field -> (CategoryField) field)
                    .collect(Collectors.toList());
            for (final CategoryField categoryField : categoryFields) {
                //We need to know how categories are saved by field on the given contentlet
                final Set<Category> selected = findSelected(categoryField, parents, user);
                if(null != selected){
                   builder.put(categoryField, selected);
                }
            }
        }
        return builder.build();
    }

    /**
     * This is basically tells how we bind categories to the fields they belong to
     * @param categoryField
     * @param categories
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private Set<Category> findSelected(final CategoryField categoryField, final List<Category> categories,
            final User user)
            throws DotDataException, DotSecurityException {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final Category parent = categoryAPI.find(categoryField.values(), user,
                false);

        if (categoryAPI.canUseCategory(parent, user, false)) {
            return categories.stream()
                    //in order to make sure we include all categories We need to take into account also the actual parent and verify it against the current category
                    .filter(category -> category.equals(parent) || categoryAPI.isParent(category, parent, user, false))
                    .collect(Collectors.toSet());
        }
        return null;
    }

    /**
     * @deprecated This method should not be used because it does not consider self related content.
     * We suggest to manually create ContentletRelationships
     * @param contentlet
     * @param contentRelationships
     * @return
     */
    public ContentletRelationships getContentletRelationshipsFromMap(final Contentlet contentlet,
                                                                      final Map<Relationship, List<Contentlet>> contentRelationships) {

        return new ContentletRelationshipsTransformer(contentlet, contentRelationships).findFirst();
    }

    private ContentletRelationships getContentletRelationships(final Map<String, Object> map,
                                                                       final Contentlet contentlet) throws DotDataException {

        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        final ContentType contentType = contentlet.getContentType();
        ContentletRelationships contentletRelationships = null;

        for (final Relationship relationship : relationshipAPI.byContentType(contentType)) {

            //searches for legacy relationships(those with legacy relation type value) and field
            //relationships (those with period. For example: News.comments)
            final Map<String, String> queryMap = getRelationshipQuery(contentType, map, relationship);

            for (Entry<String, String> queryEntry: queryMap.entrySet()){
                String query = queryEntry.getValue();

                if (UtilMethods.isSet(query)) {

                    try {

                        final List<Contentlet> contentlets = RelationshipUtil
                                .filterContentlet(contentlet.getLanguageId(), query,
                                        APILocator.getUserAPI().getSystemUser(), false);

                        if (contentlets.size() > 0) {

                            if (contentletRelationships == null){
                                contentletRelationships = new ContentletRelationships(contentlet);
                            }

                            addContentletRelationships(contentlet,
                                    relationshipAPI, contentletRelationships,
                                    relationship, queryEntry, contentlets);
                        }

                        Logger.info(this, "got " + contentlets.size() + " related contents");
                    } catch (Exception e) {

                        Logger.warn(this, e.getMessage(), e);
                    }
                } else if (query != null && query.trim().equals("")){

                    if (contentletRelationships == null){
                        contentletRelationships = new ContentletRelationships(contentlet);
                    }

                    //wipe out relationship
                    addContentletRelationships(contentlet,
                            relationshipAPI, contentletRelationships,
                            relationship, queryEntry, new ArrayList<>());
                }
            }
        }

        return contentletRelationships;
    } // getContentletRelationships.

    @NotNull
    private void addContentletRelationships(Contentlet contentlet, RelationshipAPI relationshipAPI,
            ContentletRelationships contentletRelationships, Relationship relationship,
            Entry<String, String> queryEntry, List<Contentlet> contentlets) {

        final boolean isParent =
                relationshipAPI.sameParentAndChild(relationship) ? queryEntry.getKey()
                        .equalsIgnoreCase(relationship.getChildRelationName())
                        : relationshipAPI.isParent(relationship, contentlet.getContentType());

        final ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                relationship, isParent);
        records.setRecords(contentlets);
        contentletRelationships.getRelationshipsRecords().add(records);

    }

    /**
     * Gets the query to be used for filtering related contentlet. It supports legacy and field relationships
     * @param contentType
     * @param fieldsMap
     * @param relationship
     * @return
     */
    private Map<String, String> getRelationshipQuery(final ContentType contentType,
            final Map<String, Object> fieldsMap, final Relationship relationship) {
        final String relationTypeValue = relationship.getRelationTypeValue();

        final Map<String, String> queryMap = new HashMap<>();

        //Important: On this method fieldsMap.contains is not valid
        if (!relationship.isRelationshipField() && fieldsMap.get(relationTypeValue) != null) {
            //returns a legacy relationship
            queryMap.put(relationTypeValue, (String) fieldsMap.get(relationTypeValue));
        } else if (relationship.isRelationshipField()) {
            //returns a field relationship if exists
            final Set<String> relationshipFields = contentType.fields().stream()
                    .filter(field -> field instanceof RelationshipField)
                    .map(field -> field.variable()).collect(
                            Collectors.toSet());
            if (fieldsMap.get(relationship.getChildRelationName()) != null && relationshipFields
                    .contains(relationship.getChildRelationName())) {
                queryMap.put(relationship.getChildRelationName(),
                        (String) fieldsMap.get(relationship.getChildRelationName()));
            }

            if (fieldsMap.get(relationship.getParentRelationName()) != null && relationshipFields
                    .contains(relationship.getParentRelationName())) {
                queryMap.put(relationship.getParentRelationName(),
                        (String) fieldsMap.get(relationship.getParentRelationName()));
            }
        }

        return queryMap;
    }

    private void processIdentifier(final Contentlet contentlet,
                                   final Map<String, Object> map) {

        if (map.containsKey(IDENTIFIER)) {

            contentlet.setIdentifier(String.valueOf(map.get(IDENTIFIER)));
            try {

                final Contentlet existing = APILocator.getContentletAPI()
                        .findContentletByIdentifier((String) map.get(IDENTIFIER), false,
                                contentlet.getLanguageId(),
                                APILocator.getUserAPI().getSystemUser(), false);
                APILocator.getContentletAPI().copyProperties(contentlet, existing.getMap());
                contentlet.setInode(BLANK);
            } catch (Exception e) {

                Logger.debug(this.getClass(),
                        "can't get existing content for ident " + map.get(IDENTIFIER)
                                + " lang " + contentlet.getLanguageId()
                                + " - creating new one");
            }
        }
    } // processIdentifier.
}
