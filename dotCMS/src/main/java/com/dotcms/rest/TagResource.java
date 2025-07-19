package com.dotcms.rest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.tag.RestTag;
import com.dotcms.rest.tag.TagForm;
import com.dotcms.rest.tag.TagsResourceHelper;
import com.dotcms.rest.tag.UpdateTagForm;
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
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import com.dotcms.rest.annotation.SwaggerCompliant;

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
@SwaggerCompliant(value = "Legacy and utility APIs", batch = 8)
@Path("/v1/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags")
@Deprecated(since = "23.12")
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
    @Operation(
        summary = "List tags (deprecated)",
        description = "Lists all tags or searches by name. If name is provided, performs search-by-name operation that can be delimited by site ID. If no matches found against site ID, searches global tags. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "Map of tag identifiers to RestTag objects containing tag metadata and properties"))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, RestTag> list(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @Parameter(description = "Name of the tag to search for (optional)") @QueryParam("name") final String  tagName,
            @Parameter(description = "ID of the site where the tag lives (optional)") @QueryParam("siteId") final String siteId) {


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
    @Operation(
        summary = "Create tags (deprecated)",
        description = "Creates new tags and optionally links them to an owner/user. Handles multiple tags and provides error reporting for failed saves. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags created successfully (may include error details)",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagOperationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(
                description = "Tag form containing tags to create with optional owner assignment", 
                required = true,
                content = @Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TagForm.class))
            ) final TagForm tagForm) {

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
    @Operation(
        summary = "Update tag (deprecated)",
        description = "Updates an existing tag's information. Requires tag ID, site ID, and new tag name. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tag updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagsView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid or incomplete tag data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Tag not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(
                description = "Update tag form containing tag ID, site ID, and new tag name", 
                required = true,
                content = @Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UpdateTagForm.class))
            ) final UpdateTagForm tagForm) {

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
                    .ok(new ResponseEntityView<>(toRestTagMap(tagAPI.getTagByTagId(tagForm.tagId))))
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
    @Operation(
        summary = "Get tags by user ID (deprecated)",
        description = "Retrieves all tags owned by a specific user. Returns tags that were explicitly linked to the user during creation. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "User tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagsView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "No tags found for the specified user",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/user/{userId}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagsByUserId(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "User ID to retrieve tags for", required = true) @PathParam("userId") final String userId) {

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
        return Response.ok(new ResponseEntityView<>(toRestTagMap(tags))).build();
    }

    /**
     * Lookup operation. Tags can be retrieved by tag name or tag id
     * @param request
     * @param response
     * @param nameOrId
     * @return
     */
    @Operation(
        summary = "Get tags by name or ID (deprecated)",
        description = "Retrieves tags by name or ID. If the provided value is a valid UUID, performs search-by-ID and returns a single result. Otherwise, searches by name and returns all matching tags. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagsView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "No tags found by the specified name or ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/{nameOrId}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagsByNameOrId(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @Parameter(description = "Name or UUID of the tag to search for", required = true) @PathParam("nameOrId") final String nameOrId) {

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
        return Response.ok(new ResponseEntityView<>(toRestTagMap(foundTags))).build();
    }


    /**
     * Delete tag for a given tag Id
     * @param request
     * @param response
     * @param tagId
     * @return
     */
    @Operation(
        summary = "Delete tag (deprecated)",
        description = "Deletes a tag based on its ID. The tag must exist and the user must have appropriate permissions. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tag deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - error occurred during deletion",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Tag not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @JSONP
    @Path("/{tagId}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "ID of the tag to delete", required = true) @PathParam("tagId") final String tagId) {

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
            return Response.ok(new ResponseEntityView<>(OK)).build();
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
    @Operation(
        summary = "Link tags to inode (deprecated)",
        description = "Binds tags with a given inode. Lookup can be done via tag name or ID. If tag name matches multiple tags, all matching tags will be bound. Use tag ID for specific binding. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags linked to inode successfully (may include error details)",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagInodeOperationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Tag not found by the specified name or ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @JSONP
    @Path("/tag/{nameOrId}/inode/{inode}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response linkTagsAndInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Name or UUID of the tag to link", required = true) @PathParam("nameOrId") final String nameOrId,
            @Parameter(description = "Inode to link the tag(s) to", required = true) @PathParam("inode") final String inode) {

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
    @Operation(
        summary = "Get tags by inode (deprecated)",
        description = "Retrieves all tags associated with a given inode. Returns tag-inode relationships for the specified content. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagInodesView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "No tags found for the specified inode",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/inode/{inode}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response findTagsByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Inode to retrieve tags for", required = true) @PathParam("inode") final String inode) {

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
    @Operation(
        summary = "Delete tag-inode associations (deprecated)",
        description = "Breaks the link between an inode and all its associated tags. Removes all tag associations for the specified content but does not delete the tags themselves. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tag-inode associations deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - error occurred during deletion",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "No tag associations found for the specified inode",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @JSONP
    @Path("/inode/{inode}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTagInodesByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Inode to remove tag associations from", required = true) @PathParam("inode") final String inode) {

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
        return Response.ok(new ResponseEntityView<>(OK)).build();

    }

    @Operation(
        summary = "Import tags from CSV (deprecated)",
        description = "Imports tags from a CSV file. The CSV file should contain tag data in the expected format for bulk tag creation. This endpoint is deprecated - use v2 instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags imported successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - failure importing tags file",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/import")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response importTags(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(
                description = "CSV file containing tag data for import", 
                required = true,
                content = @Content(mediaType = "multipart/form-data")
            ) final FormDataMultiPart form
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
        return Response.ok(new ResponseEntityView<>(OK)).build();

    }

}
