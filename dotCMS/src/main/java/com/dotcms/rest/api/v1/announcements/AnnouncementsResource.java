package com.dotcms.rest.api.v1.announcements;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.system.announcements.Announcement;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This REST Resource behaves as a proxy to the remote dotCMS instance that provides the announcements
 */
@Path("/v1/announcements")
@Tag(name = "Announcements", description = "System announcements and notifications")
public class AnnouncementsResource {

    private final WebResource webResource;

    private final AnnouncementsHelper helper;

    /**
     * Default constructor
     */
    public AnnouncementsResource() {
        this(new WebResource(), new AnnouncementsHelperImpl());
    }

    /**
     * Constructor
     * @param webResource WebResource
     * @param helper AnnouncementsHelper
     */
    @VisibleForTesting
    AnnouncementsResource(final WebResource webResource, final AnnouncementsHelper helper) {
        this.webResource = webResource;
        this.helper = helper;
    }

    /**
     * Get the announcements
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param langIdOrCode String
     * @param refreshCache boolean
     * @param limit int
     * @return Response
     */
    @GET
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final ResponseEntityView<List<Announcement>> announcements(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("refreshCache") final boolean refreshCache,
            @QueryParam("limit") final int limit
    ) {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            final List<Announcement> announcements = helper.getAnnouncements(refreshCache , limit, user);
            return new ResponseEntityView<>(announcements); // 200
    }

}
