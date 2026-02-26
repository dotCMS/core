package com.dotcms.rest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.tag.RestTag;
import com.dotcms.rest.tag.TagForm;
import com.dotcms.rest.tag.TagsResourceHelper;
import com.dotcms.rest.tag.UpdateTagForm;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.api.v2.tags.ResponseEntityTagInodesMapView;
import com.dotcms.rest.api.v2.tags.ResponseEntityTagMapView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dotcms.rest.ResponseEntityView.OK;
import static com.dotcms.rest.tag.TagsResourceHelper.toRestTagMap;
import static com.dotmarketing.util.UUIDUtil.isUUID;

/**
 * Tag Related logic is exposed to the web here
 * @deprecated use /v2/tags instead
 * @see com.dotcms.rest.api.v2.tags.TagResource
 */
@Path("/v1/tags")
@Deprecated(since = "23.12")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags (v1)",
        description = "Legacy tag management endpoints (deprecated - use /api/v2/tags instead)")
public class TagResource {

    private static final String TAGS = "tags";
    private final WebResource webResource;
    private final TagAPI tagAPI;
	private final TagsResourceHelper helper;

    @SuppressWarnings("unused")
    public TagResource() {
        this(APILocator.getTagAPI(), APILocator.getHostAPI(), APILocator.getFolderAPI(), new WebResource());
    }

    /**
     * Test constructor
     * @param tagAPI
     * @param hostAPI
     * @param folderAPI
     * @param webResource
     */
    @VisibleForTesting
    protected TagResource(final TagAPI tagAPI,final HostAPI hostAPI, final FolderAPI folderAPI, final WebResource webResource) {
        this.tagAPI = tagAPI;
        this.webResource = webResource;
        this.helper = new TagsResourceHelper(tagAPI, hostAPI, folderAPI);
    }

    /**
     * This performs a list operation. But if a name is provided a search-by-name (like) operation will be performed instead.
     * The search-by-name operation can be delimited by a siteId
     * if No matches are found against the siteId the search-by-name operation will be performed against the global tags.
     * @param request
     * @param response
     * @param tagName
     * @param siteId
     * @return
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "listTagsV1",
            summary = "List or search tags (deprecated)",
            description = "Returns all tags or searches tags by name, optionally filtered by site. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tags returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(type = "object",
                                            description = "Map of tag names to RestTag objects"))),
                    @ApiResponse(responseCode = "401", description = "Authentication required")
            }
    )
    public Map<String, RestTag> list(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @Parameter(description = "Tag name to search for (like match)") @QueryParam("name") final String  tagName,
            @Parameter(description = "Site identifier to filter tags by") @QueryParam("siteId") final String siteId) {


      final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
      .requiredAnonAccess(AnonymousAccess.READ)
      .requestAndResponse(request, response)
      .init();

        final User user = initDataObject.getUser();

        final List<Tag> tags = UtilMethods.isSet(tagName)
                ? helper.searchTagsInternal(tagName, helper.getSiteId(siteId, request, user))
                : helper.getTagsInternal();

        return toRestTagMap(tags);
    }



    /**
     * Creates new tags and link them to a owner-user if provided
     * @param request
     * @param response
     * @param tagForm
     * @return
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "addTagV1",
            summary = "Create new tags (deprecated)",
            description = "Creates new tags and optionally links them to an owner user. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tags created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityTagMapView.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid tag data"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
            }
    )
    public Response addTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final TagForm tagForm) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is adding tag %s ",user.getUserId(),tagForm));

        //We can assign tags to any user as long as we are admin.
        final String userId = tagForm.getOwnerId();
        final ImmutableList.Builder<Tag> savedTags = ImmutableList.builder();
        final ImmutableList.Builder<ErrorEntity> saveFails = ImmutableList.builder();
        final Map<String, RestTag> tags = tagForm.getTags();

        for (Entry<String, RestTag> entry : tags.entrySet()) {
            final String tagKey = entry.getKey();
            final RestTag tag = entry.getValue();
            final String siteId = helper.getValidateSite(tag.siteId,user, request);
            try {
                final Tag createdTag = tagAPI.getTagAndCreate(tagKey, userId, siteId);
                Logger.debug(TagResource.class,()->String.format(" saved Tag %s ",createdTag.getTagName()));
                savedTags.add(createdTag);
                if (UtilMethods.isSet(userId)) {
                    tagAPI.addUserTagInode(createdTag, userId);
                    Logger.debug(TagResource.class,()->String.format(" Tag %s is now bound with user %s ",createdTag.getTagName(), userId));
                }
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(TagResource.class,
                        String.format("Exception creating tag %s", tag.label), e);
                saveFails.add(new ErrorEntity("tag.save.error.default", e.getMessage(),
                        tag.label != null ? tag.label : "unknown"));
            }
        }
        return Response.ok(new ResponseEntityView<>(saveFails.build(),
                ImmutableMap.of(TAGS, toRestTagMap(savedTags.build())))).build();
    }

    /**
     * Tag Update
     * @param request
     * @param response
     * @param tagForm
     * @return
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "updateTagV1",
            summary = "Update a tag (deprecated)",
            description = "Updates an existing tag's name and site association. Requires tagId, tagName, and siteId. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tag updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityTagMapView.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid or incomplete tag data"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "Tag not found")
            }
    )
    public Response updateTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final UpdateTagForm tagForm) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is updating tag %s ",user.getUserId(),tagForm));

        if (UtilMethods.isNotSet(tagForm.tagId) || UtilMethods.isNotSet(tagForm.siteId)
                || UtilMethods.isNotSet(tagForm.tagName)) {
            Logger.error(TagResource.class,
                    String.format("update tag `%s` data is invalid or incomplete ", tagForm.tagId));
            final String errorMessage = Try
                    .of(() -> LanguageUtil.get(user.getLocale(), "tag.save.error.default", tagForm.tagId))
                    .getOrElse(String.format("update tag `%s` data is invalid or incomplete .",
                            tagForm.tagId)); //fallback message
            throw new BadRequestException(errorMessage);
        }

        //We can assign tags to any user as long as we are admin.
        final Tag tagByTagId = Try.of(() -> tagAPI.getTagByTagId(tagForm.tagId)).getOrNull();
        if (null == tagByTagId) {
            final String errorMessage = Try.of(() -> LanguageUtil
                    .get(user.getLocale(), "tag.id.not.found", tagForm.tagId))
                    .getOrElse(String.format("Tag with id %s wasn't found.",
                            tagForm.tagId)); //fallback message
            Logger.error(TagResource.class, errorMessage);
            throw new DoesNotExistException(errorMessage);
        }
        try {
            tagAPI.updateTag(tagForm.tagId, tagForm.tagName, false, tagForm.siteId);
            return Response
                    .ok(new ResponseEntityView(toRestTagMap(tagAPI.getTagByTagId(tagForm.tagId))))
                    .build();

        } catch (DotDataException e) {
            Logger.error(TagResource.class,
                    String.format("Exception removing tag  with id `%s`", tagForm.tagId), e);
            final String errorMessage = Try
                    .of(() -> LanguageUtil.get(user.getLocale(), "tag.error.delete", tagForm.tagId))
                    .getOrElse(String.format("Error occurred removing tag %s .",
                            tagForm.tagId)); //fallback message
            throw new BadRequestException(e, errorMessage);
        }

    }

    /**
     * if an owner was provided when saving the tag this should return all the tags owned byt a given user
     * @param request
     * @param response
     * @param userId
     * @return
     */
    @GET
    @JSONP
    @Path("/user/{userId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "getTagsByUserIdV1",
            summary = "Get tags by user ID (deprecated)",
            description = "Returns all tags owned by a given user. Tags are associated with a user " +
                    "when an ownerId is provided during tag creation. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tags returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityTagMapView.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "No tags found for the specified user")
            }
    )
    public Response getTagsByUserId(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "User identifier to retrieve tags for", required = true)
            @PathParam("userId") final String userId) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting tags owned by  %s ",user.getUserId(), userId));
        final List<Tag> tags = Try.of(() -> tagAPI.getTagsForUserByUserId(userId))
                .getOrElse(ImmutableList.of());
        if (tags.isEmpty()) {
            final String errorMessage = Try.of(() -> LanguageUtil
                    .get(user.getLocale(), "tag.user.not.found", userId))
                    .getOrElse(String.format("No tags are owned by user %s.",
                            userId)); //fallback message
            Logger.error(TagResource.class, errorMessage);
            throw new DoesNotExistException(errorMessage);
        }
        return Response.ok(new ResponseEntityView(toRestTagMap(tags))).build();
    }

    /**
     * Lookup operation. Tags can be retrieved by tag name or tag id
     * @param request
     * @param response
     * @param nameOrId
     * @return
     */
    @GET
    @JSONP
    @Path("/{nameOrId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "getTagByNameOrIdV1",
            summary = "Get tag by name or ID (deprecated)",
            description = "Looks up a tag by its name or UUID. If a UUID is provided, a single tag is " +
                    "retrieved by ID. If a name is provided, all tags matching that name are returned. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tag(s) found successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityTagMapView.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "No tags found matching the name or ID")
            }
    )
    public Response getTagsByNameOrId(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @Parameter(description = "Tag name or UUID to look up", required = true)
            @PathParam("nameOrId") final String nameOrId) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting tags by name %s ",user.getUserId(), nameOrId));
        final ImmutableList.Builder<Tag> builder = ImmutableList.builder();
        if (isUUID(nameOrId)){
            final Tag tagByTagId = Try.of(()->tagAPI.getTagByTagId(nameOrId)).getOrNull();
            if(null != tagByTagId) {
                builder.add(tagByTagId);
            }
        } else {
            final List<Tag> tagsByName = Try.of(()->tagAPI.getTagsByName(nameOrId)).getOrNull();
            builder.addAll(tagsByName);
        }
        final List<Tag> foundTags = builder.build();
        if(foundTags.isEmpty()){
           return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(new ResponseEntityView(toRestTagMap(foundTags))).build();
    }


    /**
     * Delete tag for a given tag Id
     * @param request
     * @param response
     * @param tagId
     * @return
     */
    @DELETE
    @JSONP
    @Path("/{tagId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "deleteTagV1",
            summary = "Delete a tag by ID (deprecated)",
            description = "Deletes a tag by its UUID. The tag and all its associations are removed. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tag deleted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class))),
                    @ApiResponse(responseCode = "400", description = "Error deleting tag"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "Tag not found")
            }
    )
    public Response delete(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Tag UUID to delete", required = true)
            @PathParam("tagId") final String tagId) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting delete tags by id %s ",user.getUserId(), tagId));
        final Tag tagByTagId = Try.of(() -> tagAPI.getTagByTagId(tagId)).getOrNull();
        if (null == tagByTagId) {
            final String errorMessage = Try.of(() -> LanguageUtil
                    .get(user.getLocale(), "tag.id.not.found", tagId))
                    .getOrElse(String.format("Tag with id %s wasn't found.",
                            tagId)); //fallback message
            Logger.error(TagResource.class, errorMessage);
            throw new DoesNotExistException(errorMessage);
        }
        try {
            tagAPI.deleteTag(tagByTagId);
            return Response.ok(new ResponseEntityView(OK)).build();
        } catch (Exception e) {
            Logger.error(TagResource.class,
                    String.format("Exception removing tag  with id `%s`", tagId), e);
            final String errorMessage = Try
                    .of(() -> LanguageUtil.get(user.getLocale(), "tag.error.delete", tagId))
                    .getOrElse(String.format("Error occurred removing tag %s .",
                            tagId)); //fallback message
            throw new BadRequestException(e, errorMessage);
        }
    }

    /**
     * This allows for binding a tag with a given inode
     * The lookup can be done via tag name or tag id
     * if the tag name matches more than one tag all the matching tags will be bound.
     * So if you want to be more specific provide an id instead of a tag-name
     * @param request
     * @param response
     * @param nameOrId
     * @param inode
     * @return
     */
    @PUT
    @JSONP
    @Path("/tag/{nameOrId}/inode/{inode}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "linkTagToInodeV1",
            summary = "Link tag to inode (deprecated)",
            description = "Binds a tag to a given inode. The tag can be looked up by name or UUID. " +
                    "If the tag name matches more than one tag, all matching tags will be bound. " +
                    "For specificity, provide a UUID instead of a tag name. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tag(s) linked to inode successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityTagInodesMapView.class))),
                    @ApiResponse(responseCode = "400", description = "Error linking tag to inode"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "No tags found matching the name or ID")
            }
    )
    public Response linkTagsAndInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Tag name or UUID to link", required = true)
            @PathParam("nameOrId") final String nameOrId,
            @Parameter(description = "Inode to associate the tag with", required = true)
            @PathParam("inode") final String inode) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting linking tags %s with inode %s ",user.getUserId(), nameOrId, inode));
        final List<Tag> tags = new ArrayList<>();
        if (isUUID(nameOrId)){
            Logger.error(TagResource.class, String.format("Trying to look up tag `%s` by id.",nameOrId));
            final Tag tagByTagId = Try.of(()->tagAPI.getTagByTagId(nameOrId)).getOrNull();
            if(null != tagByTagId) {
               tags.add(tagByTagId);
            }
        } else {
            final List<Tag> tagsByName = Try.of(()->tagAPI.getTagsByName(nameOrId)).getOrNull();
            Logger.error(TagResource.class, String.format("There are `%d` tags found under `%s`.",tagsByName.size(), nameOrId));
            tags.addAll(tagsByName);
        }
        final List<ErrorEntity> saveFails = new ArrayList<>();
        if (tags.isEmpty()) {
            Logger.error(TagResource.class, String.format("No tags like `%s` were found .",nameOrId));
            return Response.status(Status.NOT_FOUND).build();
        }
        final List<TagInode> tagInodes = new ArrayList<>();
        for (final Tag tag:tags) {
            try {
                 tagInodes.add(tagAPI.addUserTagInode(tag, inode));
            } catch (DotDataException e) {
                Logger.error(TagResource.class,
                        String.format("Exception linking tag `%s` with inode `%s`", tag.getTagName(), inode), e);
                saveFails.add(new ErrorEntity("tag.save.error.default", e.getMessage(), tag.getTagName()));
            }
        }
        return Response.ok(new ResponseEntityView<List<TagInode>>(saveFails, tagInodes)).build();
    }

    /**
     * Given an inode this will retrieve all the tags associated to it.
     * @param request
     * @param response
     * @param inode
     * @return
     */
    @GET
    @JSONP
    @Path("/inode/{inode}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "findTagsByInodeV1",
            summary = "Find tags by inode (deprecated)",
            description = "Retrieves all tag-inode associations for a given inode. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tag-inode associations returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityTagInodesMapView.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "No tags found for the specified inode")
            }
    )
    public Response findTagsByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Inode to retrieve associated tags for", required = true)
            @PathParam("inode") final String inode) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting finding tags by inode %s ",user.getUserId(), inode));
        final List<TagInode> tagInodes = Try.of(() -> tagAPI.getTagInodesByInode(inode))
                .getOrElse(ImmutableList.of());
        if (tagInodes.isEmpty()) {
            Logger.error(TagResource.class, String.format("No tags were found by the inode %s.",inode));
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(new ResponseEntityView<List<TagInode>>(tagInodes)).build();
    }

    /**
     * Breaks the link between an inode and all the associated tags
     * @param request
     * @param response
     * @param inode
     * @return
     */
    @DELETE
    @JSONP
    @Path("/inode/{inode}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "deleteTagInodesByInodeV1",
            summary = "Remove all tag associations from inode (deprecated)",
            description = "Breaks the link between an inode and all its associated tags. " +
                    "The tags themselves are not deleted, only the associations. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tag-inode associations removed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class))),
                    @ApiResponse(responseCode = "400", description = "Error removing tag-inode associations"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "No tags found for the specified inode")
            }
    )
    public Response deleteTagInodesByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Inode to remove all tag associations from", required = true)
            @PathParam("inode") final String inode) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting delete tagsInode by inode %s ",user.getUserId(), inode));
        final List<TagInode> tagInodes = Try.of(() -> tagAPI.getTagInodesByInode(inode))
                .getOrElse(ImmutableList.of());
        if (tagInodes.isEmpty()) {
            Logger.error(TagResource.class, String.format("No tags were found by the inode %s.",inode));
            return Response.status(Status.NOT_FOUND).build();
        }
        try {
            tagAPI.deleteTagInodesByInode(inode);
            Logger.error(TagResource.class, String.format("Tags with inode %s successfully removed.",inode));
        } catch (DotDataException e) {
            final String errorMessage = Try.of(() -> LanguageUtil
                    .get(user.getLocale(), "tag.error.tag.inode.delete", inode))
                    .getOrElse(String.format("Error occurred removing tag %s .",
                            inode)); //fallback message
            Logger.error(TagResource.class, errorMessage);
            throw new BadRequestException(e, errorMessage);
        }
        return Response.ok(new ResponseEntityView(OK)).build();

    }

    @POST
    @Path("/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            operationId = "importTagsV1",
            summary = "Import tags from file (deprecated)",
            description = "Imports tags from a multipart file upload. The file should contain tag data " +
                    "in CSV format. " +
                    "This endpoint is deprecated. Use the v2 Tag API (/api/v2/tags) instead.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tags imported successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid file or import error"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
            }
    )
    public final Response importTags(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Multipart form data with a CSV file containing tags to import",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "object",
                                    description = "CSV file with tag data to import"))
            )
            final FormDataMultiPart form
    ) {
        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting import of tags",user.getUserId()));
        try {
            helper.importTags(form, user, request);
        } catch (Exception e) {
            Logger.error(TagResource.class,"Failure Importing tags file", e);
            throw new BadRequestException(e, "Failure Importing tags file");
        }
        return Response.ok(new ResponseEntityView(OK)).build();

    }

}
