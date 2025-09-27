package com.dotcms.rest.api.v2.tags;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityListView;
import com.dotcms.rest.ResponseEntityRestTagListView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.PaginationUtilParams;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.TagsPaginator;
import com.dotcms.rest.ResponseEntityTagOperationView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.tag.RestTag;
import com.dotcms.rest.tag.TagsResourceHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.rest.tag.TagsResourceHelper.toRestTagMap;
import static com.dotmarketing.util.UUIDUtil.isUUID;

/**
 * This REST Endpoint provide CRUD operations for Tags in dotCMS.
 *
 * @author jsanca
 */
@Path("/v2/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Content tagging and labeling")
public class TagResource {

    public static final String NO_TAGS_WERE_FOUND_BY_THE_INODE_S = "No tags with Inode %s were found.";

    private final WebResource webResource;
    private final TagAPI tagAPI;
	private final TagsResourceHelper helper;

    @SuppressWarnings("unused")
    public TagResource() {
        this(APILocator.getTagAPI(), APILocator.getHostAPI(), APILocator.getFolderAPI(), new WebResource());
    }

    /**
     * Class constructor Integration Tests, if required.
     *
     * @param tagAPI      Singleton instance of the {@link TagAPI}.
     * @param hostAPI     Singleton instance of the {@link HostAPI}.
     * @param folderAPI   Singleton instance of the {@link FolderAPI}.
     * @param webResource The {@link WebResource} object containing authentication data, parameters,
     *                    etc.
     */
    @VisibleForTesting
    protected TagResource(final TagAPI tagAPI, final HostAPI hostAPI, final FolderAPI folderAPI, final WebResource webResource) {
        this.tagAPI = tagAPI;
        this.webResource = webResource;
        this.helper = new TagsResourceHelper(tagAPI, hostAPI, folderAPI);
    }

    /**
     * Searches and lists tags with filtering, pagination, and sorting.
     * The filter parameter performs a case-insensitive search with wildcards on both sides
     * (e.g., "market" matches "Marketing", "marketplace", "supermarket").
     * When using the filter, results are ordered by match length (shortest first) to prioritize exact matches.
     * The site parameter accepts either a site ID or site name for filtering by specific sites.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param filter   Tag name filter (LIKE search).
     * @param global   Include system/global tags.
     * @param site     Filter by site (ID, name, or SYSTEM_HOST).
     * @param page     Page number.
     * @param perPage  Items per page.
     * @param orderBy  Sort field.
     * @param direction Sort direction.
     *
     * @return The {@link ResponseEntityPaginatedDataView} containing the paginated list of Tags.
     */
    @Operation(
        summary = "List/Search Tags",
        description = "Searches and lists tags with filtering, pagination, and sorting. " +
                      "The filter parameter performs a case-insensitive search with wildcards " +
                      "on both sides (e.g., \"market\" matches \"Marketing\", \"marketplace\", \"supermarket\"). " +
                      "When using the filter, results are ordered by match length " +
                      "(shortest first) to prioritize exact matches. " +
                      "The site parameter accepts either a site ID or site name for filtering by specific sites."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Tags retrieved successfully with pagination metadata",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityPaginatedDataView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Bad Request - Invalid parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityPaginatedDataView list(
        @Context final HttpServletRequest request,
        @Context final HttpServletResponse response,
        @Parameter(description = "Tag name filter (LIKE search)", example = "market")
        @QueryParam("filter") final String filter,
        @Parameter(description = "Include system/global tags", example = "true")
        @QueryParam("global") @DefaultValue("false") final Boolean global,
        @Parameter(description = "Filter by site (ID, name, or SYSTEM_HOST)",
                   example = "48190c8c-42c4-46af-8d1a-0cd5db894797")
        @QueryParam("site") final String site,
        @Parameter(description = "Page number", example = "1")
        @QueryParam("page") @DefaultValue("1") final Integer page,
        @Parameter(description = "Items per page", example = "25")
        @QueryParam("per_page") final Integer perPage,
        @Parameter(description = "Sort field", example = "tagname")
        @QueryParam("orderBy") @DefaultValue("tagname") final String orderBy,
        @Parameter(description = "Sort direction", example = "ASC")
        @QueryParam("direction") @DefaultValue("ASC") final String direction
    ) throws DotDataException {
        // 1. Initialize and validate
        final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
                .requiredAnonAccess(AnonymousAccess.READ)
                .requestAndResponse(request, response)
                .init();

        final User user = initDataObject.getUser();

        // 2. Resolve site parameter
        final String resolvedSiteId = helper.resolveSiteParameter(site, user, request);

        Logger.debug(this, UtilMethods.isSet(filter)
                ? String.format("Filtering Tag(s) '%s' from Site '%s', global=%s, page=%d, perPage=%s",
                                filter, resolvedSiteId, global, page, perPage)
                : String.format("Listing ALL Tags from Site '%s', global=%s, page=%d, perPage=%s",
                                resolvedSiteId, global, page, perPage));

        // 3. Use PaginationUtil with TagsPaginator
        final PaginationUtil paginationUtil = new PaginationUtil(new TagsPaginator());

        // 4. Build extra parameters for site and global filtering
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(TagsPaginator.FILTER_PARAM, filter);
        extraParams.put(TagsPaginator.SITE_ID_PARAM, resolvedSiteId);
        extraParams.put(TagsPaginator.GLOBAL_PARAM, global);

        // 5. Build pagination parameters - let PaginationUtil handle validation and defaults
        final PaginationUtilParams<RestTag, List<RestTag>> params = new PaginationUtilParams.Builder<RestTag, List<RestTag>>()
            .withRequest(request)
            .withResponse(response)
            .withUser(user)
            .withFilter(filter)
            .withPage(page != null ? page : 1)
            .withPerPage(perPage != null ? perPage : 0)  // Let PaginationUtil apply config default
            .withOrderBy(orderBy)
            .withDirection("DESC".equalsIgnoreCase(direction) ? OrderDirection.DESC : OrderDirection.ASC)
            .withExtraParams(extraParams)
            .build();

        // 6. Return paginated response using standard pattern
        return paginationUtil.getPageView(params);
    }

    /**
     * Creates one or more tags. Accepts a list of tag objects for unified single and bulk operations.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param tagForms The list of {@link TagForm} objects containing the tags to create.
     *
     * @return The {@link ResponseEntityListView} containing the created tags.
     */
    @Operation(
            summary = "Create tags",
            description = "Creates one or more tags. Single tag = list with one element, multiple tags = list with multiple elements. This operation is idempotent - existing tags are returned without error."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Tags created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityRestTagListView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad Request - Invalid tag data with field-level error details",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - User does not have access to Tags portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal Server Error - Database or system error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response createTags(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "List of tag data to create. Single tag = list with one element, multiple tags = list with multiple elements.",
                    required = true,
                    content = @Content(schema = @Schema(type = "array", implementation = TagForm.class)))
            final List<TagForm> tagForms) throws DotDataException, DotSecurityException {

        // Initialize and check permissions
        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();

        Logger.debug(TagResource.class,()->String.format("User '%s' is adding %d tag(s)", user.getUserId(), tagForms.size()));

        // Validate all tags upfront - fail fast with structured error
        for (final TagForm form : tagForms) {
            form.checkValid(); // ValidationException (a BadRequestException) will propagate with correct messages
        }

        // Create all tags
        final List<Tag> savedTags = saveTags(request, tagForms, user);

        // Convert to RestTag list for response
        final List<RestTag> resultList = savedTags.stream()
            .map(TagsResourceHelper::toRestTag)
            .collect(Collectors.toList());

        return Response.status(Response.Status.CREATED)
                .entity(new ResponseEntityRestTagListView(resultList))
                .build();
    }


    /**
     * Saves Tags in dotCMS using a list-based approach.
     *
     * @param request   The current instance of the {@link HttpServletRequest}.
     * @param tagForms  The {@link List} of {@link TagForm} containing the Tags to save.
     * @param user      The {@link User} performing the operation.
     *
     * @return List of created {@link Tag} objects.
     * @throws DotDataException     An error occurred when persisting Tag data.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *                              perform this operation.
     */
    @WrapInTransaction
    private List<Tag> saveTags(final HttpServletRequest request,
                               final List<TagForm> tagForms,
                               final User user)
            throws DotDataException, DotSecurityException {

        final List<Tag> savedTags = new ArrayList<>();

        for (TagForm form : tagForms) {
            // Resolve site
            final String siteId = helper.getValidateSite(form.getSiteId(), user, request);

            // Create or get tag
            final boolean persona = (form.getPersona() != null) ? form.getPersona() : false;
            final Tag tag = tagAPI.getTagAndCreate(
                form.getName(),
                form.getOwnerId(),
                siteId,
                persona,
                false
            );

            Logger.debug(TagResource.class, String.format("Saving Tag '%s'", tag.getTagName()));

            // Bind to owner if specified
            if (UtilMethods.isSet(form.getOwnerId())) {
                tagAPI.addUserTagInode(tag, form.getOwnerId());
                Logger.debug(TagResource.class,
                    String.format("Tag '%s' is now bound to user '%s'",
                        tag.getTagName(), form.getOwnerId()));
            }

            savedTags.add(tag);
        }

        return savedTags;
    }

    /**
     * Updates a tag's name and/or site assignment.
     * You can identify the tag by its UUID or by its name.
     * When using a tag name, you must specify which site's tag you want to update via the siteId query parameter.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param idOrName The UUID or name of the tag to update.
     * @param siteId   Required when idOrName is a tag name (for identification).
     * @param tagForm  The {@link UpdateTagForm} containing the updated tag data.
     *
     * @return The {@link ResponseEntityRestTagView} containing the updated tag.
     */
    @Operation(
            summary = "Update tag",
            description = "Updates a tag's name and site assignment. You can identify the tag by its UUID or by its name. When using a tag name, you must specify which site's tag you want to update via the siteId query parameter."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Tag updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityRestTagView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad Request - Invalid input data",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "Tag not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409",
                    description = "Conflict - Tag name already exists on target site",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - User does not have access to Tags portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal Server Error - Database or system error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @JSONP
    @Path("/{idOrName}")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityRestTagView updateTag(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Tag UUID or tag name", required = true)
            @PathParam("idOrName") final String idOrName,
            @Parameter(description = "Site ID for name-based lookups. Required when idOrName is a tag name")
            @QueryParam("siteId") final String siteId,
            @RequestBody(description = "Updated tag data",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateTagForm.class)))
            final UpdateTagForm tagForm) throws DotDataException, DotSecurityException {

        // 1. Validate form upfront (like CREATE does)
        tagForm.checkValid();
        
        // 2. Initialize security context
        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        
        Logger.debug(TagResource.class, () -> String.format(
                "User '%s' is updating tag '%s' with data %s",
                user.getUserId(), idOrName, tagForm));

        // 3. Find the tag to update
        Tag existingTag = null;
        if (isUUID(idOrName)) {
            // Update by UUID
            existingTag = Try.of(() -> tagAPI.getTagByTagId(idOrName)).getOrNull();
        } else {
            // Update by name - require siteId query parameter
            if (!UtilMethods.isSet(siteId)) {
                throw new BadRequestException("siteId query parameter is required when updating tag by name");
            }
            // Use helper for FINDING the tag (with fallbacks)
            final String resolvedSiteId = helper.getValidateSite(siteId, user, request);
            existingTag = Try.of(() -> tagAPI.getTagByNameAndHost(idOrName, resolvedSiteId)).getOrNull();
        }
        
        if (existingTag == null) {
            throw new NotFoundException(String.format("Tag with id %s was not found", idOrName));
        }

        // 4. Validate target site exists - STRICT validation, no fallback
        final String targetSiteId;
        final Host targetHost = APILocator.getHostAPI().find(tagForm.getSiteId(), user, false);
        if (targetHost == null || UtilMethods.isNotSet(targetHost.getIdentifier())) {
            throw new BadRequestException(
                String.format("Site with ID '%s' does not exist", tagForm.getSiteId())
            );
        }
        targetSiteId = targetHost.getIdentifier();

        // 5. Check for duplicate if name or site is changing
        if (!existingTag.getTagName().equals(tagForm.getName()) ||
                !existingTag.getHostId().equals(targetSiteId)) {
            
            final Tag duplicateCheck = Try.of(() ->
                    tagAPI.getTagByNameAndHost(tagForm.getName(), targetSiteId)).getOrNull();
            
            if (duplicateCheck != null && !duplicateCheck.getTagId().equals(existingTag.getTagId())) {
                throw new BadRequestException(
                    String.format("Tag '%s' already exists for site '%s'", 
                        tagForm.getName(), targetSiteId)
                );
            }
        }

        // 6. Update the tag
        tagAPI.updateTag(existingTag.getTagId(), tagForm.getName(), true, targetSiteId);

        // 7. Get updated tag and return
        final Tag updatedTag = tagAPI.getTagByTagId(existingTag.getTagId());
        final RestTag restTag = TagsResourceHelper.toRestTag(updatedTag);
        
        return new ResponseEntityRestTagView(restTag);
    }


    /**
     * Retrieves all Tags owned by a given User if an owner was provided when saving such Tags.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param userId   The User ID that matches a given Tag.
     *
     * @return The {@link ResponseEntityTagMapView} containing the list of Tags that belong to a
     * User.
     */
    @GET
    @JSONP
    @Path("/user/{userId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagMapView getTagsByUserId(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("userId") final String userId) {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class, String.format("User '%s' is requesting tags owned by User '%s'",user.getUserId(), userId));

        final List<Tag> tags = Try.of(() -> tagAPI.getTagsForUserByUserId(userId)).getOrElse(List.of());
        if (tags.isEmpty()) {

            final String errorMessage = Try.of(() -> LanguageUtil
                    .get(user.getLocale(), "tag.user.not.found", userId))
                    .getOrElse(String.format("No tags are owned by user '%s'",
                            userId)); //fallback message
            Logger.error(TagResource.class, errorMessage);
            throw new DoesNotExistException(errorMessage);
        }

        return new ResponseEntityTagMapView(toRestTagMap(tags));
    }

    /**
     * Retrieves a Tag by name or ID. If the provided value is a valid UUID, a search-by-ID operation
     * will be performed. If it's a name, the tag will be searched within the specified site context.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param nameOrId The name or ID of the Tag to search for.
     * @param siteId   Optional site ID for name-based searches. If not provided, uses current site context with SYSTEM_HOST fallback.
     *
     * @return The {@link Response} containing the found Tag or error information.
     */
    @Operation(
            summary = "Get tag by name or ID",
            description = "Retrieves a single tag by its name or UUID. For name-based searches, uses site context for disambiguation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Tag found successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityRestTagView.class))),
            @ApiResponse(responseCode = "404",
                    description = "Tag not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - User does not have access to Tags portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/{nameOrId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityRestTagView getTagsByNameOrId(@Context final HttpServletRequest request,
                                                       @Context final HttpServletResponse response,
                                                       @Parameter(description = "Tag name or UUID", required = true)
                                                       @PathParam("nameOrId") final String nameOrId,
                                                       @Parameter(description = "Site ID for name-based searches. If not provided, uses current site context with SYSTEM_HOST fallback")
                                                       @QueryParam("siteId") final String siteId) throws DotDataException {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        
        Logger.debug(TagResource.class, () -> String.format(
            "User '%s' is requesting tag by name or ID '%s' with siteId '%s'", 
            user.getUserId(), nameOrId, siteId));
        
        Tag tag = null;
        if (isUUID(nameOrId)) {
            // Get by ID - no site resolution needed
            tag = Try.of(() -> tagAPI.getTagByTagId(nameOrId)).getOrNull();
        } else {
            // Get by name with site resolution
            final String resolvedSiteId = helper.getValidateSite(siteId, user, request);
            tag = Try.of(() -> tagAPI.getTagByNameAndHost(nameOrId, resolvedSiteId)).getOrNull();
            
            // If tag is not found in current site and we're not already checking SYSTEM_HOST, try SYSTEM_HOST as fallback
            if (tag == null && !Host.SYSTEM_HOST.equals(resolvedSiteId)) {
                Logger.debug(TagResource.class, () -> String.format(
                    "Tag '%s' not found in site '%s', trying SYSTEM_HOST fallback", nameOrId, resolvedSiteId));
                tag = Try.of(() -> tagAPI.getTagByNameAndHost(nameOrId, Host.SYSTEM_HOST)).getOrNull();
            }
        }
        
        if (tag == null) {
            Logger.warn(TagResource.class, String.format(
                "Tag not found: nameOrId='%s', siteId='%s'", nameOrId, siteId));
            throw new DoesNotExistException(String.format("Tag not found: %s", nameOrId));
        }
        
        final RestTag restTag = TagsResourceHelper.toRestTag(tag);
        return new ResponseEntityRestTagView(restTag);
    }

    /**
     * Deletes one or more tags based on their IDs.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param deleteRequest The request body containing the tag IDs to delete.
     *
     * @return A {@link ResponseEntityBooleanView} containing the result of the delete operation.
     */
    @Operation(
        summary = "Delete tags",
        description = "Deletes one or more tags by their IDs"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid request - tagIds field is required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityBooleanView delete(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @RequestBody(description = "Tag IDs to delete", required = true,
                                                       content = @Content(schema = @Schema(type = "object", 
                                                                                         description = "Object containing array of tag IDs",
                                                                                         example = "{\"tagIds\": [\"tag-123\", \"tag-456\", \"tag-789\"]}")))
                                            final java.util.Map<String, Object> deleteRequest) throws DotDataException {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();

        // Extract tag IDs from JSON object
        @SuppressWarnings("unchecked")
        final java.util.List<String> tagIds = (java.util.List<String>) deleteRequest.get("tagIds");

        if (tagIds == null || tagIds.isEmpty()) {
            throw new BadRequestException("tagIds field is required and cannot be empty");
        }

        Logger.debug(TagResource.class, () -> String.format(
            "User '%s' is deleting %d tag(s): %s", 
            user.getUserId(), tagIds.size(), tagIds
        ));

        // Perform bulk delete
        tagAPI.deleteTags(tagIds.toArray(new String[0]));

        return new ResponseEntityBooleanView(true);
    }

    /**
     * Binds a Tag with a given inode. The lookup can be done via tag name or tag id. if the tag
     * name matches more than one tag, all the matching tags will be bound. So, if you want to be
     * more specific, provide the Tag ID instead of its name.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param nameOrId The name or ID of the Tag to link.
     * @param inode    The inode of the Tag to be linked.
     *
     * @return The {@link ResponseEntityTagInodesMapView} containing the list of linked Tags.
     */
    @PUT
    @JSONP
    @Path("/tag/{nameOrId}/inode/{inode}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagInodesMapView linkTagsAndInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("nameOrId") final String nameOrId,
            @PathParam("inode") final String inode) throws DotDataException {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class, String.format("User '%s' is linking Tag '%s' with inode '%s'",user.getUserId(), nameOrId, inode));
        final List<Tag> tags = new ArrayList<>();
        if (isUUID(nameOrId)){

            Logger.debug(TagResource.class, String.format("Trying to look up tag `%s` by name or id",nameOrId));
            final Tag tag = Try.of(()->tagAPI.getTagByTagId(nameOrId)).getOrNull();
            if(null != tag) {

               tags.add(tag);
            }
        } else {

            final List<Tag> tagsByName = Try.of(()->tagAPI.getTagsByName(nameOrId)).getOrNull();
            Logger.debug(TagResource.class, String.format("There are `%d` tags found under `%s`",tagsByName.size(), nameOrId));
            tags.addAll(tagsByName);
        }

        if (tags.isEmpty()) {

            Logger.error(TagResource.class, String.format("No Tags as `%s` were found",nameOrId));
            throw new NotFoundException(String.format("No tags were found by the name or id '%s'", nameOrId));
        }
        final List<TagInode> tagInodes = new ArrayList<>();
        for (final Tag tag : tags) {
            tagInodes.add(tagAPI.addUserTagInode(tag, inode));
        }
        return new ResponseEntityTagInodesMapView(tagInodes);
    }

    /**
     * Retrieves all Tags associated to a given Inode.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param inode    The inode of the Tags to search for.
     *
     * @return The {@link ResponseEntityTagInodesMapView} containing the list of Tags that match the
     * specified Inode.
     */
    @GET
    @JSONP
    @Path("/inode/{inode}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagInodesMapView findTagsByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("inode") final String inode) {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class, String.format("User '%s' is requesting tags by inode '%s'",user.getUserId(), inode));
        final List<TagInode> tagInodes = Try.of(() -> tagAPI.getTagInodesByInode(inode)).getOrElse(List.of());
        if (tagInodes.isEmpty()) {
            Logger.error(TagResource.class, String.format(NO_TAGS_WERE_FOUND_BY_THE_INODE_S,inode));
            throw new NotFoundException(String.format(NO_TAGS_WERE_FOUND_BY_THE_INODE_S, inode));
        }

        return new ResponseEntityTagInodesMapView(tagInodes);
    }

    /**
     * Breaks the link between an inode and all its associated Tags.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param inode    The inode of the Tags to delete.
     *
     * @return A {@link ResponseEntityBooleanView} containing the result of the delete operation.
     */
    @DELETE
    @JSONP
    @Path("/inode/{inode}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityBooleanView deleteTagInodesByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("inode") final String inode) throws DotDataException {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class, String.format("User '%s' is deleting Tags by inode '%s'",user.getUserId(), inode));
        final List<TagInode> tagInodes = Try.of(() -> tagAPI.getTagInodesByInode(inode))
                .getOrElse(List.of());
        if (tagInodes.isEmpty()) {
            Logger.error(TagResource.class, String.format(NO_TAGS_WERE_FOUND_BY_THE_INODE_S,inode));
            throw new NotFoundException(String.format(NO_TAGS_WERE_FOUND_BY_THE_INODE_S, inode));
        }

        tagAPI.deleteTagInodesByInode(inode);
        Logger.debug(TagResource.class, String.format("Tags with inode '%s' were removed successfully",inode));

        return new ResponseEntityBooleanView(true);
    }

    /**
     * Creates the Initialization Data Object, which is in charge of providing the necessary
     * authentication data, parameters, and restriction mechanisms when an any method of this
     * endpoint is called by any user. For this specific scenario, this endpoint can only be used by
     * dotCMS Back-End users with access to the {@code Tags} portlet.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     *
     * @return The {@link InitDataObject} containing the authentication and request data.
     */
    private InitDataObject getInitDataObject(final HttpServletRequest request,
                                             final HttpServletResponse response) {
        return
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(PortletID.TAGS.toString())
                        .init();
    }

    /**
     * Imports Tags from a CSV file with detailed row-level error reporting.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param form     The {@link FormDataMultiPart} containing the CSV file.
     *
     * @return A {@link ResponseEntityTagOperationView} containing import statistics and detailed error information.
     *
     * @throws DotDataException     An error occurred when persisting Tag data.
     * @throws IOException          An error occurred when reading the CSV file.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *                              perform this operation.
     */
    @Operation(
        summary = "Import tags from CSV file",
        description = "Imports tags from a CSV file with row-level error reporting. Returns detailed statistics and error information for each failed row."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                description = "Import completed with detailed results",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ResponseEntityTagOperationView.class))),
        @ApiResponse(responseCode = "400",
                description = "Bad Request - Invalid file format or content",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                description = "Unauthorized - Authentication required",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                description = "Forbidden - User does not have access to Tags portlet",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                description = "Internal Server Error - Database or system error",
                content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final ResponseEntityTagOperationView importTags(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "CSV file with tag data in format: tag_name,host_id",
                    required = true)
            final FormDataMultiPart form
    ) throws DotDataException, IOException, DotSecurityException {
        final InitDataObject initDataObject = getInitDataObject(request, response);

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class, String.format("User '%s' is importing Tags from CSV file.", user.getUserId()));

        // Get detailed import results
        final TagsResourceHelper.TagImportResult result = helper.importTags(form, user, request);

        // Build statistics map
        final Map<String, Object> stats = Map.of(
            "totalRows", result.totalRows,
            "successCount", result.successCount,
            "failureCount", result.errors.size(),
            "success", result.errors.isEmpty()
        );

        Logger.info(TagResource.class, String.format(
            "Tag import completed for user '%s': %d total, %d success, %d errors",
            user.getUserId(), result.totalRows, result.successCount, result.errors.size()));

        // Return a detailed response with statistics and errors
        return new ResponseEntityTagOperationView(
            stats,
            List.copyOf(result.errors)
    );
    }

    /**
     * Exports tags to CSV or JSON format.
     * Supports the same filtering as the list endpoint.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param format   The export format (csv or json).
     * @param global   Include global/system tags.
     * @param siteId   Filter by specific host/site.
     * @param filter   Tag name filter (LIKE search).
     *
     * @throws DotDataException     An error occurred when retrieving Tag data.
     * @throws DotSecurityException The specified user does not have the required permissions.
     */
    @Operation(
        summary = "Export tags",
        description = "Export tags to CSV or JSON format. Supports the same filtering as list/search endpoint with configurable export format."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "File download initiated with appropriate content type",
            content = {
                @Content(mediaType = "text/csv"),
                @Content(mediaType = "application/json")
            }),
        @ApiResponse(responseCode = "400",
            description = "Invalid parameters",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
            description = "Unauthorized - Authentication required",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
            description = "Forbidden - Insufficient permissions",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/export")
    @NoCache
    @Produces({"text/csv", "application/json"})
    public Response exportTags(
        @Context final HttpServletRequest request,
        @Context final HttpServletResponse response,
        @Parameter(description = "Export format", example = "csv",
                   schema = @Schema(allowableValues = {"csv", "json"}))
        @QueryParam("format") @DefaultValue("csv") final String format,
        @Parameter(description = "Include global tags", example = "false")
        @QueryParam("global") @DefaultValue("false") final Boolean global,
        @Parameter(description = "Filter by specific host/site", example = "48190c8c-42c4-46af-8d1a-0cd5db894797")
        @QueryParam("siteId") final String siteId,
        @Parameter(description = "Tag name filter (LIKE search)", example = "market")
        @QueryParam("filter") final String filter
    ) throws DotDataException, DotSecurityException {
        
        // Initialize and validate
        final InitDataObject initData = getInitDataObject(request, response);
        final User user = initData.getUser();
        
        // Validate format parameter
        if (!"csv".equalsIgnoreCase(format) && !"json".equalsIgnoreCase(format)) {
            throw new BadRequestException("Export format must be either 'csv' or 'json'");
        }
        
        Logger.debug(this, () -> String.format(
            "User '%s' exporting tags with format=%s, filter=%s, siteId=%s, global=%s",
            user.getUserId(), format, filter, siteId, global));
        
        // Delegate to helper with all parameters
        return helper.exportTags(request, response, format, global, siteId, filter, user);
    }

    /**
     * Downloads a CSV template file for tag imports.
     * No parameters required.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     */
    @Operation(
        summary = "Download tag import template",
        description = "Download a CSV template file with headers and example data for tag imports. No parameters required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "CSV template file download",
            content = @Content(mediaType = "text/csv")),
        @ApiResponse(responseCode = "401",
            description = "Unauthorized - Authentication required",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
            description = "Forbidden - Insufficient permissions",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
            description = "Template generation error",
            content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/export/template")
    @NoCache
    @Produces("text/csv")
    public Response downloadTemplate(
        @Context final HttpServletRequest request,
        @Context final HttpServletResponse response
    ) {
        
        // Ensure authenticated
        final InitDataObject initData = getInitDataObject(request, response);
        final User user = initData.getUser();
        
        Logger.debug(this, () -> String.format(
            "User '%s' downloading tag import template", user.getUserId()));
        
        return helper.downloadImportTemplate(response);
    }

}
