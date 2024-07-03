package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.DotPreconditions;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.*;
import com.dotmarketing.util.json.JSONException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
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
                               @DefaultValue("") @QueryParam("language") final String language,
                               @DefaultValue("-1") @QueryParam("depth") final int depth) {

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
            throw new DoesNotExistException(getDoesNotExistMessage(inodeOrIdentifier,languageId));
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
