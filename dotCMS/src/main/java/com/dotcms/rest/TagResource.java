package com.dotcms.rest;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.glassfish.jersey.server.JSONP;

@Path("/v1/tags")
public class TagResource {

    private static final String TAGS = "tags";
    private final WebResource webResource;
    private final TagAPI tagAPI;
	private final HostAPI hostAPI;
	private final LayoutAPI layoutAPI;
	private final FolderAPI folderAPI;

    @SuppressWarnings("unused")
    public TagResource() {
        this(APILocator.getTagAPI(), APILocator.getHostAPI(), APILocator.getLayoutAPI(), APILocator.getFolderAPI(), new WebResource());
    }

    @VisibleForTesting
    protected TagResource(final TagAPI tagAPI,final HostAPI hostAPI, final LayoutAPI layoutAPI, final FolderAPI folderAPI, final WebResource webResource) {
        this.tagAPI = tagAPI;
        this.webResource = webResource;
        this.hostAPI = hostAPI;
        this.layoutAPI = layoutAPI;
        this.folderAPI = folderAPI;
    }

    /**
     * This performs a list operation. But if a name is provided a search-by-name (like) operation will be performed instead.
     * The search-by-name operation can be delimited by a siteId
     * if No site id is provided the search-by-name operation will be performed against the sites listed under SYSTEM_HOST
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
    public Map<String, RestTag> list(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @QueryParam("name") final String  tagName,
            @QueryParam("siteId") final String siteId) {


      final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
      .requiredAnonAccess(AnonymousAccess.READ)
      .requestAndResponse(request, response)
      .init();

        final User user = initDataObject.getUser();

        List<Tag> tags = UtilMethods.isSet(tagName)
                ? searchTagsInternal(tagName, this.getSiteId(siteId, request), user)
                : getTagsInternal();

        final Map<String, RestTag> hash = Maps.newHashMapWithExpectedSize(tags.size());
        final TagTransform transform = new TagTransform();
        for (final Tag tag : tags) {
            hash.put(tag.getTagName(), transform.appToRest(tag));
        }
        return hash;
    }

    private String getSiteId (final String siteId, final HttpServletRequest request) {

        if (!UtilMethods.isSet(siteId)) {
            final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            if (null != currentHost) {
                return currentHost.getIdentifier();
            }
        }

        return siteId;
    }

    private List<Tag> searchTagsInternal(final String tagName, final String siteOrFolderId,
            final User user) {
        List<Tag> tags;

        try {
            final boolean frontEndRequest = user.isFrontendUser();
            final Host host = hostAPI.find(siteOrFolderId, user, frontEndRequest);
            String internalSiteOrFolderId = siteOrFolderId;


            if ((!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getInode()))
                    && UtilMethods.isSet(siteOrFolderId)) {
                internalSiteOrFolderId = folderAPI
                        .find(siteOrFolderId, user, frontEndRequest).getHostId();
            }

            tags = tagAPI.getSuggestedTag(tagName, internalSiteOrFolderId);
        } catch (DotDataException | DotSecurityException e) {
            throw new BadRequestException(e, e.getMessage());
        }
        return tags;
    }

    private List<Tag> getTagsInternal() {
        try {
            return tagAPI.getAllTags();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        }
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response addTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final TagForm tagForm) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();

        final User user = initDataObject.getUser();
        final boolean hasAccessToPortlet = Try.of(()->layoutAPI.doesUserHaveAccessToPortlet(TAGS, user)).getOrElse(false);
        if(hasAccessToPortlet || user.isAdmin()) {
            //We can assign tags to any user as long as we are admin.
            final String userId = tagForm.getUserId();
            final ImmutableList.Builder<Tag> savedTags = ImmutableList.builder();
            final ImmutableList.Builder<ErrorEntity> saveFails = ImmutableList.builder();
            final String fallbackSiteId = fallbackSiteId(request);
            final Map<String, RestTag> tags = tagForm.getTags();

            for (Entry<String, RestTag> entry : tags.entrySet()) {
                final String tagKey = entry.getKey();
                final RestTag tag = entry.getValue();
                final Host host = Try.of(() -> hostAPI.find(tag.siteId, user, true)).getOrNull();
                final String siteId;
                if (host == null) {
                    siteId = fallbackSiteId;
                    Logger.warn(TagResource.class, () -> String
                            .format("siteId `%s` isn't valid, DEFAULTING to siteId `%s`.",
                                    tag.siteId,
                                    siteId));
                } else {
                    siteId = host.getIdentifier();
                }
                try {
                    final Tag createdTag = tagAPI.getTagAndCreate(tagKey, userId, siteId);
                    savedTags.add(createdTag);
                    if(UtilMethods.isSet(userId)) {
                        tagAPI.addUserTagInode(createdTag, userId);
                    }
                } catch (DotDataException | DotSecurityException e) {
                    Logger.error(TagResource.class,
                            String.format("Exception creating tag %s", tag.label), e);
                    saveFails.add(new ErrorEntity("save-tag-error", e.getMessage(),
                            tag.label != null ? tag.label : "unknown"));
                }
            }
            return Response.ok(new ResponseEntityView(saveFails.build(),
                    ImmutableMap.of(TAGS, savedTags.build()))).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response updateTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final UpdateTagForm tagForm) {

        final InitDataObject initDataObject =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();

        final User user = initDataObject.getUser();
        final boolean hasAccessToPortlet = Try.of(()->layoutAPI.doesUserHaveAccessToPortlet(TAGS, user)).getOrElse(false);
        if(hasAccessToPortlet || user.isAdmin()) {
            //We can assign tags to any user as long as we are admin.
            final Tag tagByTagId = Try.of(()->tagAPI.getTagByTagId(tagForm.tagId)).getOrNull();
            if(null == tagByTagId){
                final String errorMessage = Try.of(()-> LanguageUtil
                        .get( user.getLocale(), "tag.id.not.found", tagForm.tagId ))
                        .getOrElse(String.format("Tag with id %s wasn't found.", tagForm.tagId)); //fallback message
                throw new DoesNotExistException(errorMessage);
            }
            try {
                tagAPI.updateTag(tagForm.tagId, tagForm.tagName, false, tagForm.siteId);
                 tagAPI.getTagByTagId(tagForm.tagId);
                //return Response.ok(new ResponseEntityView(saveFails.build(), ImmutableMap.of(TAGS, savedTags.build()))).build();

            } catch (DotDataException e) {
                Logger.error(TagResource.class, String.format("Exception removing tag  with id `%s`", tagForm.tagId), e);
                final String errorMessage = Try.of(()->LanguageUtil.get( user.getLocale(), "tag.error.delete", tagForm.tagId ))
                        .getOrElse(String.format("Error occurred removing tag %s .", tagForm.tagId)); //fallback message
                throw new BadRequestException(e, errorMessage);
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }


    private String fallbackSiteId(final HttpServletRequest request)  {
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        return host != null ? host.getIdentifier() : Host.SYSTEM_HOST;
    }

    @GET
    @JSONP
    @Path("/user/{userId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getTagsByUserId(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @PathParam("userId") final String userId) {

        final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
                .requiredAnonAccess(AnonymousAccess.READ)
                .requestAndResponse(request, response)
                .init();
        final User user = initDataObject.getUser();
        final boolean hasAccessToPortlet = Try.of(()->layoutAPI.doesUserHaveAccessToPortlet(
                TAGS, user)).getOrElse(false);
        if(hasAccessToPortlet || user.isAdmin()){
            final List<Tag> tags = Try.of(()->tagAPI.getTagsForUserByUserId(userId)).getOrElse(ImmutableList.of());
            if(tags.isEmpty()){
                final String errorMessage = Try.of(()-> LanguageUtil
                        .get( user.getLocale(), "tag.user.not.found", userId ))
                        .getOrElse(String.format("No tags are owned by user %s.", userId)); //fallback message
                throw new DoesNotExistException(errorMessage);
            }
            return Response.ok(new ResponseEntityView(tags)).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    @DELETE
    @JSONP
    @Path("/{tagId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response delete(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            @PathParam("tagId") final String tagId) {

        final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
                .requiredAnonAccess(AnonymousAccess.READ)
                .requestAndResponse(request, response)
                .init();
        final User user = initDataObject.getUser();
        final boolean hasAccessToPortlet = Try.of(()->layoutAPI.doesUserHaveAccessToPortlet(
                TAGS, user)).getOrElse(false);
        if(hasAccessToPortlet || user.isAdmin()){
                final Tag tagByTagId = Try.of(()->tagAPI.getTagByTagId(tagId)).getOrNull();
                if(null == tagByTagId){
                    final String errorMessage = Try.of(()-> LanguageUtil
                            .get( user.getLocale(), "tag.id.not.found", tagId ))
                            .getOrElse(String.format("Tag with id %s wasn't found.", tagId)); //fallback message
                   throw new DoesNotExistException(errorMessage);
                }
            try{
                tagAPI.deleteTag(tagByTagId);
                return Response.ok(new ResponseEntityView(OK)).build();
            } catch (Exception e) {
                Logger.error(TagResource.class, String.format("Exception removing tag  with id `%s`", tagId), e);
                final String errorMessage = Try.of(()->LanguageUtil.get( user.getLocale(), "tag.error.delete", tagId ))
                        .getOrElse(String.format("Error occurred removing tag %s .", tagId)); //fallback message
                throw new BadRequestException(e, errorMessage);
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

}
