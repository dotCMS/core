package com.dotcms.rest;

import com.dotcms.api.web.HttpServletRequestImpersonator;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategy;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategyResolver;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;

/**
 * Encapsulate helper method for the {@link com.dotcms.rest.ContentResource}
 * @author jsanca
 */
public class ContentHelper {

    private final MapToContentletPopulator mapToContentletPopulator;
    private final IdentifierAPI identifierAPI;
    private final BaseTypeToContentTypeStrategyResolver baseTypeToContentTypeStrategyResolver =
            BaseTypeToContentTypeStrategyResolver.getInstance();

    public static final String[] ignoreFields = {"disabledWYSIWYG", "lowIndexPriority"};

    private static class SingletonHolder {
        private static final ContentHelper INSTANCE = new ContentHelper();
    }

    public static ContentHelper getInstance() {
        return ContentHelper.SingletonHolder.INSTANCE;
    }

    private ContentHelper() {
        this(  APILocator.getIdentifierAPI(),
                MapToContentletPopulator.INSTANCE);
    }

    @VisibleForTesting
    public ContentHelper(final IdentifierAPI identifierAPI,
                            final MapToContentletPopulator mapToContentletPopulator) {

        this.identifierAPI            = identifierAPI;
        this.mapToContentletPopulator = mapToContentletPopulator;
    }

    /**
     * Populate the contentlet from the map will all logic inside.
     * @param contentlet      {@link Contentlet}
     * @param stringObjectMap Map
     * @return Contentlet
     */
    public Contentlet populateContentletFromMap(final Contentlet contentlet,
                                                final Map<String, Object> stringObjectMap) {

        return this.mapToContentletPopulator.populate(contentlet, stringObjectMap);
    }


    /**
     * If the contentletMap does not have any content type assigned and a base type is set, tries to figure out the content type using the base type
     * @param contentletMap {@link Map}
     * @param user {@link User}
     */
    public void checkOrSetContentType(final Map<String, Object> contentletMap, final User user) {

        this.checkOrSetContentType(contentletMap, user, Collections.emptyList());
    }

    /**
     * If the contentletMap does not have any content type assigned and a base type is set, tries to figure out the content type using the base type
     * @param contentletMap {@link Map}
     * @param user {@link User}
     */
    public void checkOrSetContentType(final Map<String, Object> contentletMap, final User user, final List<File> binaryFiles) {

        if (!this.hasContentType(contentletMap) && contentletMap.containsKey(Contentlet.BASE_TYPE_KEY)) {

            final String baseType = contentletMap.get(Contentlet.BASE_TYPE_KEY).toString();
            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if (UtilMethods.isSet(baseType) && null != request) {

                this.tryToSetContentType(contentletMap, user, baseType, request, binaryFiles);
            }
        }
    }

    private boolean hasContentType (final Map<String, Object> contentletMap) {
        return  contentletMap.containsKey(Contentlet.CONTENT_TYPE_KEY)    ||
                contentletMap.containsKey(Contentlet.STRUCTURE_INODE_KEY) ||
                contentletMap.containsKey(Contentlet.STRUCTURE_NAME_KEY);
    }


    private void tryToSetContentType(final Map<String, Object> contentletMap,
                                     final User user, final String baseType, final HttpServletRequest request,
                                     final List<File> binaryFiles) {

        final Host host = Try.of(()-> WebAPILocator.getHostWebAPI().getCurrentHost(request)).getOrNull();
        final BaseContentType baseContentType = BaseContentType.getBaseContentType(baseType);
        final Optional<BaseTypeToContentTypeStrategy> typeStrategy =
                this.baseTypeToContentTypeStrategyResolver.get(baseContentType);

        if (null != host && typeStrategy.isPresent()) {

            final String sessionId = request!=null && request.getSession(false)!=null? request.getSession().getId() : null;
            final Optional<ContentType> contentTypeOpt = typeStrategy.get().apply(baseContentType,
                    Map.of("user", user, "host", host,
                            "contentletMap", contentletMap, "binaryFiles", binaryFiles,
                            "accessingList", Arrays.asList(user.getUserId(),
                                    APILocator.getTempFileAPI().getRequestFingerprint(request), sessionId)));

            if (contentTypeOpt.isPresent()) {

                Logger.debug(this, ()-> "For the base type: " + baseType + " resolved the content type: "
                        + contentTypeOpt.get().variable());
                contentletMap.put(Contentlet.CONTENT_TYPE_KEY, contentTypeOpt.get().variable());
            } else{
                final String errorMsg = Try.of(() -> LanguageUtil.get(user.getLocale(),
                        "contentType.not.resolved.baseType", user.getUserId(), baseType)).getOrElse("Content-Type could not be resolved based on base type");
                throw new DotContentletValidationException(errorMsg);
            }
        }
    }

    /**
     * Serves as an Entry point to the DotTransformerBuilder
     * @See DotTransformerBuilder
     * @param contentlet {@link Contentlet} original contentlet to hydrate, won't be modified.
     * @return Contentlet returns a contentlet, if there is something to add will create a new instance based on the current one in the parameter and the new attributes, otherwise will the same instance
     */
    public Contentlet hydrateContentlet(final Contentlet contentlet) {
        return new DotTransformerBuilder().contentResourceOptions(false).content(contentlet).build().hydrate().get(0);
    } // hydrateContentlet.

    /**
     * Gets if possible the url associated to this asset contentlet
     * @param contentlet {@link Contentlet}
     * @return String the url, null if can not get
     */
    public String getUrl (final Contentlet contentlet) {

        if(hasUrlField(contentlet)){
            if(IsNeitherPageOrFileAsset(contentlet)){
                return contentlet.getStringProperty(URL_FIELD);
            }
        }
        return this.getUrl(contentlet.getMap().get( ContentletForm.IDENTIFIER_KEY ));
    } // getUrl.

    /**
     * Determines if a contentlet is a regular content (neither a file asset nor an HTML page).
     * This method is used to check the type of a contentlet when processing URLs.
     *
     * @param contentlet The contentlet to check
     * @return boolean True if the contentlet is regular content (neither a file asset nor an HTML page), false otherwise
     */
    private static boolean IsNeitherPageOrFileAsset(Contentlet contentlet) {
        return !contentlet.isFileAsset() && !contentlet.isHTMLPage();
    }

    /**
     * Checks if a contentlet has a URL field in its content type and if that URL field has a non-null value.
     * This method is used to determine if a contentlet has a valid URL property that can be accessed.
     *
     * @param contentlet The contentlet to check
     * @return boolean True if the contentlet has a URL field with a non-null value, false otherwise
     */
    private static boolean hasUrlField(Contentlet contentlet) {
        return contentlet.getContentType().fieldMap((key) -> URL_FIELD) != null &&
                contentlet.getStringProperty(URL_FIELD) != null;
    }


    /**
     * Gets if possible the url associated to this asset identifier
     * @param identifierObj {@link Object}
     * @return String the url, null if can not get
     */
    public String getUrl ( final Object identifierObj) {

        String url = null;
        if ( identifierObj != null ) {
            try {

                final Identifier identifier = this.identifierAPI.find(  (String) identifierObj );
                url = ( UtilMethods.isSet( identifier ) && UtilMethods.isSet( identifier.getId() ) )?
                        identifier.getURI():null;
            } catch ( DotDataException e ) {
                Logger.error( this.getClass(), "Unable to get Identifier with id [" + identifierObj + "]. Could not get the url", e );
            }
        }

        return url;
    } // getUrl.

    /**
     *
     * @param request
     * @param response
     * @param user
     * @param query
     * @param offset
     * @param limit
     * @param sort
     * @param pageMode
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public SearchView pullContent(final HttpServletRequest request,
                                  final HttpServletResponse response, final User user,
                                  final String query, final int offset, final int limit,
                                  final String sort, final PageMode pageMode) throws DotDataException, DotSecurityException {
        return pullContent(request, response, query, user, pageMode, offset, limit, sort,
                StringPool.BLANK, "false", user, -1, -1, false);
    }

    /**
     *
     * @param request
     * @param response
     * @param query
     * @param resultsSize
     * @param userForPull
     * @param pageMode
     * @param offset
     * @param limit
     * @param sort
     * @param tmDate
     * @param render
     * @param user
     * @param depth
     * @param language
     * @param allCategoriesInfo
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public SearchView pullContent(HttpServletRequest request, HttpServletResponse response,
                                   String query, User userForPull, PageMode pageMode, int offset, int limit,
                                   String sort, String tmDate, String render, User user, int depth, long language,
                                   boolean allCategoriesInfo) throws DotDataException, DotSecurityException {
        JSONObject resultJson = new JSONObject();
        long startAPISearchPull = 0;
        long afterAPISearchPull = 0;
        long startAPIPull = 0;
        long afterAPIPull = 0;
        long resultsSize = 0;
        if (UtilMethods.isSet(query)) {
            startAPISearchPull = Calendar.getInstance().getTimeInMillis();
            resultsSize = APILocator.getContentletAPI().indexCount(query, userForPull, pageMode.respectAnonPerms);
            afterAPISearchPull = Calendar.getInstance().getTimeInMillis();

            startAPIPull = Calendar.getInstance().getTimeInMillis();
            List<Contentlet> contentlets = ContentUtils.pull(query, offset, limit, sort, userForPull, tmDate, pageMode.respectAnonPerms);
            resultJson = getJSONObject(contentlets, request, response, render, user, depth, pageMode.respectAnonPerms, language, pageMode.showLive, allCategoriesInfo);

            afterAPIPull = Calendar.getInstance().getTimeInMillis();
            if (contentlets.isEmpty() && offset <= resultsSize) {
                resultsSize = 0;
            }
        }

        final long queryTook = afterAPISearchPull - startAPISearchPull;
        final long contentTook = afterAPIPull - startAPIPull;
        return new SearchView(resultsSize, queryTook, contentTook, new JsonObjectView(resultJson));
    }


    /**
     * Creates a JSON Object off of a list of Contentlets. Their representation will be set to the {@code contentlets}
     * attribute in the JSON response. In case dotCMS cannot transform a piece of Content into a valid JSON Object, it
     * will just not be included in the result object.
     *
     * @param contentletList       The list of {@link Contentlet} objects that will be transformed into JSON.
     * @param request              The current {@link HttpServletRequest} object.
     * @param response             The current {@link HttpServletResponse} object.
     * @param render               If the rendered HTML version must be included in the response, set to {@code true}.
     * @param user                 The {@link User} performing this action.
     * @param depth                The required depth for related Contentlets, in case they're required.
     * @param respectFrontendRoles If front-end Roles for the specified User must be validated, set this to {@code
     *                             true}.
     * @param language             The Language ID for the related Contentlets -- required only if the {@code depth}
     *                             parameter is specified.
     * @param live                 If the live version of the specified Contentlets must be retrieved, set this to
     *                             {@code true}.
     * @param allCategoriesInfo    If information about Categories must be included, set this to {@code true}.
     *
     * @return The JSON representation as {@link JSONArray} of the specified Contentlets.
     *
     * @throws IOException      An error occurred when generating the printable Contentlet map.
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    public JSONObject getJSONObject(final List<Contentlet> contentletList, final HttpServletRequest request,
                                     final HttpServletResponse response, final String render, final User user,
                                     final int depth, final boolean respectFrontendRoles, final long language,
                                     final boolean live, final boolean allCategoriesInfo){
        final JSONObject json = new JSONObject();
        final JSONArray jsonContentlets = new JSONArray();

        for (final Contentlet contentlet : contentletList) {
            try {
                final JSONObject contentAsJson = contentletToJSON(contentlet, response, render, user, allCategoriesInfo);
                jsonContentlets.put(contentAsJson);
                //we need to add relationships fields
                if (depth != -1){
                    addRelationshipsToJSON(request, response, render, user, depth,
                            respectFrontendRoles, contentlet, contentAsJson, null, language, live, allCategoriesInfo);
                }
            } catch (final Exception e) {
                final String errorMsg = String.format("An error occurred when converting Contentlet '%s' into JSON: " +
                        "%s", contentlet.getIdentifier(), e.getMessage());
                Logger.warn(this.getClass(), errorMsg);
                Logger.debug(this.getClass(), errorMsg, e);
            }
        }

        try {
            json.put("contentlets", jsonContentlets);
        } catch (final JSONException e) {
            final String errorMsg = String.format("An error occurred when adding Contentlets to the result JSON " +
                    "object: %s", e.getMessage());
            Logger.warn(this.getClass(), errorMsg);
            Logger.debug(this.getClass(), errorMsg, e);
        }

        return json;
    }

    /**
     * Transforms the specified Contentlet object into its JSON representation.
     *
     * @param con               The {@link Contentlet} object that will be transformed.
     * @param request           The current {@link HttpServletRequest} instance.
     * @param response          The current {@link HttpServletResponse} instance.
     * @param render            If the rendered HTML version must be included in the response, set to {@code true}.
     * @param user              The {@link User} performing this action.
     * @param allCategoriesInfo If information about Categories must be included, set to {@code true}.
     *
     * @return The representation of the Contentlet as a {@link JSONObject}.
     *
     * @throws JSONException        An error occurred when generating the JSON object.
     * @throws IOException          An error occurred when generating the printable Contentlet map.
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this action.
     */
    public JSONObject contentletToJSON(final Contentlet con,
                                              final HttpServletResponse response, final String render, final User user, final boolean allCategoriesInfo)
            throws JSONException, IOException, DotDataException, DotSecurityException {
        return contentletToJSON(con, response, render, user, allCategoriesInfo, false);
    }

    /**
     * Transforms the specified Contentlet object into its JSON representation.
     *
     * @param contentlet        The {@link Contentlet} object that will be transformed.
     * @param request           The current {@link HttpServletRequest} instance.
     * @param response          The current {@link HttpServletResponse} instance.
     * @param render            If the rendered HTML version must be included in the response, set to {@code true}.
     * @param user              The {@link User} performing this action.
     * @param allCategoriesInfo If information about Categories must be included, set to {@code true}.
     * @param hydrateRelated
     *
     * @return The representation of the Contentlet as a {@link JSONObject}.
     *
     * @throws JSONException        An error occurred when generating the JSON object.
     * @throws IOException          An error occurred when generating the printable Contentlet map.
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this action.
     */
    public JSONObject contentletToJSON(Contentlet contentlet,
                                              final HttpServletResponse response, final String render, final User user,
                                              final boolean allCategoriesInfo, final boolean hydrateRelated)
            throws JSONException, IOException, DotDataException, DotSecurityException {
        final JSONObject jsonObject = new JSONObject();
        final ContentType type = contentlet.getContentType();

        if(hydrateRelated) {
            final DotContentletTransformer myTransformer = new DotTransformerBuilder()
                    .hydratedContentMapTransformer().content(contentlet).build();
            contentlet = myTransformer.hydrate().get(0);
        }

        final boolean doRender = (BaseContentType.WIDGET.equals(type.baseType()) && Boolean.TRUE.toString().equalsIgnoreCase(render));
        //By default, all underlying transformer strategies that are triggered by the Widget ContentType or the option RENDER_FIELDS are enabled
        //Therefore, we need to disable them in case the render option is not enabled to avoid undesired rendering when no explicitly requested
        // Render field "code"
        final Map<String, Object> map = ContentletUtil.getContentPrintableMap(user, contentlet, allCategoriesInfo, doRender);
        final Set<String> jsonFields = getJSONFields(type);

        for (final String key : map.keySet()) {
            if (Arrays.binarySearch(ignoreFields, key) < 0) {
                if (jsonFields.contains(key)) {
                    Logger.debug(ContentResource.class,
                            key + " is a json field: " + map.get(key).toString());
                    jsonObject.put(key, new JSONObject(contentlet.getKeyValueProperty(key)));
                } else if (isCategoryField(type, key) && map.get(key) instanceof Collection) {
                    final Collection<?> categoryList = (Collection<?>) map.get(key);
                    jsonObject.put(key, new JSONArray(categoryList.stream()
                            .map(value -> new JSONObject((Map<?, ?>) value))
                            .collect(Collectors.toList())));
                }else if (isTagField(type, key) && map.get(key) instanceof Collection) {
                    final Collection<?> tags = (Collection<?>) map.get(key);
                    jsonObject.put(key, new JSONArray(tags));
                    // this might be coming from transformers views, so let's try to make them JSONObjects
                } else if (isStoryBlockField(type, key)) {
                    final String fieldValue = String.class.cast(map.get(key));
                    jsonObject.put(key, JsonUtil.isValidJSON(fieldValue) ? new JSONObject(fieldValue) : fieldValue);
                } else if(hydrateRelated) {
                    if(map.get(key) instanceof Map) {
                        jsonObject.put(key, new JSONObject((Map) map.get(key)));
                    } else {
                        jsonObject.put(key, map.get(key));
                    }
                } else {
                    jsonObject.put(key, map.get(key));
                }
            }
        }
        if (BaseContentType.WIDGET.equals(type.baseType()) && "true".equalsIgnoreCase(render)) {
            final HttpServletRequestImpersonator impersonator = HttpServletRequestImpersonator.newInstance();
            jsonObject.put("parsedCode", WidgetResource.parseWidget(impersonator.request(), response, contentlet));
        }

        if (BaseContentType.HTMLPAGE.equals(type.baseType())) {
            jsonObject.put(HTMLPageAssetAPI.URL_FIELD, ContentHelper.getInstance().getUrl(contentlet));
        }

        jsonObject.put("__icon__", UtilHTML.getIconClass(contentlet));
        jsonObject.put("contentTypeIcon", type.icon());
        jsonObject.put("variant", contentlet.getVariantId());
        return jsonObject;
    }

    public JSONObject addRelationshipsToJSON(final HttpServletRequest request,
                                                    final HttpServletResponse response,
                                                    final String render, final User user, final int depth,
                                                    final boolean respectFrontendRoles,
                                                    final Contentlet contentlet,
                                                    final JSONObject jsonObject, Set<Relationship> addedRelationships, final long language,
                                                    final boolean live, final boolean allCategoriesInfo)
            throws DotDataException, JSONException, IOException ,  DotSecurityException {

        return addRelationshipsToJSON(request, response, render, user, depth, respectFrontendRoles,
                contentlet, jsonObject, addedRelationships, language, live, allCategoriesInfo, false);
    }

    /**
     * Add relationships fields records to the json contentlet
     * @param request
     * @param response
     * @param render
     * @param user
     * @param depth
     * @param contentlet
     * @param jsonObject
     * @param addedRelationships
     * @param language
     * @param live
     * @param allCategoriesInfo {@code "true"} to return all fields for
     * the categories associated to the content (key, name, description), {@code "false"}
     * to return only categories names.
     * @return
     * @throws DotDataException
     * @throws JSONException
     * @throws IOException
     * @throws DotSecurityException
     */
    public JSONObject addRelationshipsToJSON(final HttpServletRequest request,
                                                    final HttpServletResponse response,
                                                    final String render, final User user, final int depth,
                                                    final boolean respectFrontendRoles,
                                                    final Contentlet contentlet,
                                                    final JSONObject jsonObject, Set<Relationship> addedRelationships, final long language,
                                                    final boolean live, final boolean allCategoriesInfo, final boolean hydrateRelated)
            throws DotDataException, JSONException, IOException, DotSecurityException {

        Relationship relationship;

        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

        //filter relationships fields
        final Map<String, com.dotcms.contenttype.model.field.Field> fields = contentlet.getContentType().fields()
                .stream().filter(field -> field instanceof RelationshipField).collect(
                        Collectors.toMap(field -> field.variable(), field -> field));

        if (addedRelationships == null){
            addedRelationships = new HashSet<>();
        }

        for (com.dotcms.contenttype.model.field.Field field:fields.values()) {

            try {
                relationship = relationshipAPI.getRelationshipFromField(field, user);
            }catch(DotDataException | DotSecurityException e){
                Logger.warn("Error getting relationship for field " + field, e.getMessage(), e);
                continue;
            }

            if (addedRelationships.contains(relationship)){
                continue;
            }
            if (!relationship.getParentStructureInode().equals(relationship.getChildStructureInode())) {
                addedRelationships.add(relationship);
            }

            final boolean isChildField = relationshipAPI.isChildField(relationship, field);

            final ContentletRelationships contentletRelationships = new ContentletRelationships(
                    contentlet);
            ContentletRelationships.ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                    relationship, isChildField);

            JSONArray jsonArray = addRelatedContentToJsonArray(request, response,
                    render, user, depth, respectFrontendRoles,
                    contentlet, addedRelationships, language, live, field, isChildField,
                    allCategoriesInfo, hydrateRelated);

            jsonObject.put(field.variable(), getJSONArrayValue(jsonArray, records.doesAllowOnlyOne()));

            //For self-related fields, the other side of the relationship should be added if the other-side field exists
            if (relationshipAPI.sameParentAndChild(relationship)){
                com.dotcms.contenttype.model.field.Field otherSideField = null;

                if (relationship.getParentRelationName() != null
                        && relationship.getChildRelationName() != null) {
                    if (isChildField) {
                        if (fields.containsKey(relationship.getParentRelationName())) {
                            otherSideField = fields.get(relationship.getParentRelationName());
                        }
                    } else {
                        if (fields.containsKey(relationship.getChildRelationName())) {
                            otherSideField = fields.get(relationship.getChildRelationName());
                        }
                    }
                }

                if (otherSideField != null){

                    records = contentletRelationships.new ContentletRelationshipRecords(
                            relationship, !isChildField);
                    jsonArray = addRelatedContentToJsonArray(request, response,
                            render, user, depth, respectFrontendRoles,
                            contentlet, addedRelationships, language, live,
                            otherSideField, !isChildField, allCategoriesInfo, hydrateRelated);

                    jsonObject.put(otherSideField.variable(),
                            getJSONArrayValue(jsonArray, records.doesAllowOnlyOne()));
                }
            }

        }

        return jsonObject;
    }

    public Set<String> getJSONFields(ContentType type)
            throws DotDataException, DotSecurityException {
        Set<String> jsonFields = new HashSet<>();
        List<Field> fields = new LegacyFieldTransformer(
                APILocator.getContentTypeAPI(APILocator.systemUser()).
                        find(type.inode()).fields()).asOldFieldList();
        for (Field f : fields) {
            if (f.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())
                    || f.getFieldType().equals(Field.FieldType.JSON_FIELD.toString()) ) {
                jsonFields.add(f.getVelocityVarName());
            }
        }

        return jsonFields;
    }

    private static boolean isCategoryField(final ContentType type, final String key) {
        try {
            Optional<com.dotcms.contenttype.model.field.Field> optionalField =
                    type.fields().stream().filter(f -> UtilMethods.equal(key, f.variable())).findFirst();
            if (optionalField.isPresent()) {
                return optionalField.get() instanceof CategoryField;
            }
        } catch (Exception e) {
            Logger.error(ContentResource.class, "Error getting field " + key, e);
        }
        return false;
    }

    private static boolean isTagField(final ContentType type, final String key) {
        try {
            Optional<com.dotcms.contenttype.model.field.Field> optionalField =
                    type.fields().stream().filter(f -> UtilMethods.equal(key, f.variable())).findFirst();
            if (optionalField.isPresent()) {
                return optionalField.get() instanceof TagField;
            }
        } catch (Exception e) {
            Logger.error(ContentResource.class, "Error getting field " + key, e);
        }
        return false;
    }

    /**
     * Verifies if the specified field in a Content Type is of type {@link StoryBlockField}.
     *
     * @param type         The {@link ContentType} containing such a field.
     * @param fieldVarName The Velocity Variable Name of the field that must be checked.
     *
     * @return If the field is of type {@link StoryBlockField}, returns {@code true}.
     */
    private static boolean isStoryBlockField(final ContentType type, final String fieldVarName) {
        try {
            final com.dotcms.contenttype.model.field.Field field = type.fieldMap().get(fieldVarName);
            return field instanceof StoryBlockField;
        } catch (final Exception e) {
            Logger.error(ContentResource.class,
                    String.format("Error checking StoryBlock type on field '%s': %s", fieldVarName, e.getMessage()), e);
        }
        return Boolean.FALSE;
    }


    /**
     * Returns a jsonArray of related contentlets if depth = 2. If depth = 1 returns the related
     * object, otherwise it will return a comma-separated list of identifiers
     * @param jsonArray
     * @return
     * @throws JSONException
     */
    private static Object getJSONArrayValue(final JSONArray jsonArray, final boolean allowOnlyOne)
            throws JSONException {
        if (allowOnlyOne && !jsonArray.isEmpty()) {
            return jsonArray.get(0);
        } else {
            return jsonArray;
        }
    }

    /**
     *
     * @param request
     * @param response
     * @param render
     * @param user
     * @param depth
     * @param respectFrontendRoles
     * @param contentlet
     * @param addedRelationships
     * @param language
     * @param live
     * @param field
     * @param isParent
     * @param allCategoriesInfo
     * @return
     * @throws JSONException
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private JSONArray addRelatedContentToJsonArray(HttpServletRequest request,
                                                          HttpServletResponse response, String render, User user, int depth,
                                                          boolean respectFrontendRoles, Contentlet contentlet,
                                                          Set<Relationship> addedRelationships, long language, boolean live,
                                                          com.dotcms.contenttype.model.field.Field field, final boolean isParent,
                                                          final boolean allCategoriesInfo, final boolean hydrateRelated)
            throws JSONException, IOException, DotDataException, DotSecurityException {


        final JSONArray jsonArray = new JSONArray();

        for (Contentlet relatedContent : contentlet.getRelated(field.variable(), user, respectFrontendRoles, isParent, language, live)) {


            Object originalValue = relatedContent.get(field.name());

            relatedContent.setProperty(field.name(), null);

            if (relatedContent.getContentType() != null &&
                relatedContent.getContentType().fields().stream().anyMatch(f ->
                f.variable().equals(field.variable()) && !f.type().equals(RelationshipField.class) && !f.type().equals(StoryBlockField.class))) {
                relatedContent.setProperty(field.name(), originalValue);
            }


            switch (depth) {
                //returns a list of identifiers
                case 0:
                    jsonArray.put(relatedContent.getIdentifier());
                    break;

                //returns a list of related content objects
                case 1:
                    jsonArray
                            .put(contentletToJSON(relatedContent, response,
                                    render, user, allCategoriesInfo, hydrateRelated));
                    break;

                //returns a list of related content identifiers for each of the related content
                case 2:
                    jsonArray.put(addRelationshipsToJSON(request, response, render, user, 0,
                            respectFrontendRoles, relatedContent,
                            contentletToJSON(relatedContent, response,
                                    render, user, allCategoriesInfo, hydrateRelated),
                            new HashSet<>(addedRelationships), language, live, allCategoriesInfo, hydrateRelated));
                    break;

                //returns a list of hydrated related content for each of the related content
                case 3:
                    jsonArray.put(addRelationshipsToJSON(request, response, render, user, 1,
                            respectFrontendRoles, relatedContent,
                            contentletToJSON(relatedContent, response,
                                    render, user, allCategoriesInfo, hydrateRelated),
                            new HashSet<>(addedRelationships), language, live, allCategoriesInfo, hydrateRelated));
                    break;
            }

        }

        return jsonArray;
    }

    /**
     * Extracts the value of the language id from a luceneQuery
     * **/
    public Long extractLanguageIdFromQuery(String query, SearchForm searchForm) {
        String languageId = null;
        String languageIdStr = "languageId";

        if (UtilMethods.isSet(query) && query.contains(languageIdStr)) {
            int index = query.indexOf(languageIdStr) + languageIdStr.length() + 1; // +1 for ':'
            try {
                languageId = query.substring(index, query.indexOf(" ", index));
            } catch (StringIndexOutOfBoundsException e) {
                languageId = query.substring(index);
            }
        }

        return UtilMethods.isSet(languageId) ? Long.parseLong(languageId) : searchForm.getLanguageId();
    }

}
