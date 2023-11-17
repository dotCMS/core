package com.dotcms.rest.api.v2.tags;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
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
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.dotcms.rest.ResponseEntityView.OK;
import static com.dotcms.rest.tag.TagsResourceHelper.toRestTagMap;
import static com.dotmarketing.util.UUIDUtil.isUUID;

/**
 * Tag Related logic is exposed to the web here
 */
@Path("/v2/tags")
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
    protected TagResource(final TagAPI tagAPI, final HostAPI hostAPI, final FolderAPI folderAPI, final WebResource webResource) {
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
     * @return ResponseEntityTagMapView
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagMapView list(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @QueryParam("name") final String  tagName,
            @QueryParam("siteId") final String siteId) {

      final InitDataObject initDataObject =
              new WebResource.InitBuilder(webResource)
                      .requiredAnonAccess(AnonymousAccess.READ)
                      .requestAndResponse(request, response).init();

        final User user = initDataObject.getUser();

        Logger.debug(this, ()-> "List Tags, tagName: " + tagName + ", siteId:" + siteId);

        final List<Tag> tags = UtilMethods.isSet(tagName)
                ? helper.searchTagsInternal(tagName, helper.getSiteId(siteId, request, user))
                : helper.getTagsInternal();

        final Map<String, RestTag> tagsMap = toRestTagMap(tags);
        return new ResponseEntityTagMapView(tagsMap);
    }

    /**
     * Creates new tags and link them to an owner-user if provided
     * @param request
     * @param response
     * @param tagForm
     * @return ResponseEntityTagMapView
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagMapView addTag(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final TagForm tagForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = getInitDataObject(request, response);

        final User user = initDataObject.getUser();
        Logger.debug(TagResource.class,()->String.format(" user %s is adding tag %s ", user.getUserId(), tagForm));

        //We can assign tags to any user as long as we are admin.
        final String userId = tagForm.getOwnerId();
        final ImmutableList.Builder<Tag> savedTags = ImmutableList.builder();
        final Map<String, RestTag> tags = tagForm.getTags();

        for (final Entry<String, RestTag> entry : tags.entrySet()) {
            final String tagKey = entry.getKey();
            final RestTag tag   = entry.getValue();
            final String siteId = helper.getValidateSite(tag.siteId, user, request);
            final Tag createdTag = tagAPI.getTagAndCreate(tagKey, userId, siteId);
            Logger.debug(TagResource.class,()->String.format(" saved Tag %s ",createdTag.getTagName()));
            savedTags.add(createdTag);
            if (UtilMethods.isSet(userId)) {
                tagAPI.addUserTagInode(createdTag, userId);
                Logger.debug(TagResource.class,()->String.format(" Tag %s is now bound with user %s ",createdTag.getTagName(), userId));
            }
        }

        final Map<String, RestTag> tagsMap = toRestTagMap(savedTags.build());
        return new ResponseEntityTagMapView(tagsMap);
    }

    /**
     * Tag Update
     * @param request
     * @param response
     * @param tagForm
     * @return ResponseEntityTagMapView
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
        Logger.debug(TagResource.class,()->String.format(" user %s is updating tag %s ", user.getUserId(), tagForm));

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

        tagAPI.updateTag(tagForm.tagId, tagForm.tagName, false, tagForm.siteId);

        final Map<String, RestTag> tagsMap = toRestTagMap(tagAPI.getTagByTagId(tagForm.tagId));
        return new ResponseEntityTagMapView(tagsMap);
    }

    /**
     * if an owner was provided when saving the tag this should return all the tags owned byt a given user
     * @param request
     * @param response
     * @param userId
     * @return ResponseEntityTagMapView
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

        final Map<String, RestTag> tagsMap = toRestTagMap(tags);
        return new ResponseEntityTagMapView(tagsMap);
    }

    /**
     * Lookup operation. Tags can be retrieved by tag name or tag id
     * @param request
     * @param response
     * @param nameOrId
     * @return ResponseEntityTagMapView
     */
    @GET
    @JSONP
    @Path("/{nameOrId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagMapView getTagsByNameOrId(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @PathParam("nameOrId") final String nameOrId) {

        final InitDataObject initDataObject = getInitDataObject(request, response);
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
           throw new NotFoundException(String.format("No tags were found by the name or id %s.", nameOrId));
        }
        final Map<String, RestTag> tagsMap = toRestTagMap(foundTags);
        return new ResponseEntityTagMapView(tagsMap);
    }


    /**
     * Delete tag for a given tag Id
     * @param request
     * @param response
     * @param tagId
     * @return ResponseEntityBooleanView
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

        tagAPI.deleteTag(tagByTagId);
        return new ResponseEntityBooleanView(true);
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
     * @return ResponseEntityTagInodesMapView
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

        if (tags.isEmpty()) {
            Logger.error(TagResource.class, String.format("No tags like `%s` were found .",nameOrId));
            throw new NotFoundException(String.format("No tags were found by the name or id %s.", nameOrId));
        }
        final List<TagInode> tagInodes = new ArrayList<>();
        for (final Tag tag:tags) {
            tagInodes.add(tagAPI.addUserTagInode(tag, inode));
        }
        return new ResponseEntityTagInodesMapView(tagInodes);
    }

    /**
     * Given an inode this will retrieve all the tags associated to it.
     * @param request
     * @param response
     * @param inode
     * @return ResponseEntityTagInodesMapView
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
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting finding tags by inode %s ",user.getUserId(), inode));
        final List<TagInode> tagInodes = Try.of(() -> tagAPI.getTagInodesByInode(inode))
                .getOrElse(ImmutableList.of());
        if (tagInodes.isEmpty()) {
            Logger.error(TagResource.class, String.format("No tags were found by the inode %s.",inode));
            throw new NotFoundException(String.format("No tags were found by the inode %s.", inode));
        }

        return new ResponseEntityTagInodesMapView(tagInodes);
    }

    /**
     * Breaks the link between an inode and all the associated tags
     * @param request
     * @param response
     * @param inode
     * @return ResponseEntityBooleanView
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
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting delete tagsInode by inode %s ",user.getUserId(), inode));
        final List<TagInode> tagInodes = Try.of(() -> tagAPI.getTagInodesByInode(inode))
                .getOrElse(ImmutableList.of());
        if (tagInodes.isEmpty()) {
            Logger.error(TagResource.class, String.format("No tags were found by the inode %s.",inode));
            throw new NotFoundException(String.format("No tags were found by the inode %s.", inode));
        }

        tagAPI.deleteTagInodesByInode(inode);
        Logger.error(TagResource.class, String.format("Tags with inode %s successfully removed.",inode));

        return new ResponseEntityBooleanView(true);
    }

    private InitDataObject getInitDataObject(final HttpServletRequest request,
                                             final HttpServletResponse response) {
        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet(TAGS)
                        .init();
        return initDataObject;
    }

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
        Logger.debug(TagResource.class,()->String.format(" user %s is requesting import of tags",user.getUserId()));
        helper.importTags(form, user, request);

        return new ResponseEntityBooleanView(true);
    }

}
