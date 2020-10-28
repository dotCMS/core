package com.dotcms.rest;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.RelationshipUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ASSIGN_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_COMMENTS_KEY;

/**
 * Complete populator to populate a contentlet from a map (from a resources form) using all logic needed
 *
 * @author jsanca
 */
public class MapToContentletPopulator  {

    public  static final MapToContentletPopulator INSTANCE = new MapToContentletPopulator();
    private static final String RELATIONSHIP_KEY           = Contentlet.RELATIONSHIP_KEY;
    private static final String LANGUAGE_ID                = "languageId";
    private static final String IDENTIFIER                 = "identifier";

    @CloseDBIfOpened
    public Contentlet populate(final Contentlet contentlet, final Map<String, Object> stringObjectMap) {

        try {

            this.processMap(contentlet, stringObjectMap);
        } catch (DotDataException | DotSecurityException e) {

            throw new DotStateException(e);
        }

        return contentlet;
    }

    protected String getStInode (final Map<String, Object> map) {

        return getContentTypeInode(map);
    }

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

        final String stInode = this.getStInode(map);

        if (UtilMethods.isSet(stInode)) {

            final ContentType type = APILocator.getContentTypeAPI
                    (APILocator.systemUser()).find(stInode);

            if (type != null && InodeUtils.isSet(type.inode())) {
                // basic data
                contentlet.setContentTypeId(type.inode());
                contentlet.setLanguageId(map.containsKey(LANGUAGE_ID)?
                        Long.parseLong(map.get(LANGUAGE_ID).toString()):
                        APILocator.getLanguageAPI().getDefaultLanguage().getId()
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
            }

            this.setIndexPolicy (contentlet, map);
        }
    } // processMap.

    private void setIndexPolicy(final Contentlet contentlet, final Map<String, Object> map) {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        Object indexPolicyValue          = null;
        if (null != request) {

            indexPolicyValue = request.getParameter("indexPolicy");
        }

        indexPolicyValue  = null != indexPolicyValue? indexPolicyValue:
                map.getOrDefault("indexPolicy", IndexPolicyProvider.getInstance().forSingleContent());
        
        contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicyValue));
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

                    contentlet.setStringProperty(field.getVelocityVarName(),  value != null ? value.toString() : null);
                } else if (
                         (field.getFieldType().equals(FieldType.FILE.toString()) || field.getFieldType().equals(FieldType.IMAGE.toString()))
                                 && (value != null && value.toString().startsWith("//"))
                       ) {

                    this.processFileOrImageField(contentlet, value, field);
                } else {
                    APILocator.getContentletAPI()
                            .setContentletProperty(contentlet, field, value);
                }
            }
        }
    } // fillFields.

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
        final Host     host   = APILocator.getHostAPI()
                .findByName(split[0], systemUser, false);

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

    @CloseDBIfOpened
    public List<Category> getCategories (final Contentlet contentlet, final User user,
                                         final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        DotPreconditions.checkNotNull(contentlet, IllegalArgumentException.class, "Invalid Contentlet");
        DotPreconditions.checkNotNull(contentlet.getContentType(), IllegalArgumentException.class, "Invalid Content Type");

        final List<Category> categories = new ArrayList<>();
        final List<Field> fields = new LegacyFieldTransformer(
                APILocator.getContentTypeAPI(APILocator.systemUser()).
                        find(contentlet.getContentType().inode()).fields()).asOldFieldList();

        for (final Field field : fields) {
            if (field.getFieldType().equals(FieldType.CATEGORY.toString())) {

                final String catValue = contentlet.getStringProperty(field.getVelocityVarName());
                if (UtilMethods.isSet(catValue)) {
                    for (final String categoryValue : catValue.split("\\s*,\\s*")) {
                        // take it as catId
                        Category category = APILocator.getCategoryAPI()
                                .find(categoryValue, user, respectFrontendRoles);
                        if (category != null && InodeUtils.isSet(category.getCategoryId())) {
                            categories.add(category);
                        } else {
                            // try it as catKey
                            category = APILocator.getCategoryAPI()
                                    .findByKey(categoryValue, user, respectFrontendRoles);
                            if (category != null && InodeUtils.isSet(category.getCategoryId())) {
                                categories.add(category);
                            } else {
                                // try it as variable
                                // FIXME: https://github.com/dotCMS/dotCMS/issues/2847
                                final HibernateUtil hu = new HibernateUtil(Category.class);
                                hu.setQuery("from " + Category.class.getCanonicalName()
                                        + " WHERE category_velocity_var_name=?");
                                hu.setParam(categoryValue);
                                category = (Category) hu.load();
                                if (category != null && InodeUtils.isSet(category.getCategoryId())) {
                                    categories.add(category);
                                }
                            }
                        }

                    }
                }
            }
        }

        return categories;
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
                contentlet.setInode(StringPool.BLANK);
            } catch (Exception e) {

                Logger.debug(this.getClass(),
                        "can't get existing content for ident " + map.get(IDENTIFIER)
                                + " lang " + contentlet.getLanguageId()
                                + " - creating new one");
            }
        }
    } // processIdentifier.
}
