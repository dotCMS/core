package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.rest.ResponseEntityContentletView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.DotPreconditions;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * Version 1 of the Content resource, to interact and retrieve contentlets
 * @author jsanca
 */
@Path("/v1/content")
public class ContentResource {

    private final WebResource    webResource;
    private final ContentletAPI  contentletAPI;
    private final IdentifierAPI  identifierAPI;

    private final Lazy<Boolean> isDefaultContentToDefaultLanguageEnabled = Lazy.of(
            () -> Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false));


    public ContentResource() {

        this(new WebResource(),
                APILocator.getContentletAPI(),
                APILocator.getIdentifierAPI());
    }

    @VisibleForTesting
    public ContentResource(final WebResource     webResource,
                           final ContentletAPI   contentletAPI,
                           final IdentifierAPI  identifierAPI) {

        this.webResource    = webResource;
        this.contentletAPI  = contentletAPI;
        this.identifierAPI  = identifierAPI;
    }

    /**
     * Executes a "Save as Draft" action on the specified Contentlet. For more information on how the save-as-draft
     * works in dotCMS, see {@link ContentletAPI#saveDraft(Contentlet, Map, List, List, User, boolean)}.
     *
     * @param request     {@link HttpServletRequest}
     * @param inode       {@link String} (Optional) The Inode that may be used to save the Contentlet.
     * @param identifier  {@link String} (Optional) The existing identifier of the Contentlet (in combination with
     *                                  the language).
     * @param indexPolicy {@link String} (Optional) The indexing policy that will be used to save this Contentlet.
     * @param language    {@link String} (Optional) The Contentlet's language (in combination with the identifier).
     * @param contentForm {@link ContentForm} The {@link ContentForm} containing the Contentlet's data map.
     *
     * @return ResponseEntityContentletView The data map of the saved Contentlet.
     */
    @PUT
    @Path("/_draft")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    public final ResponseEntityContentletView saveDraft(@Context final HttpServletRequest request,
                                                     @QueryParam("inode")                        final String inode,
                                                     @QueryParam("identifier")                   final String identifier,
                                                     @QueryParam("indexPolicy")                  final String indexPolicy,
                                                     @DefaultValue("-1") @QueryParam("language") final String language,
                                                     final ContentForm contentForm) throws DotDataException, DotSecurityException {
        final InitDataObject initDataObject = new WebResource.InitBuilder().requestAndResponse(request,
                new MockHttpResponse()).requiredAnonAccess(AnonymousAccess.WRITE).init();
        Logger.debug(this, () -> "On Saving Draft, inode = " + inode + ", identifier = " + identifier + ", language =" +
                                         " " + language + " indexPolicy = " + indexPolicy);
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode = PageMode.get(request);
        final Contentlet contentlet = this.getContentlet(inode, identifier, languageId,
                () -> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(), contentForm,
                initDataObject.getUser(), mode);
        if (UtilMethods.isSet(indexPolicy)) {
            contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
        }
        final Optional<List<Category>> categories = MapToContentletPopulator.INSTANCE.fetchCategories(contentlet,
                initDataObject.getUser(), mode.respectAnonPerms);
        contentlet.setLowIndexPriority(true);
        contentlet.setProperty(Contentlet.DONT_VALIDATE_ME, true);
        contentlet.setProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().saveDraft(contentlet,
                (ContentletRelationships) contentlet.get(Contentlet.RELATIONSHIP_KEY), categories.orElse(null), null,
                initDataObject.getUser(), false);
        return new ResponseEntityContentletView(
                new DotTransformerBuilder().defaultOptions().content(contentlet).build().toMaps().stream().findFirst().orElse(Collections.emptyMap()));
    }

    /**
     * Returns a Contentlet form the API based on either the specified query parameters used when calling the REST
     * Endpoint, or the values that are present in the form. The resulting object will contain the original data map
     * merged with the new values that where provided via the {@link ContentForm} object.
     *
     * @param inodeByParam      (Optional) The Inode specified via query param.
     * @param identifierByParam (Optional) The Identifier specified via query param.
     * @param languageByParam   (Optional) The language ID specified via query param.
     * @param sessionLanguage   The {@link Supplier} for providing the language ID in case the specified language is
     *                          invalid.
     * @param contentForm       The {@link ContentForm} with the Contentlet's data map being passed down in the
     *                          request, which includes all the changes that must be persisted.
     * @param user              The {@link User} executing this action.
     * @param pageMode          The {@link PageMode} in which the Contentlet is being retrieved.
     *
     * @return The original {@link Contentlet} including the new changes.
     *
     * @throws DotDataException     An error occurred when retrieving the Contentlet from the API.
     * @throws DotSecurityException The specified User doesn't have the required permissions to retrieve the
     * specified Contentlet.
     */
    private Contentlet getContentlet(final String inodeByParam, final String identifierByParam,
                                     final long languageByParam, final Supplier<Long> sessionLanguage,
                                     final ContentForm contentForm, final User user, final PageMode pageMode) throws DotDataException, DotSecurityException {
        DotPreconditions.notNull(contentForm, () -> "When no inode is sent, the info on the Request body becomes " +
                                                            "mandatory.");
        DotPreconditions.notNull(contentForm.getContentlet(), () -> "When no inode is sent, the info on the Request " +
                                                                            "body becomes mandatory.");
        PageMode mode = pageMode;
        final String inode = UtilMethods.isSet(inodeByParam) ? inodeByParam :
                                     (String) contentForm.getContentlet().get("inode");
        final String identifier = UtilMethods.isSet(identifierByParam) ? identifierByParam :
                                          (String) contentForm.getContentlet().get("identifier");

        if (UtilMethods.isSet(inode)) {
            Logger.debug(this, () -> "Looking for content by inode: " + inode);
            final Contentlet existentContentlet = this.contentletAPI.find(inode, user, mode.respectAnonPerms);
            DotPreconditions.notNull(existentContentlet, () -> String.format("Contentlet with Inode '%s' was not " +
                                                                                     "found", inode),
                    DoesNotExistException.class);
            return this.populateContentlet(contentForm, existentContentlet, user, mode);
        } else if (UtilMethods.isSet(identifier)) {
            Logger.debug(this,
                    () -> "Looking for content by identifier: " + identifier + " and language id: " + languageByParam);
            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<Contentlet> existentContentlet = languageByParam <= 0 ?
                                                                    Optional.ofNullable(this.contentletAPI.findContentletByIdentifier(identifier, mode.showLive, sessionLanguage.get(), user, mode.respectAnonPerms)) : this.contentletAPI.findContentletByIdentifierOrFallback(identifier, mode.showLive, languageByParam, user, mode.respectAnonPerms);
            DotPreconditions.isTrue(existentContentlet.isPresent(), () -> String.format("Contentlet with ID '%s' was " +
                                                                                                "not found",
                    identifier), DoesNotExistException.class);
            return this.populateContentlet(contentForm, existentContentlet.get(), user, mode);
        }
        throw new DotContentletValidationException(String.format("Contentlet with ID '%s' [%s] could not be " +
                                                                         "retrieved", identifier, inode));
    }

    /**
     * Takes both the Contentlet with changes from the User and the existent Contentlet in dotCMS, and merges both into
     * one for it to be saved correctly.
     *
     * @param contentForm        The Contentlet with the changes submitted by the User.
     * @param existentContentlet The original existent Contentlet.
     * @param user               The {@link User} executing this action.
     * @param mode               The {@link PageMode} in which the Contentlet is being retrieved.
     *
     * @return The full {@link Contentlet} with the updated properties merged in it.
     *
     * @throws DotSecurityException The specified User doesn't have the required permissions to retrieve the
     * specified Contentlet.
     */
    private Contentlet populateContentlet(final ContentForm contentForm, final Contentlet existentContentlet,
                                          final User user, final PageMode mode) throws DotSecurityException {
        final Contentlet tempContentlet = new Contentlet();
        tempContentlet.getMap().putAll(existentContentlet.getMap());
        final Contentlet mergedContentlet = MapToContentletPopulator.INSTANCE.populate(tempContentlet,
                contentForm.getContentlet());
        if (null == mergedContentlet || null == mergedContentlet.getContentType()) {
            throw new DotContentletValidationException(String.format("Contentlet '%s' does not have a specified " +
                                                                             "Content Type",
                    tempContentlet.getIdentifier()));
        }
        try {
            if (!APILocator.getPermissionAPI().doesUserHavePermission(mergedContentlet.getContentType(),
                    PermissionAPI.PERMISSION_EDIT, user, mode.respectAnonPerms)) {
                throw new DotSecurityException(String.format("User '%s' does not have EDIT permissions on contentlet "
                                                                     + "'%s'", user.getUserId(),
                        mergedContentlet.getIdentifier()));
            }
        } catch (final DotDataException e) {
            throw new DotSecurityException(e.getMessage(), e);
        }
        for (final Field constant : mergedContentlet.getContentType().fields()) {
            if (constant instanceof ConstantField) {
                mergedContentlet.getMap().put(constant.variable(), constant.values());
            }
        }
        return mergedContentlet;
    }

    /**
     * Retrieves a Contentlet based on either its Inode or Identifier. If it doesn't exist, a 404
     * will be returned.
     *
     * @param request  The current {@link HttpServletRequest} instance.
     * @param response The current {@link HttpServletResponse} instance.
     *
     * @return The {@link Contentlet} matching the Inode or Identifier.
     */
    @GET
    @Path("/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContent(@Context HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                               @DefaultValue("") @QueryParam("language") final String language,
                               @DefaultValue("-1") @QueryParam("depth") final int depth


    ) {

        final User user =
                new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .init().getUser();

        Logger.debug(this, () -> "Finding the contentlet: " + inodeOrIdentifier);

        final LanguageWebAPI languageWebAPI = WebAPILocator.getLanguageWebAPI();
        final LongSupplier sessionLanguageSupplier = ()-> languageWebAPI.getLanguage(request).getId();
        final PageMode mode   = PageMode.get(request);
        final long testLangId = LanguageUtil.getLanguageId(language);
        final long languageId = testLangId <=0 ? sessionLanguageSupplier.getAsLong() : testLangId;

        Contentlet contentlet = this.resolveContentletOrFallBack(inodeOrIdentifier, mode, languageId, user);

        if (-1 != depth) {
            ContentUtils.addRelationships(contentlet, user, mode,
                    languageId, depth, request, response);
        }
        contentlet = new DotTransformerBuilder().contentResourceOptions(false).content(contentlet).build().hydrate().get(0);
        return Response.ok(new ResponseEntityView<>(
                WorkflowHelper.getInstance().contentletToMap(contentlet))).build();
    }

    @GET
    @Path("/_canlock/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response canLockContent(@Context HttpServletRequest request, @Context final HttpServletResponse response,
                                   @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                                   @DefaultValue("-1") @QueryParam("language") final String language)
            throws DotDataException, JSONException, DotSecurityException {

        final User user =
                new WebResource.InitBuilder(this.webResource)
                        .requestAndResponse(request, response)
                        .requiredAnonAccess(AnonymousAccess.READ)
                        .init().getUser();

        final PageMode mode   = PageMode.get(request);
        final long testLangId = LanguageUtil.getLanguageId(language);
        final long languageId = testLangId <=0 ? WebAPILocator.getLanguageWebAPI().getLanguage(request).getId() : testLangId;

        final Contentlet contentlet = this.resolveContentletOrFallBack(inodeOrIdentifier, mode, languageId, user);


        final Map<String, Object> resultMap = new HashMap<>();

        if(UtilMethods.isEmpty(contentlet::getIdentifier)) {
            throw new DoesNotExistException(getDoesNotExistMessage(inodeOrIdentifier, languageId));
        }

        final boolean canLock = Try.of(()->this.contentletAPI.canLock( contentlet, user)).getOrElse(false);

        resultMap.put("canLock", canLock);
        resultMap.put("locked", contentlet.isLocked());

        final Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

        if (contentlet.isLocked() && cvi.isPresent()) {
            resultMap.put("lockedBy", cvi.get().getLockedBy());
            resultMap.put("lockedOn", cvi.get().getLockedOn());
            resultMap.put("lockedByName", APILocator.getUserAPI().loadUserById(cvi.get().getLockedBy()));
        }

        resultMap.put("inode", contentlet.getInode());
        resultMap.put("id",    contentlet.getIdentifier());

        return Response.ok(new ResponseEntityView<>(resultMap)).build();
    }

    /**
     * Given an inode or identifier, this method will return the contentlet object for the given language
     * @param inodeOrIdentifier the inode or identifier to test
     * @param languageId the language id
     * @return the contentlet object
     */
    private static String getDoesNotExistMessage(final String inodeOrIdentifier, final long languageId){
        return String.format("The contentlet %s and language %d does not exist", inodeOrIdentifier, languageId);
    }

    /**
     * Given an inode or identifier, this method will return the contentlet object for the given language
     * If no contentlet is found, it will return the contentlet for the default language if FallBack is enabled
     * @param inodeOrIdentifier the inode or identifier to test
     * @param mode the page mode used to determine if we are
     * @param languageId the language id
     * @param user the user
     * @return the contentlet object
     */
    private Contentlet resolveContentletOrFallBack (final String inodeOrIdentifier, final PageMode mode, final long languageId, final User user) {
        // This property is used to determine if we should map the contentlet to the default language
        final boolean mapToDefault = isDefaultContentToDefaultLanguageEnabled.get();
        // default language supplier, we only get the language id if we need it
        LongSupplier defaultLang = () -> APILocator.getLanguageAPI().getDefaultLanguage().getId();
        //Attempt to resolve the contentlet for the given language
        Optional<Contentlet> optional = resolveContentlet(inodeOrIdentifier, mode, languageId, user);
        //If the contentlet is not found, and we are allowed to map to the default language..
        if(optional.isEmpty() && mapToDefault && languageId != defaultLang.getAsLong()){
            //Attempt to resolve the contentlet for the default language
             optional = resolveContentlet(inodeOrIdentifier, mode, defaultLang.getAsLong(), user);
        }
        //If we found the contentlet, return it
        if (optional.isPresent()) {
            return optional.get();
        }
        //If we didn't find the contentlet, throw an exception
        throw new DoesNotExistException(getDoesNotExistMessage(inodeOrIdentifier,languageId));
    }

    /**
     * Given an inode or identifier, this method will return the contentlet object
     * internally relies on the shorty api to resolve the type of identifier we are dealing with
     * Therefore it's lighter on the system than the findContentletByIdentifier method
     * @param inodeOrIdentifier the inode or identifier to test
     * @param mode the page mode used to determine if we are
     * @param languageId the language id
     * @param user the user
     * @return the contentlet object
     */
    private Optional<Contentlet> resolveContentlet (final String inodeOrIdentifier, PageMode mode, long languageId, User user) {

        final Optional<ShortyId> shortyId = APILocator.getShortyAPI().getShorty(inodeOrIdentifier);

        if (shortyId.isEmpty()) {
            throw new DoesNotExistException(getDoesNotExistMessage(inodeOrIdentifier, languageId));
        }

        String testInode = inodeOrIdentifier;

        final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
        if(ShortType.IDENTIFIER == shortyId.get().type) {
            Optional<ContentletVersionInfo> cvi = versionableAPI.getContentletVersionInfo(shortyId.get().longId, languageId);
            if (cvi.isPresent()) {
                testInode =  mode.showLive ? cvi.get().getLiveInode() : cvi.get().getWorkingInode();
            }
        }

        final String finalInode = testInode;
        return Optional.ofNullable(Try.of(() -> contentletAPI.find(finalInode, user, mode.respectAnonPerms)).getOrNull());
    }


    /**
     * Will return a Contentlet objects. If you are building large pulls and depending on the types
     * of fields on the content this can get expensive especially with large data sets.<br />
     * EXAMPLE:<br /> /api/v1/content/related<br />
     *
     * {
     *     "identifier":"d66309a7378bbad381fda3accd7b2e80",
     *     "fieldVariable":"myRelationship",
     *     "condition":"+title:child5",
     *     "orderBy":"title desc"
     * }
     * The method will figure out language, working and
     * live for you if not passed in with the condition Returns empty List if no results are found
     * @param request  - Http Request
     * @param response - Http Response
     * @param pullRelatedForm {@link PullRelatedForm}
     * @return Returns empty List if no results are found
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("related")
    @Operation(summary = "Pull Related Content",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "404", description = "Contentlet not found"),
                    @ApiResponse(responseCode = "400", description = "Contentlet does not have a relationship field")})
    public Response pullRelated(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               final PullRelatedForm pullRelatedForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredAnonAccess(AnonymousAccess.READ).init();

        Logger.debug(this, ()-> "Requesting pull related parents for the contentletIdentifier: " + pullRelatedForm.getIdentifier() +
                ", relationshipFieldVariable: " + pullRelatedForm.getFieldVariable() + ", luceneCondition: " + pullRelatedForm.getCondition() +
                ", limit: " + pullRelatedForm.getLimit() + ", offset: " + pullRelatedForm.getOffset() + ", orderby: " + pullRelatedForm.getOrderBy());

        final User user = initData.getUser();
        final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final long langId = UtilMethods.isSet(pullRelatedForm.getCondition())
                && pullRelatedForm.getCondition().contains("languageId") ? -1 : language.getId();
        final String tmDate = request.getSession (false) != null?
                (String) request.getSession (false).getAttribute("tm_date"):null;
        final PageMode mode = PageMode.get(request);
        final boolean editOrPreviewMode = !mode.showLive;
        final Contentlet contentlet = APILocator.getContentletAPI().
                findContentletByIdentifier(pullRelatedForm.getIdentifier(), mode.showLive, langId, user, mode.respectAnonPerms);

        if (null ==  contentlet || !InodeUtils.isSet(contentlet.getIdentifier())) {

            Logger.debug(this, ()-> "The identifier:" + pullRelatedForm.getIdentifier() + " does not exists");
            throw new DoesNotExistException("The identifier:" + pullRelatedForm.getIdentifier() + " does not exists");
        }

        final Field field = contentlet.getContentType().fieldMap().get(pullRelatedForm.getFieldVariable());
        if (field instanceof RelationshipField) {

            final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field, user);
            final boolean pullParents = APILocator.getRelationshipAPI().isParentField(relationship, field);
            final List<Contentlet> retrievedContentlets = ContentUtils.getPullResults(relationship,
                    pullRelatedForm.getIdentifier(),
                    pullRelatedForm.getCondition() != null?
                            pullRelatedForm.getCondition():
                            ContentUtils.addDefaultsToQuery(pullRelatedForm.getCondition(), editOrPreviewMode, request),
                    pullRelatedForm.getLimit(), pullRelatedForm.getOffset(), pullRelatedForm.getOrderBy(),
                    user, tmDate, pullParents, langId, editOrPreviewMode? null : true);

            return Response.ok(new ResponseEntityView<>(retrievedContentlets.stream()
                        .map(contentItem -> WorkflowHelper.getInstance().contentletToMap(contentItem)).collect(Collectors.toList())
                    )).build();
        }

        Logger.debug(this, ()-> "The field:" + pullRelatedForm.getFieldVariable() + " is not a relationship");
        throw new IllegalArgumentException("The field:" + pullRelatedForm.getFieldVariable() + " is not a relationship");
    }
}
