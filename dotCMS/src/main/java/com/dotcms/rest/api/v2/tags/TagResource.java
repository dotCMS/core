package com.dotcms.rest.api.v2.tags;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
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
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.ImmutableList;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dotcms.rest.tag.TagsResourceHelper.toRestTagMap;
import static com.dotmarketing.util.UUIDUtil.isUUID;

/**
 * This REST Endpoint provide CRUD operations for Tags in dotCMS.
 *
 * @author jsanca
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Path("/v2/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags")
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
     * Lists all Tags based on the provided criteria. If a Tag name is provided, a search-by-name
     * (like) operation will be performed instead. The search-by-name operation can be delimited by
     * a Site ID as well. If no matches are found against the Site ID, the search-by-name operation
     * will be performed against the global tags.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param tagName  The name of the Tag to search for.
     * @param siteId   The ID of the Site where the specified Tag lives, in case it was provided.
     *
     * @return The {@link ResponseEntityTagMapView} containing the list of Tags that match the
     * provided criteria.
     */
    @Operation(
        summary = "List tags",
        description = "Lists all tags based on provided criteria. If a tag name is provided, performs a search-by-name (like) operation that can be delimited by site ID. If no matches are found against the site ID, searches global tags."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityTagMapView list(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                         @Parameter(description = "Name of the tag to search for (optional)") @QueryParam("name") final String tagName,
                                         @Parameter(description = "ID of the site where the tag lives (optional)") @QueryParam("siteId") final String siteId) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredAnonAccess(AnonymousAccess.READ)
                        .requestAndResponse(request, response).init();

        final User user = initDataObject.getUser();

        Logger.debug(this, UtilMethods.isSet(tagName)
                ? String.format("Listing Tag(s) '%s' from Site '%s'", tagName, siteId)
                : "Listing ALL Tags");

        final List<Tag> tags = UtilMethods.isSet(tagName)
                ? helper.searchTagsInternal(tagName, helper.getSiteId(siteId, request, user))
                : helper.getTagsInternal();

        return new ResponseEntityTagMapView(toRestTagMap(tags));
    }

    /**
     * Creates one or more Tags and link them to an owner/user, if provided.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param tagForm  The {@link TagForm} containing the Tags to save.
     *
     * @return The {@link ResponseEntityTagMapView} containing the saved Tags.
     */
    @Operation(
        summary = "Create tags",
        description = "Creates one or more tags and optionally links them to an owner/user. Tags can be created with site-specific scoping."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid tag data or parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to create tags",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityTagMapView addTag(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(
                description = "Tag form containing tags to create with optional owner assignment", 
                required = true,
                content = @Content(schema = @Schema(implementation = TagForm.class))
            ) final TagForm tagForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = getInitDataObject(request, response);

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format("User '%s' is adding tag(s): %s ", user.getUserId(), tagForm));

        //We can assign tags to any user as long as we are admin.
        final String userId = tagForm.getOwnerId();
        final ImmutableList.Builder<Tag> savedTags = ImmutableList.builder();
        final Map<String, RestTag> tags = tagForm.getTags();

        saveTags(request, tags, user, userId, savedTags);

        return new ResponseEntityTagMapView(toRestTagMap(savedTags.build()));
    }

    /**
     * Saves a Tag in dotCMS.
     *
     * @param request   The current instance of the {@link HttpServletRequest}.
     * @param tags      The {@link Map} containing the Tags to save.
     * @param user      The {@link User} performing the operation.
     * @param userId    The ID of the User to link the Tag to.
     * @param savedTags The {@link ImmutableList.Builder} containing the Tags that were saved.
     *
     * @throws DotDataException     An error occurred when persisting Tag data.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *                              perform this operation.
     */
    @WrapInTransaction
    private void saveTags(final HttpServletRequest request,
                          final Map<String, RestTag> tags,
                          final User user, final String userId,
                          final ImmutableList.Builder<Tag> savedTags)
            throws DotDataException, DotSecurityException {

        for (final Entry<String, RestTag> entry : tags.entrySet()) {
            final String tagKey = entry.getKey();
            final RestTag tag   = entry.getValue();
            final String siteId = helper.getValidateSite(tag.siteId, user, request);
            final Tag createdTag = tagAPI.getTagAndCreate(tagKey, userId, siteId);
            Logger.debug(TagResource.class, String.format("Saving Tag '%s'", createdTag.getTagName()));
            savedTags.add(createdTag);
            if (UtilMethods.isSet(userId)) {
                tagAPI.addUserTagInode(createdTag, userId);
                Logger.debug(TagResource.class, String.format("Tag '%s' is now bound to user '%s'",createdTag.getTagName(), userId));
            }
        }
    }

    /**
     * Updates the information belonging to a specific Tag.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param tagForm  The {@link UpdateTagForm} containing the Tag information to update.
     *
     * @return The {@link ResponseEntityTagMapView} containing the updated Tag information.
     */
    @Operation(
        summary = "Update tag",
        description = "Updates the information belonging to a specific tag. Requires tag ID, site ID, and new tag name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tag updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid or incomplete tag data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to update tags",
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
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityTagMapView updateTag(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(
                description = "Update tag form containing tag ID, site ID, and new tag name", 
                required = true,
                content = @Content(schema = @Schema(implementation = UpdateTagForm.class))
            ) final UpdateTagForm tagForm) throws DotDataException {

        final InitDataObject initDataObject = getInitDataObject(request, response);

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format("User '%s' is updating Tag %s", user.getUserId(), tagForm));

        if (UtilMethods.isNotSet(tagForm.tagId) || UtilMethods.isNotSet(tagForm.siteId)
                || UtilMethods.isNotSet(tagForm.tagName)) {

            Logger.error(TagResource.class,
                    String.format("Data for Tag `%s` is invalid or incomplete", tagForm.tagId));
            final String errorMessage = Try
                    .of(() -> LanguageUtil.get(user.getLocale(), "tag.save.error.default", tagForm.tagId))
                    .getOrElse(String.format("Data for Tag `%s` is invalid or incomplete",
                            tagForm.tagId)); //fallback message
            throw new BadRequestException(errorMessage);
        }

        //We can assign tags to any user as long as we are admin.
        final Tag tag = Try.of(() -> tagAPI.getTagByTagId(tagForm.tagId)).getOrNull();
        if (null == tag) {

            final String errorMessage = Try.of(() -> LanguageUtil
                    .get(user.getLocale(), "tag.id.not.found", tagForm.tagId))
                    .getOrElse(String.format("Tag with id %s wasn't found.",
                            tagForm.tagId)); //fallback message
            Logger.error(TagResource.class, errorMessage);
            throw new DoesNotExistException(errorMessage);
        }

        tagAPI.updateTag(tagForm.tagId, tagForm.tagName, false, tagForm.siteId);

        return new ResponseEntityTagMapView(toRestTagMap(tagAPI.getTagByTagId(tagForm.tagId)));
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
    @Operation(
        summary = "Get tags by user ID",
        description = "Retrieves all tags owned by a specific user. Returns tags that were explicitly linked to the user during creation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "User tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags",
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
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityTagMapView getTagsByUserId(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "User ID to retrieve tags for", required = true) @PathParam("userId") final String userId) {

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
     * Retrieves Tags by name or ID. If the provided value is a valid UUID, a search-by-ID operation
     * will be performed and will yield one single result.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param nameOrId The name or ID of the Tag to search for.
     *
     * @return The {@link ResponseEntityTagMapView} containing the list of Tags that match the
     * search criteria.
     */
    @Operation(
        summary = "Get tags by name or ID",
        description = "Retrieves tags by name or ID. If the provided value is a valid UUID, performs search-by-ID and returns a single result. Otherwise, searches by name and returns all matching tags."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags",
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
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityTagMapView getTagsByNameOrId(@Context final HttpServletRequest request,
                                                      @Context final HttpServletResponse response,
                                                      @Parameter(description = "Name or UUID of the tag to search for", required = true) @PathParam("nameOrId") final String nameOrId) {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format("User '%s' is requesting tags by name or ID '%s'", user.getUserId(), nameOrId));
        final ImmutableList.Builder<Tag> builder = ImmutableList.builder();
        if (isUUID(nameOrId)) {
            final Tag tag = Try.of(()->tagAPI.getTagByTagId(nameOrId)).getOrNull();
            if(null != tag) {
                builder.add(tag);
            }
        } else {
            final List<Tag> tagsByName = Try.of(()->tagAPI.getTagsByName(nameOrId)).getOrNull();
            builder.addAll(tagsByName);
        }
        final List<Tag> foundTags = builder.build();
        if(foundTags.isEmpty()) {

           throw new NotFoundException(String.format("No tags were found by the name or ID '%s'", nameOrId));
        }

        return new ResponseEntityTagMapView(toRestTagMap(foundTags));
    }


    /**
     * Deletes a Tag based on its ID.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param tagId    The ID of the Tag to delete.
     *
     * @return A {@link ResponseEntityBooleanView} containing the result of the delete operation.
     */
    @Operation(
        summary = "Delete tag",
        description = "Deletes a tag based on its ID. The tag must exist and the user must have appropriate permissions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tag deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to delete tags",
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
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityBooleanView delete(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @Parameter(description = "ID of the tag to delete", required = true) @PathParam("tagId") final String tagId) throws DotDataException {

        final InitDataObject initDataObject = getInitDataObject(request, response);
        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format("User '%s' is deleting tags by ID '%s'",user.getUserId(), tagId));
        final Tag tag = Try.of(() -> tagAPI.getTagByTagId(tagId)).getOrNull();
        if (null == tag) {

            final String errorMessage = Try.of(() -> LanguageUtil
                    .get(user.getLocale(), "tag.id.not.found", tagId))
                    .getOrElse(String.format("Tag with id %s wasn't found.",
                            tagId)); //fallback message
            Logger.error(TagResource.class, errorMessage);
            throw new DoesNotExistException(errorMessage);
        }

        tagAPI.deleteTag(tag);
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
    @Operation(
        summary = "Link tags to inode",
        description = "Binds tags with a given inode. Lookup can be done via tag name or ID. If tag name matches multiple tags, all matching tags will be bound. Use tag ID for specific binding."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags linked to inode successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagInodesMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid tag name/ID or inode",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to link tags",
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
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityTagInodesMapView linkTagsAndInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Name or UUID of the tag to link", required = true) @PathParam("nameOrId") final String nameOrId,
            @Parameter(description = "Inode to link the tag(s) to", required = true) @PathParam("inode") final String inode) throws DotDataException {

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
    @Operation(
        summary = "Get tags by inode",
        description = "Retrieves all tags associated with a given inode. Returns tag-inode relationships for the specified content."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityTagInodesMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access tags",
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
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityTagInodesMapView findTagsByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Inode to retrieve tags for", required = true) @PathParam("inode") final String inode) {

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
    @Operation(
        summary = "Delete tag-inode associations",
        description = "Breaks the link between an inode and all its associated tags. Removes all tag associations for the specified content but does not delete the tags themselves."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tag-inode associations deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to modify tag associations",
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
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityBooleanView deleteTagInodesByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Inode to remove tag associations from", required = true) @PathParam("inode") final String inode) throws DotDataException {

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
     * Imports Tags from a CSV file.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param form     The {@link FormDataMultiPart} containing the CSV file.
     *
     * @return A {@link ResponseEntityBooleanView} containing the result of the import operation.
     *
     * @throws DotDataException     An error occurred when persisting Tag data.
     * @throws IOException          An error occurred when reading the CSV file.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *                              perform this operation.
     */
    @Operation(
        summary = "Import tags from CSV",
        description = "Imports tags from a CSV file. The CSV file should contain tag data in the expected format for bulk tag creation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Tags imported successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid CSV file format or data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to import tags",
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
    public final ResponseEntityBooleanView importTags(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(
                description = "CSV file containing tag data for import", 
                required = true,
                content = @Content(mediaType = "multipart/form-data")
            ) final FormDataMultiPart form
    ) throws DotDataException, IOException, DotSecurityException {
        final InitDataObject initDataObject = getInitDataObject(request, response);

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class, String.format("User '%s' is importing Tags form CSV file.",user.getUserId()));
        helper.importTags(form, user, request);

        return new ResponseEntityBooleanView(true);
    }

}
