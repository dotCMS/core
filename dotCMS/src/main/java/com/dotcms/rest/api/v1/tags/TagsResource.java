package com.dotcms.rest.api.v1.tags;

import static com.dotcms.rest.tag.TagsResourceHelper.toRestTagMap;


import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.container.ResponseEntityContainerView;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This resource provides all the different end-points associated to information and actions that
 * the front-end can perform on the {@link com.dotmarketing.tag.model.Tag}.
 */

@Path("/v1/tags")
public class TagsResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TAGS = "tags";
    private final WebResource webResource;
    private final TagAPI tagAPI;

    public TagsResource() {
        this(APILocator.getTagAPI(), APILocator.getHostAPI(), APILocator.getFolderAPI(), new WebResource());
    }

    @VisibleForTesting
    protected TagsResource(final TagAPI tagAPI,final HostAPI hostAPI, final FolderAPI folderAPI, final WebResource webResource) {
        this.tagAPI = tagAPI;
        this.webResource = webResource;
    }

    /**
     * Creates a new tag
     * @param request
     * @param response
     * @param tagForm
     * @return
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityTagView saveNew(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final TagForm tagForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this,
                () -> "Adding tag. Request payload is : " + JsonUtil.getJsonStringFromObject(tagForm));

        final Tag tag = tagAPI.getTagAndCreate(tagForm.getTag(), user.getUserId(), tagForm.getSiteId());

        ActivityLogger.logInfo(this.getClass(), "Save Tag",
                "User " + user.getPrimaryKey() + " saved " + tag.getTagName(), host.getHostname());

        Logger.debug(this, ()-> "The tag: " + tag.getTagId() + " has been saved");

        return new ResponseEntityTagView(Collections.singletonList(tag));
    }
}