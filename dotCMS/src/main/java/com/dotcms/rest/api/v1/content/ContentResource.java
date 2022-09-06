package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.rest.ResponseEntityContentletView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.DotPreconditions;
import com.dotcms.workflow.form.FireActionByNameForm;
import com.dotcms.workflow.form.FireActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
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
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Version 1 of the Content resource, to interact and retrieve contentlets
 * @author jsanca
 */
@Path("/v1/content")
public class ContentResource {

    private final WebResource    webResource;
    private final ContentletAPI  contentletAPI;
    private final IdentifierAPI  identifierAPI;

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
     * Do a Save draft of the content
     * @param request     {@link HttpServletRequest}
     * @param inode       {@link String} (Optional) to save draft over the existing inode.
     * @param identifier  {@link String} (Optional) to save draft over the existing identifier (in combination of language).
     * @param indexPolicy {@link String} (Optional) to save draft over the existing index policy
     * @param language    {@link String} (Optional) to save draft over the existing language (in combination of identifier).
     * @param contentForm {@link ContentForm} ContentForm
     * @return ResponseEntityContentletView
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
                                                     @DefaultValue("-1") @QueryParam("language") final String   language,
                                                     final ContentForm contentForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, new MockHttpResponse())
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .init();

        Logger.debug(this, ()-> "On Saving Draft, inode = " + inode +
                ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode   = PageMode.get(request);
        //if inode is set we use it to look up a contentlet
        final Contentlet contentlet = this.getContentlet
                (inode, identifier, languageId,
                        ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                        contentForm, initDataObject, mode);

        if (UtilMethods.isSet(indexPolicy)) {
            contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
        }

        final List<Category> categories = MapToContentletPopulator.INSTANCE.getCategories
                (contentlet, initDataObject.getUser(), mode.respectAnonPerms);

        contentlet.setLowIndexPriority(true);
        contentlet.setProperty(Contentlet.DONT_VALIDATE_ME, true);
        contentlet.setProperty(Contentlet.DISABLE_WORKFLOW, true);
        //Contentlet.RELATIONSHIP_KEY
        final Contentlet savedDraftContentlet =
                APILocator.getContentletAPI().saveDraft(contentlet, (ContentletRelationships)contentlet.get(Contentlet.RELATIONSHIP_KEY),
                            categories, null, initDataObject.getUser(), false);

        return new ResponseEntityContentletView(null == contentlet?Collections.emptyMap():
                new DotTransformerBuilder().defaultOptions().content(contentlet).build()
                        .toMaps().stream().findFirst().orElse(Collections.emptyMap())
                );
    }

    private Contentlet getContentlet(final String paramInode,
                                     final String paramIdentifier,
                                     final long   paramLanguage,
                                     final Supplier<Long> sessionLanguage,
                                     final ContentForm contentForm,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        DotPreconditions.notNull(contentForm , ()-> "When no inode is sent the info on the Request body becomes mandatory.");
        DotPreconditions.notNull(contentForm.getContentlet() , ()-> "When no inode is sent the info on the Request body becomes mandatory.");

        PageMode mode = pageMode;
        final String inode      = UtilMethods.isSet(paramInode)? paramInode:          (String)contentForm.getContentlet().get("inode");
        final String identifier = UtilMethods.isSet(paramIdentifier)?paramIdentifier: (String)contentForm.getContentlet().get("identifier");
        final long language     = UtilMethods.isSet(paramLanguage)?paramLanguage:     (Long)  contentForm.getContentlet().getOrDefault("languageId", -1);

        if(UtilMethods.isSet(inode)) {

            Logger.debug(this, ()-> "Looking for content by inode: " + inode);

            final Contentlet currentContentlet = this.contentletAPI.find(inode, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.notNull(currentContentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);

            return populateContentlet(contentForm, currentContentlet, initDataObject.getUser(), mode);
        } else if (UtilMethods.isSet(identifier)) {

            Logger.debug(this, ()-> "Looking for content by identifier: " + identifier + " and language id: " + language);

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<Contentlet> currentContentlet =  language <= 0?
                    Optional.ofNullable(this.contentletAPI.findContentletByIdentifier(identifier, mode.showLive, sessionLanguage.get(), initDataObject.getUser(), mode.respectAnonPerms)):
                    this.contentletAPI.findContentletByIdentifierOrFallback(identifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.isTrue(currentContentlet.isPresent(), ()-> "contentlet-was-not-found", DoesNotExistException.class);

            return populateContentlet(contentForm, currentContentlet.get(), initDataObject.getUser(), mode);
        }

         throw new DotContentletValidationException("Can not do save draft on new contentlets");
    }

    private Contentlet populateContentlet(final ContentForm contentForm,
                                          final Contentlet paramContentletInput,
                                          final User user,
                                          final PageMode mode) throws DotSecurityException {

        final Contentlet cloneContentlet = new Contentlet();
        cloneContentlet.getMap().putAll(paramContentletInput.getMap());

        final Contentlet contentlet = MapToContentletPopulator.INSTANCE.populate (cloneContentlet, contentForm.getContentlet());

        if (null == contentlet || null == contentlet.getContentType()) {

            throw new DotContentletValidationException("The Contentlet does not has content type");
        }

        try {
            if (!APILocator.getPermissionAPI().doesUserHavePermission(contentlet.getContentType(),
                    PermissionAPI.PERMISSION_EDIT, user, mode.respectAnonPerms)) {

                throw new DotSecurityException("The user has not permissions to edit the contentlet");
            }
        } catch (DotDataException e) {
            throw new DotSecurityException(e.getMessage(), e);
        }

        for(final Field constant : contentlet.getContentType().fields()) {
            if(constant instanceof ConstantField)
                contentlet.getMap().put(constant.variable(), constant.values());
        }

        return contentlet;
    } // populateContentlet.

    /**
     * Retrieve a contentlet if exists, 404 otherwise
     * @param request
     * @param response
     * @return Retrieve single contentlet
     */
    @GET
    @Path("/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContent(@Context HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                               @DefaultValue("-1") @QueryParam("language") final String language) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .init();

        Logger.debug(this, () -> "Finding the contentlet: " + inodeOrIdentifier);

        final Tuple2<String, String> idOrInode = this.getIdentifierOrInode(inodeOrIdentifier);
        final String id       = idOrInode._1();
        final String inode    = idOrInode._2();
        final PageMode mode   = PageMode.get(request);
        final long languageId = LanguageUtil.getLanguageId(language);

        return Response.ok(new ResponseEntityView(
                WorkflowHelper.getInstance().contentletToMap(
                        this.getContentlet(inode, id, languageId,
                                ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                                initDataObject, mode)))).build();
    }

    @GET
    @Path("/_canlock/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response canLockContent(@Context HttpServletRequest request, @Context final HttpServletResponse response,
                                   @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                                   @DefaultValue("-1") @QueryParam("language") final String language)
            throws DotDataException, JSONException, DotSecurityException {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(this.webResource)
                        .requestAndResponse(request, response)
                        .requiredAnonAccess(AnonymousAccess.READ)
                        .init();

        Logger.debug(this, () -> "Can lock Contentlet: " + inodeOrIdentifier);

        final Tuple2<String, String> idOrInode = this.getIdentifierOrInode(inodeOrIdentifier);
        final String id       = idOrInode._1();
        final String inode    = idOrInode._2();
        final PageMode mode   = PageMode.get(request);
        final long languageId = LanguageUtil.getLanguageId(language);

        final Map<String, Object> resultMap = new HashMap<>();
        final Contentlet contentlet = this.getContentlet(inode, id, languageId,
                ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                initDataObject, mode);

        final boolean canLock = Try.of(()->this.contentletAPI.canLock(
                contentlet, initDataObject.getUser())).getOrElse(false);

        resultMap.put("canLock", canLock);
        resultMap.put("locked", contentlet.isLocked());

        final Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI()
                .getContentletVersionInfo(id, contentlet.getLanguageId());

        if (contentlet.isLocked() && cvi.isPresent()) {
            resultMap.put("lockedBy", cvi.get().getLockedBy());
            resultMap.put("lockedOn", cvi.get().getLockedOn());
            resultMap.put("lockedByName", APILocator.getUserAPI().loadUserById(cvi.get().getLockedBy()));
        }

        resultMap.put("inode", contentlet.getInode());
        resultMap.put("id",    contentlet.getIdentifier());

        return Response.ok(new ResponseEntityView(resultMap)).build();
    }

    private Tuple2<String, String> getIdentifierOrInode (final String inodeOrIdentifier) throws DotDataException {

        String inode = null;
        String id    = null;
        //Check if is an inode
        final String type = Try.of(() -> InodeUtils.getAssetTypeFromDB(inodeOrIdentifier)).getOrNull();

        //Could mean 2 things: it's an identifier or uuid does not exists
        if (null == type) {
            final Identifier identifier = this.identifierAPI.find(inodeOrIdentifier);

            if (null == identifier || UtilMethods.isNotSet(identifier.getId())) {
                throw new DoesNotExistException(
                        "The contentlet " + inodeOrIdentifier + " does not exists");
            }

            id    = identifier.getId();
        } else {

            inode = inodeOrIdentifier;  // look for by inode
        }

        return Tuple.of(id, inode);
    }

    private Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        PageMode mode = pageMode;

        if(UtilMethods.isSet(inode)) {

            Logger.debug(this, ()-> "Looking for content by inode: " + inode);

            contentlet = this.contentletAPI.find
                    (inode, initDataObject.getUser(), mode.respectAnonPerms);
        } else if (UtilMethods.isSet(identifier)) {

            Logger.debug(this, ()-> "Looking for content by identifier: " + identifier
                    + " and language id: " + language);

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<Contentlet> contentletOpt =  language <= 0?
                    WorkflowHelper.getInstance().getContentletByIdentifier(identifier, mode, initDataObject.getUser(), sessionLanguage):
                    this.contentletAPI.findContentletByIdentifierOrFallback
                            (identifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms);

            if (contentletOpt.isPresent()) {
                contentlet = contentletOpt.get();
            }
        }

        DotPreconditions.notNull(contentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);
        return contentlet;
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
