package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.CountView;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.rest.ResponseEntityContentletView;
import com.dotcms.rest.ResponseEntityListMapView;
import com.dotcms.rest.ResponseEntityCountView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.SearchForm;
import com.dotcms.rest.SearchView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.content.search.LuceneQueryBuilder;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.PaginationUtilParams;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.variant.VariantAPI;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Identifier;
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
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.velocity.exception.ResourceNotFoundException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Version 1 of the Content resource, to interact and retrieve contentlets
 * @author jsanca
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Path("/v1/content")
@Tag(name = "Content", description = "Endpoints for managing content and contentlets - the core data objects in dotCMS")
public class ContentResource {

    private final WebResource    webResource;
    private final ContentletAPI  contentletAPI;
    private final IdentifierAPI  identifierAPI;
    private final LanguageWebAPI languageWebAPI;
    private final LanguageAPI languageAPI;
    private final ContentHelper contentHelper;

    private final Lazy<Boolean> isDefaultContentToDefaultLanguageEnabled = Lazy.of(
            () -> Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false));

    public ContentResource() {
        this(new WebResource(),
                APILocator.getContentletAPI(),
                APILocator.getIdentifierAPI(),
                WebAPILocator.getLanguageWebAPI(),
                APILocator.getLanguageAPI(),
                ContentHelper.getInstance());
    }

    @VisibleForTesting
    public ContentResource(final WebResource     webResource,
                           final ContentletAPI   contentletAPI,
                           final IdentifierAPI  identifierAPI,
                           final LanguageWebAPI languageWebAPI,
                           final LanguageAPI    languageAPI,
                           final ContentHelper contentHelper) {
        this.webResource    = webResource;
        this.contentletAPI  = contentletAPI;
        this.identifierAPI  = identifierAPI;
        this.languageWebAPI = languageWebAPI;
        this.languageAPI = languageAPI;
        this.contentHelper = contentHelper;
    }



    @Operation(
            summary = "Contentlet Push History",
            description = "Returns the push history of a contentlet"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History data retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityPaginatedDataView.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "The specified identifier does not match any contentlet.",
            content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "An internal dotCMS error has occurred.",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{identifier}/push/history")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityPaginatedDataView getPushHistory(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "ID of the Contentlet whose push history will be retrieved.")
                @PathParam("identifier") final String contentId,
            @Parameter(description = "Maximum number or results being returned, for pagination purposes.")
                @QueryParam("limit") final int limit,
            @Parameter(description = "Page number of the results being returned, for pagination purposes.")
                @QueryParam("offset") final int offset) throws DotDataException {
        new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentId);
        if (!UtilMethods.isSet(contentlet)) {
            throw new ResourceNotFoundException(String.format("Content ID '%s' does not exist", contentId));
        }
        Logger.debug(this, String.format("Retrieving Push History for Content ID '%s' / limit = %s / offset = %s",
                contentId, limit, offset));
        final PaginationUtil paginationUtil = new PaginationUtil(new ContentPushHistoryPaginator());

        final PaginationUtilParams<Map<String, Object>, PaginatedArrayList<?>> params =
                new PaginationUtilParams.Builder<Map<String, Object>, PaginatedArrayList<?>>()
                        .withFilter(contentId)
                        .withRequest(request)
                        .withResponse(response)
                        .withPage(offset)
                        .withPerPage(limit)
                        .build();

        return paginationUtil.getPageView(params);
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
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(
            operationId = "saveDraft",
            summary = "Saves a content draft",
            description = "Creates or updates a draft version of a contentlet without triggering workflow. " +
                    "Drafts allow content editors to save work in progress without publishing.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Draft saved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityContentletView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid content data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks write permissions"),
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final ResponseEntityContentletView saveDraft(@Context final HttpServletRequest request,
                                                     @Parameter(description = "Content inode for existing content") @QueryParam("inode") final String inode,
                                                     @Parameter(description = "Content identifier for existing content") @QueryParam("identifier") final String identifier,
                                                     @Parameter(description = "Index policy (DEFER_UNTIL_PUBLISH, FORCE, WAIT_FOR)") @QueryParam("indexPolicy") final String indexPolicy,
                                                     @Parameter(description = "Language ID for content localization") @DefaultValue("-1") @QueryParam("language") final String language,
                                                     @RequestBody(description = "Content data and field values",
                                                             required = true,
                                                             content = @Content(schema = @Schema(implementation = ContentForm.class)))
                                                     final ContentForm contentForm) throws DotDataException, DotSecurityException {
        final InitDataObject initDataObject = new WebResource.InitBuilder().requestAndResponse(request,
                new MockHttpResponse()).requiredAnonAccess(AnonymousAccess.WRITE).init();
        Logger.debug(this, () -> "On Saving Draft, inode = " + inode + ", identifier = " + identifier + ", language =" +
                                         " " + language + " indexPolicy = " + indexPolicy);
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode = PageMode.get(request);
        final Contentlet contentlet = this.getContentlet(inode, identifier, languageId,
                () -> this.languageWebAPI.getLanguage(request).getId(), contentForm,
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
    @Operation(
            operationId = "getContent",
            summary = "Retrieves a contentlet by identifier or inode",
            description = "Returns a single contentlet based on its identifier or inode. " +
                    "This is the primary endpoint for fetching content data with support for language, variants, and relationship depth.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contentlet retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityMapView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid identifier format"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks read permissions"),
                    @ApiResponse(responseCode = "404", description = "Contentlet not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getContent(@Context HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @Parameter(description = "Contentlet identifier or inode") @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                               @Parameter(description = "Language ID for content localization") @DefaultValue("") @QueryParam("language") final String language,
                               @Parameter(description = "Variant name for A/B testing") @DefaultValue("DEFAULT") @QueryParam("variantName") final String variantName,
                               @Parameter(description = "Relationship depth to include (-1 for no relationships)") @DefaultValue("-1") @QueryParam("depth") final int depth


    ) {

        final User user =
                new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .init().getUser();

        Logger.debug(this, () -> "Finding the contentlet: " + inodeOrIdentifier);

        final LongSupplier sessionLanguageSupplier = ()-> languageWebAPI.getLanguage(request).getId();
        final PageMode mode   = PageMode.get(request);
        final long testLangId = LanguageUtil.getLanguageId(language);
        final long languageId = testLangId <=0 ? sessionLanguageSupplier.getAsLong() : testLangId;

        Contentlet contentlet = this.resolveContentletOrFallBack(inodeOrIdentifier, mode, languageId, user, variantName);

        if (-1 != depth) {
            ContentUtils.addRelationships(contentlet, user, mode,
                    languageId, depth, request, response);
        }
        final String variant = contentlet.getVariantId();
        contentlet = new DotTransformerBuilder().contentResourceOptions(false).content(contentlet).build().hydrate().get(0);
        contentlet.setVariantId(variant);
        return Response.ok(new ResponseEntityView<>(
                WorkflowHelper.getInstance().contentletToMap(contentlet))).build();
    }

    /**
     * Retrieves the count of a Contentlet based on the identifier
     * If the contentlet exist or not, does not matter, the count will be returned
     *
     * @param request  The current {@link HttpServletRequest} instance.
     * @param response The current {@link HttpServletResponse} instance.
     *
     * @return The {@link ResponseEntityCountView}
     */
    @Operation(
        summary = "Get contentlet references count",
        description = "Retrieves the total number of references to a specific contentlet by its identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "References count retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityCountView.class))),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Not found - contentlet with identifier not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{identifier}/references/count")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityCountView getAllContentletReferencesCount(
                            @Context HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @Parameter(description = "Content identifier", required = true)
                               @PathParam("identifier") final String identifier
    ) throws DotDataException {

        new WebResource.InitBuilder(this.webResource)
                        .requestAndResponse(request, response)
                        .requiredAnonAccess(AnonymousAccess.READ)
                        .init();

        Logger.debug(this, () -> "Finding the counts for contentlet id: " + identifier);
        final Optional<Integer> count = this.contentletAPI.getAllContentletReferencesCount(identifier);

        if (!count.isPresent()) {
            throw new DoesNotExistException("The contentlet with identifier " + identifier + " does not exist");
        }

        return new ResponseEntityCountView(new CountView(count.get()));
    }

    /**
     * Retrieves the references of a Contentlet based on the identifier
     * If the contentlet does not exist, 404
     *
     * @param request  The current {@link HttpServletRequest} instance.
     * @param response The current {@link HttpServletResponse} instance.
     *
     * @return The {@link ResponseEntityView<List<ContentReferenceView>>}
     */
    @Operation(
        summary = "Get contentlet references",
        description = "Retrieves all references to a specific contentlet, including pages, containers, and personas that reference it"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "References retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityContentReferenceListView.class))),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Not found - contentlet not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{inodeOrIdentifier}/references")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityContentReferenceListView getContentletReferences(
            @Context HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Content inode or identifier", required = true)
            @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
            @Parameter(description = "Language ID for content localization", example = "1")
            @DefaultValue("") @QueryParam("language") final String language
    ) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .init().getUser();

        Logger.debug(this, () -> "Finding the references for contentlet id: " + inodeOrIdentifier);

        final LongSupplier sessionLanguageSupplier = ()-> languageWebAPI.getLanguage(request).getId();
        final PageMode mode   = PageMode.get(request);
        final long testLangId = LanguageUtil.getLanguageId(language);
        final long languageId = testLangId <=0 ? sessionLanguageSupplier.getAsLong() : testLangId;

        final Contentlet contentlet = this.resolveContentletOrFallBack(inodeOrIdentifier, mode, languageId, user);

        final List<Map<String, Object>>  references = this.contentletAPI.getContentletReferences(contentlet, user, mode.respectAnonPerms);
        final List<ContentReferenceView> contentReferenceViews = Objects.nonNull(references)?
                references.stream()
                .map(reference -> new ContentReferenceView(
                        (IHTMLPage) reference.get("page"),
                        new ContainerView((Container) reference.get("container")),
                        (String) reference.get("personaName")))
                .collect(Collectors.toList()):
                List.of();
        return new ResponseEntityContentReferenceListView(contentReferenceViews);
    }

    /**
     * Checks whether a given contentlet can be locked by the current user.
     *
     * @param request            the HTTP servlet request, containing information about the client request.
     * @param response           the HTTP servlet response, used for setting response parameters.
     * @param inodeOrIdentifier  the inode or identifier of the contentlet to be checked.
     * @param language           the language ID of the contentlet (optional, defaults to -1 for fallback).
     * @return a ResponseEntityView containing:
     *         <ul>
     *             <li>"canLock": boolean indicating if the contentlet can be locked by the user.</li>
     *             <li>"locked": boolean indicating if the contentlet is currently locked.</li>
     *             <li>"lockedBy": (optional) user ID of the user who locked the contentlet, if locked.</li>
     *             <li>"lockedOn": (optional) timestamp when the contentlet was locked, if locked.</li>
     *             <li>"lockedByName": (optional) name of the user who locked the contentlet, if locked.</li>
     *             <li>"inode": the inode of the contentlet.</li>
     *             <li>"id": the identifier of the contentlet.</li>
     *         </ul>
     * @throws DotDataException        if there is a data access issue.
     * @throws DotSecurityException    if the user does not have the required permissions.
     * @throws DoesNotExistException   if the contentlet does not exist.
     */
    @GET
    @Path("/_canlock/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "canLockContent", summary = "Check if a contentlet can be locked",
            description = "Checks if the contentlet specified by its inode or identifier can be locked by the current user.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved lock status",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityMapView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "401", description = "Invalid User"), // not logged in
                    @ApiResponse(responseCode = "403", description = "Forbidden"), // no permission
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public ResponseEntityView<Map<String, Object>> canLockContent(@Context HttpServletRequest request,
                                                                  @Context final HttpServletResponse response,
                                                                   @Parameter(description = "Contentlet inode or identifier", required = true) @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                                                                   @Parameter(description = "Language ID for content localization") @DefaultValue("-1") @QueryParam("language") final String language)
            throws DotDataException, DotSecurityException {

        final User user =
                new WebResource.InitBuilder(this.webResource)
                        .requestAndResponse(request, response)
                        .requiredAnonAccess(AnonymousAccess.READ)
                        .init().getUser();

        Logger.debug(this, () -> "Checking if the contentlet can be locked: " + inodeOrIdentifier);
        final PageMode mode   = PageMode.get(request);
        final long testLangId = LanguageUtil.getLanguageId(language);
        final long languageId = testLangId <=0 ? this.languageWebAPI.getLanguage(request).getId() : testLangId;
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

        return new ResponseEntityView<>(resultMap);
    }

    /**
     * Unlock a given contentlet by the current user.
     *
     * @param request            the HTTP servlet request, containing information about the client request.
     * @param response           the HTTP servlet response, used for setting response parameters.
     * @param inodeOrIdentifier  the inode or identifier of the contentlet to be checked.
     * @param language           the language ID of the contentlet (optional, defaults to -1 for fallback).
     * @return a ResponseEntityMapView return the contentlet unlock
     *
     * @throws DotDataException        if there is a data access issue.
     * @throws DotSecurityException    if the user does not have the required permissions.
     * @throws DoesNotExistException   if the contentlet does not exist.
     */
    @PUT
    @Path("/_unlock/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "unlockContent", summary = "Unlock a given contentlet by the current user",
            description = "If the user is allowed to unlock the contentlet specified by its inode or identifier, " +
                    "the contentlet will be unlocked.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully unlocked contentlet",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityMapView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "401", description = "Invalid User"), // not logged in
                    @ApiResponse(responseCode = "403", description = "Forbidden"), // no permission
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public ResponseEntityMapView unlockContent(@Context HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @Parameter(description = "Contentlet inode or identifier", required = true) @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                                  @Parameter(description = "Language ID for content localization") @DefaultValue("-1") @QueryParam("language") final String language)
            throws DotDataException, DotSecurityException {

        final User user =
                new WebResource.InitBuilder(this.webResource)
                        .requestAndResponse(request, response)
                        .requiredAnonAccess(AnonymousAccess.WRITE)
                        .init().getUser();

        Logger.debug(this, () -> "Unlocking the contentlet: " + inodeOrIdentifier);
        final PageMode mode   = PageMode.get(request);
        final long testLangId = LanguageUtil.getLanguageId(language);
        final long languageId = testLangId <=0 ? this.languageWebAPI.getLanguage(request).getId() : testLangId;
        final Contentlet contentlet = this.resolveContentletOrFallBack(inodeOrIdentifier, mode, languageId, user);

        APILocator.getContentletAPI().unlock(contentlet, user, mode.respectAnonPerms);

        final Contentlet contentletHydrated = new DotTransformerBuilder().contentResourceOptions(false)
                .content(contentlet).build().hydrate().get(0);
        return new ResponseEntityMapView(WorkflowHelper.getInstance().contentletToMap(contentlet));
    }

    /**
     * Lock a given contentlet by the current user.
     *
     * @param request            the HTTP servlet request, containing information about the client request.
     * @param response           the HTTP servlet response, used for setting response parameters.
     * @param inodeOrIdentifier  the inode or identifier of the contentlet to be checked.
     * @param language           the language ID of the contentlet (optional, defaults to -1 for fallback).
     * @return a ResponseEntityMapView return the contentlet locked
     *
     * @throws DotDataException        if there is a data access issue.
     * @throws DotSecurityException    if the user does not have the required permissions.
     * @throws DoesNotExistException   if the contentlet does not exist.
     */
    @PUT
    @Path("/_lock/{inodeOrIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "lockContent", summary = "Lock a given contentlet by the current user",
            description = "If the user is allowed to lock the contentlet specified by its inode or identifier, " +
                    "the contentlet will be locked.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully locked contentlet",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityMapView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"), // invalid param string like `\`
                    @ApiResponse(responseCode = "401", description = "Invalid User"), // not logged in
                    @ApiResponse(responseCode = "403", description = "Forbidden"), // no permission
                    @ApiResponse(responseCode = "404", description = "Content not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public ResponseEntityMapView lockContent(@Context HttpServletRequest request,
                                             @Context HttpServletResponse response,
                                             @Parameter(description = "Contentlet inode or identifier", required = true) @PathParam("inodeOrIdentifier") final String inodeOrIdentifier,
                                             @Parameter(description = "Language ID for content localization") @DefaultValue("-1") @QueryParam("language") final String language)
            throws DotDataException, DotSecurityException {

        final User user =
                new WebResource.InitBuilder(this.webResource)
                        .requestAndResponse(request, response)
                        .requiredAnonAccess(AnonymousAccess.WRITE)
                        .init().getUser();

        Logger.debug(this, () -> "Locking the contentlet: " + inodeOrIdentifier);
        final PageMode mode   = PageMode.get(request);
        final long testLangId = LanguageUtil.getLanguageId(language);
        final long languageId = testLangId <=0 ? this.languageWebAPI.getLanguage(request).getId() : testLangId;
        final Contentlet contentlet = this.resolveContentletOrFallBack(inodeOrIdentifier, mode, languageId, user);

        APILocator.getContentletAPI().lock(contentlet, user, mode.respectAnonPerms);

        final Contentlet contentletHydrated = new DotTransformerBuilder().contentResourceOptions(false)
                .content(contentlet).build().hydrate().get(0);
        return new ResponseEntityMapView(WorkflowHelper.getInstance().contentletToMap(contentlet));
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
        return resolveContentletOrFallBack (inodeOrIdentifier, mode, languageId, user, VariantAPI.DEFAULT_VARIANT.name());
    }
    /**
     * Given an inode or identifier, this method will return the contentlet object for the given language
     * If no contentlet is found, it will return the contentlet for the default language if FallBack is enabled
     * @param inodeOrIdentifier the inode or identifier to test
     * @param mode the page mode used to determine if we are
     * @param languageId the language id
     * @param user the user
     * @param variantName  variant name
     * @return the contentlet object
     */
    private Contentlet resolveContentletOrFallBack (final String inodeOrIdentifier, final PageMode mode, final long languageId, final User user,
                                                    final String variantName) {
        // This property is used to determine if we should map the contentlet to the default language
        final boolean mapToDefault = isDefaultContentToDefaultLanguageEnabled.get();
        // default language supplier, we only get the language id if we need it
        LongSupplier defaultLang = () -> APILocator.getLanguageAPI().getDefaultLanguage().getId();
        //Attempt to resolve the contentlet for the given language
        Optional<Contentlet> optional = resolveContentlet(inodeOrIdentifier, mode, languageId, user, variantName);
        //If the contentlet is not found, and we are allowed to map to the default language..
        if(optional.isEmpty() && mapToDefault && languageId != defaultLang.getAsLong()){
            //Attempt to resolve the contentlet for the default language
             optional = resolveContentlet(inodeOrIdentifier, mode, defaultLang.getAsLong(), user, variantName);
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
     * @param variantName variant name
     * @return the contentlet object
     */
    private Optional<Contentlet> resolveContentlet (final String inodeOrIdentifier,
                                                    final PageMode mode,
                                                    final long languageId,
                                                    final User user,
                                                    final String variantName) {

        final Optional<ShortyId> shortyId = APILocator.getShortyAPI().getShorty(inodeOrIdentifier);

        if (shortyId.isEmpty()) {
            throw new DoesNotExistException(getDoesNotExistMessage(inodeOrIdentifier, languageId));
        }

        String testInode = inodeOrIdentifier;

        final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
        if(ShortType.IDENTIFIER == shortyId.get().type) {

            final Optional<ContentletVersionInfo> cvi = versionableAPI.getContentletVersionInfo(shortyId.get().longId, languageId, variantName);
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
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("related")
    @Operation(
        summary = "Pull Related Content",
        description = "Retrieves related content for a contentlet based on relationship field configuration and query conditions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Related content retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityListMapView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Bad request - contentlet does not have a relationship field",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Not found - contentlet not found",
                    content = @Content(mediaType = "application/json"))
    })
    public Response pullRelated(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @RequestBody(description = "Pull related content request parameters",
                                          required = true,
                                          content = @Content(schema = @Schema(implementation = PullRelatedForm.class)))
                               final PullRelatedForm pullRelatedForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredAnonAccess(AnonymousAccess.READ).init();

        Logger.debug(this, ()-> "Requesting pull related parents for the contentletIdentifier: " + pullRelatedForm.getIdentifier() +
                ", relationshipFieldVariable: " + pullRelatedForm.getFieldVariable() + ", luceneCondition: " + pullRelatedForm.getCondition() +
                ", limit: " + pullRelatedForm.getLimit() + ", offset: " + pullRelatedForm.getOffset() + ", orderby: " + pullRelatedForm.getOrderBy());

        final User user = initData.getUser();
        final Language language = this.languageWebAPI.getLanguage(request);
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


    /**
     * Receives the Identifier of a {@link Contentlet }, returns all the available languages in
     * dotCMS and, for each of them, adds a flag indicating whether the Contentlet is available in
     * that language or not. This may be particularly useful when requiring the system to provide a
     * specific action when a Contentlet is NOT available in a given language. Here's an example of
     * how you can use it:
     * <pre>
     *     GET <a href="http://localhost:8080/api/v1/content/${CONTENT_ID}/languages">http://localhost:8080/api/v1/content/${CONTENT_ID}/languages</a>
     * </pre>
     *
     * @param request    The current instance of the {@link HttpServletRequest}.
     * @param response   The current instance of the {@link HttpServletResponse}.
     * @param identifier The Identifier of the Contentlet whose available languages will be
     *                   checked.
     *
     * @return A {@link Response} object containing the list of languages and the flag indicating
     * whether the Contentlet is available in such a language or not.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @Operation(
        summary = "Check content language versions",
        description = "Retrieves all available languages for a contentlet and indicates which languages have content versions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Language versions retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Not found - contentlet identifier not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{identifier}/languages")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityView<List<ExistingLanguagesForContentletView>> checkContentLanguageVersions(@Context final HttpServletRequest request,
                                                                                                     @Context final HttpServletResponse response,
                                                                                                     @Parameter(description = "Identifier of contentlet whose language status to display.") @PathParam("identifier") final String identifier) throws DotDataException {
        Logger.debug(this, () -> String.format("Check the languages that Contentlet '%s' is " +
                "available on", identifier));
        final User user = new WebResource.InitBuilder(webResource).requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(false)
                .init()
                .getUser();
        final Identifier identifierBean = APILocator.getIdentifierAPI().find(identifier);
        if (Objects.isNull(identifierBean) || !InodeUtils.isSet(identifierBean.getId())) {

            throw new DoesNotExistException("Identifier not found for id: " + identifier);
        }
        final List<ExistingLanguagesForContentletView> languagesForContent =
                this.getExistingLanguagesForContent(identifier, user);
        return new ResponseEntityView<>(languagesForContent);
    }

    /**
     * Returns a list of ALL languages in dotCMS and, for each of them, adds a boolean indicating
     * whether the specified Contentlet Identifier is available in such a language or not. This is
     * particularly useful for the UI layer to be able to easily check what languages a Contentlet
     * is available on, and what languages it is not.
     *
     * @param identifier The Identifier of the {@link Contentlet} whose languages are being
     *                   checked.
     * @param user       The {@link User} performing this action.
     *
     * @return The list of languages and the flag indicating whether the Contentlet is available in
     * such a language or not.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    private List<ExistingLanguagesForContentletView> getExistingLanguagesForContent(final String identifier, final User user) throws DotDataException {
        DotPreconditions.checkNotNull(identifier, "Contentlet ID cannot be null");
        DotPreconditions.checkNotNull(user, "User cannot be null");
        final ImmutableList.Builder<ExistingLanguagesForContentletView> languagesForContent = new ImmutableList.Builder<>();
        final Set<Long> existingContentLanguages =
                APILocator.getVersionableAPI().findContentletVersionInfos(identifier)
                .stream().map(ContentletVersionInfo::getLang)
                .collect(Collectors.toSet());
        final List<Language> allLanguages = this.languageAPI.getLanguages();
        allLanguages.forEach(language -> languagesForContent.add(new ExistingLanguagesForContentletView(language,
                existingContentLanguages.contains(language.getId()))));
        return languagesForContent.build();
    }

    /**
     * Allows you to retrieve contents via Elasticsearch by abstracting the generation of the Lucene
     * query, and leaving it to dotCMS itself. This endpoint uses the {@link ContentSearchForm} to
     * define the search parameters, and returns the same {@link SearchView} object that the
     * {@link com.dotcms.rest.ContentResource#search(HttpServletRequest, HttpServletResponse,
     * boolean, SearchForm)} endpoint returns.
     *
     * @param request           The current instance of the {@link HttpServletRequest}.
     * @param response          The current instance of the {@link HttpServletResponse}.
     * @param contentSearchForm The {@link ContentSearchForm} object containing the search
     *                          parameters.
     *
     * @return The {@link SearchView} object containing the search results.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The User accessing this endpoint doesn't have the required
     *                              permissions.
     */
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/search")
    @Operation(operationId = "search", summary = "Retrieves content from the dotCMS repository",
            description = "Abstracts the generation of the required Lucene query to look for user searchable fields " +
                    "in a Content Type, and returns the expected results.",
            tags = {"Content"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The query has been executed. It's possible that " +
                            "no contents matched the search criteria.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SearchView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request. Malformed JSON body"),
                    @ApiResponse(responseCode = "401", description = "Invalid User, or not logged in"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public ResponseEntityView<SearchView> search(@Context final HttpServletRequest request,
                             @Context final HttpServletResponse response,
                             @RequestBody(description = "Content search parameters", required = true,
                                        content = @Content(schema = @Schema(implementation = ContentSearchForm.class),
                                                examples = @ExampleObject(
                                                             value = "{\n" +
                                                             "  \"globalSearch\": \"test\",\n" +
                                                             "  \"perPage\": 20,\n" +
                                                             "  \"page\": 1\n" +
                                                             "}")
                                        ))
                             final ContentSearchForm contentSearchForm) throws DotDataException, DotSecurityException {
        Logger.debug(this, () -> "Searching for contentlets with the following parameters: " + contentSearchForm);
        final User user = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true)
                .init()
                .getUser();
        final PageMode pageMode = PageMode.get(request);
        final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, user);
        final SearchView searchView = this.contentHelper.pullContent(request, response, user,
                luceneQueryBuilder.build(), contentSearchForm.offset(), contentSearchForm.perPage(),
                luceneQueryBuilder.getOrderByClause(), pageMode);
        return new ResponseEntityView<>(searchView);
    }

}
