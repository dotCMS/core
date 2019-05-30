package com.dotcms.rest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Path("/v1/tags")
public class TagResource {

	private final TagAPI tagAPI;
    private final WebResource webResource;

    @SuppressWarnings("unused")
    public TagResource() {
        this(APILocator.getTagAPI(), new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected TagResource(TagAPI tagAPI, WebResource webResource) {
        this.tagAPI = tagAPI;
        this.webResource = webResource;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Map<String, RestTag> list(@Context final HttpServletRequest request,
            @QueryParam("name") final String  tagName,
            @QueryParam("siteId") final String siteId) {


        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

        final User user = initDataObject.getUser();

        List<Tag> tags = UtilMethods.isSet(tagName)
                ? searchTagsInternal(tagName, siteId, user)
                : getTagsInternal();

        final Map<String, RestTag> hash = Maps.newHashMapWithExpectedSize(tags.size());
        final TagTransform transform = new TagTransform();
        for (final Tag tag : tags) {
            hash.put(tag.getTagName(), transform.appToRest(tag));
        }
        return hash;
    }

    private List<Tag> searchTagsInternal(final String tagName, final String siteOrFolderId,
            final User user) {
        List<Tag> tags;

        try {
            final Host host = APILocator.getHostAPI().find(siteOrFolderId, user, false);
            String internalSiteOrFolderId = siteOrFolderId;

            if ((!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getInode()))
                    && UtilMethods.isSet(siteOrFolderId)) {
                internalSiteOrFolderId = APILocator.getFolderAPI()
                        .find(siteOrFolderId, user, false).getHostId();
            }

            tags = APILocator.getTagAPI().getSuggestedTag(tagName, internalSiteOrFolderId);
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
}
