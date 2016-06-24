package com.dotcms.rest;

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
    public Map<String, RestTag> list(@Context HttpServletRequest request) {
        TagTransform transform = new TagTransform();
        List<Tag> tags = getTagsInternal();
        Map<String, RestTag> hash = Maps.newHashMapWithExpectedSize(tags.size());
        for (Tag tag : tags) {
            hash.put(tag.getTagName(), transform.appToRest(tag));
        }
        return hash;
    }

    private List<Tag> getTagsInternal() {
        try {
            return tagAPI.getAllTags();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (InvalidLicenseException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
