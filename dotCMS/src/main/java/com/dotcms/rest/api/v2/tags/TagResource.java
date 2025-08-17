package com.dotcms.rest.api.v2.tags;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.*;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.tag.*;
import com.dotcms.rest.tag.TagsResourceHelper;
import com.dotcms.rest.api.v2.tags.ResponseEntityRestTagView;
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
import com.google.common.collect.ImmutableList;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dotcms.rest.tag.TagsResourceHelper.toRestTagMap;
import static com.dotcms.rest.validation.Preconditions.checkNotNull;
import static com.dotcms.util.DotPreconditions.checkArgument;
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
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagMapView list(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                         @QueryParam("name") final String tagName,
                                         @QueryParam("siteId") final String siteId) {

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
            summary = "Create multiple tags",
            description = "Creates multiple tags in bulk with optional owner assignment"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Tags created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityTagMapView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad Request - Invalid tag data",
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
    @Path("/_bulk")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagMapView addTag(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Bulk tag data to create",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TagForm.class)))
            final TagForm tagForm) throws DotDataException, DotSecurityException {

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
     * Creates a single tag.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param tagForm  The {@link SingleTagForm} containing the tag to create.
     *
     * @return The {@link ResponseEntityRestTagView} containing the created or existing tag.
     */
    @Operation(
            summary = "Create single tag",
            description = "Creates a single tag or returns existing tag if it already exists"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Tag created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityRestTagView.class))),
            @ApiResponse(responseCode = "200",
                    description = "Existing tag returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityRestTagView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad Request - Invalid tag data",
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
    public Response createSingleTag(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Single tag data to create",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SingleTagForm.class)))
            final SingleTagForm tagForm) {

        try {
            // Initialize and check permissions
            final InitDataObject initDataObject = getInitDataObject(request, response);
            final User user = initDataObject.getUser();

            Logger.debug(TagResource.class,
                    () -> String.format("User '%s' is creating single tag: %s",
                            user.getUserId(), tagForm.getName()));

            // Validate tag name
            Logger.debug(TagResource.class, "Validating tag form: " + tagForm.getName());
            final List<ErrorEntity> validationErrors = validateSingleTag(tagForm);
            Logger.debug(TagResource.class, "Validation errors found: " + validationErrors.size());
            if (!validationErrors.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(validationErrors))
                        .build();
            }

            // Determine if this is a new tag or existing
            final String siteId = helper.getValidateSite(tagForm.getSiteId(), user, request);
            Tag existingTag = null;
            try {
                existingTag = tagAPI.getTagByNameAndHost(tagForm.getName(), siteId);
            } catch (Exception e) {
                // Tag doesn't exist, will be created
            }

            // Create or get the tag
            final Tag tag = tagAPI.getTagAndCreate(
                    tagForm.getName(),
                    tagForm.getOwnerId(),
                    siteId
            );

            // Bind to owner if specified
            if (UtilMethods.isSet(tagForm.getOwnerId())) {
                tagAPI.addUserTagInode(tag, tagForm.getOwnerId());
                Logger.debug(TagResource.class,
                        String.format("Tag '%s' is now bound to user '%s'",
                                tag.getTagName(), tagForm.getOwnerId()));
            }

            // Convert to RestTag
            final RestTag restTag = TagsResourceHelper.toRestTag(tag);

            // Return 201 for new tag, 200 for existing
            final Response.Status status = (existingTag == null)
                    ? Response.Status.CREATED
                    : Response.Status.OK;

            return Response.status(status)
                    .entity(new ResponseEntityRestTagView(restTag))
                    .build();

        } catch (DotSecurityException e) {
            Logger.warn(TagResource.class,
                    "User lacks permission to create tags: " + e.getMessage());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ResponseEntityView<>(List.of(
                            new ErrorEntity("dotcms.api.error.forbidden",
                                    "User does not have access to Tags portlet")
                    )))
                    .build();

        } catch (DotDataException e) {
            Logger.error(TagResource.class,
                    "Database error creating tag: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>(List.of(
                            new ErrorEntity("dotcms.api.error.db",
                                    "There was an error saving the tag")
                    )))
                    .build();

        } catch (Exception e) {
            Logger.error(TagResource.class,
                    "Unexpected error creating tag: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>(List.of(
                            new ErrorEntity("dotcms.api.error.internal",
                                    "An unexpected error occurred")
                    )))
                    .build();
        }
    }

    /**
     * Validates a single tag form.
     */
    private List<ErrorEntity> validateSingleTag(final SingleTagForm form) {
        final List<ErrorEntity> errors = new ArrayList<>();

        if (!UtilMethods.isSet(form.getName())) {
            errors.add(new ErrorEntity("tag.validation.error",
                    "Tag name is required", "name"));
        } else {
            // Check for commas
            if (form.getName().contains(",")) {
                errors.add(new ErrorEntity("tag.validation.error",
                        "Tag name cannot contain commas", "name"));
            }
            // Check length
            if (form.getName().length() > 255) {
                errors.add(new ErrorEntity("tag.validation.error",
                        "Tag name cannot exceed 255 characters", "name"));
            }
            // Check for blank/whitespace only
            if (form.getName().trim().isEmpty()) {
                errors.add(new ErrorEntity("tag.validation.error",
                        "Tag name cannot be blank", "name"));
            }
        }

        return errors;
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
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagMapView updateTag(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final UpdateTagForm tagForm) throws DotDataException {

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
    public Response getTagsByNameOrId(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @Parameter(description = "Tag name or UUID", required = true)
                                      @PathParam("nameOrId") final String nameOrId,
                                      @Parameter(description = "Site ID for name-based searches. If not provided, uses current site context with SYSTEM_HOST fallback")
                                      @QueryParam("siteId") final String siteId) {

        try {
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
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ResponseEntityView<>(List.of(
                        new ErrorEntity("dotcms.api.error.not_found", 
                            String.format("Tag not found: %s", nameOrId), null)
                    )))
                    .build();
            }
            
            final RestTag restTag = TagsResourceHelper.toRestTag(tag);
            return Response.ok(new ResponseEntityRestTagView(restTag)).build();
            
        } catch (Exception e) {
            Logger.error(TagResource.class, "Unexpected error retrieving tag: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ResponseEntityView<>(List.of(
                    new ErrorEntity("dotcms.api.error.internal", 
                        "An unexpected error occurred", null)
                )))
                .build();
        }
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
    @DELETE
    @JSONP
    @Path("/{tagId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityBooleanView delete(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @PathParam("tagId") final String tagId) throws DotDataException {

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
    @POST
    @Path("/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final ResponseEntityBooleanView importTags(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final FormDataMultiPart form
    ) throws DotDataException, IOException, DotSecurityException {
        final InitDataObject initDataObject = getInitDataObject(request, response);

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class, String.format("User '%s' is importing Tags form CSV file.",user.getUserId()));
        helper.importTags(form, user, request);

        return new ResponseEntityBooleanView(true);
    }

}
