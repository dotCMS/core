package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;

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
            @QueryParam("siteOrFolder") final String siteOrFolderId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

        final User user = initDataObject.getUser();

        List<Tag> tags;

        if(UtilMethods.isSet(tagName)) {
            tags = searchTagsInternal(tagName, siteOrFolderId, user);
        } else {
            tags = getTagsInternal();
        }

        Map<String, RestTag> hash = Maps.newHashMapWithExpectedSize(tags.size());
        TagTransform transform = new TagTransform();
        for (Tag tag : tags) {
            hash.put(tag.getTagName(), transform.appToRest(tag));
        }
        return hash;
    }

    private List<Tag> searchTagsInternal(
            @QueryParam("name") String tagName,
            @QueryParam("siteOrFolder") String siteOrFolderId,
            User user) {
        List<Tag> tags;

        try {
            Host host = APILocator.getHostAPI().find(siteOrFolderId, user, false);
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
