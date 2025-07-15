package com.dotcms.rest.api.v1.announcements;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This REST Resource behaves as a proxy to the remote dotCMS instance that provides the announcements
 */
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Path("/v1/announcements")
@Tag(name = "Announcements")
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

    @Operation(
        summary = "Get system announcements",
        description = "Retrieves system announcements from the remote dotCMS instance. Acts as a proxy to the announcement service with optional cache refresh and result limiting."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Announcements retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityAnnouncementListView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final ResponseEntityAnnouncementListView announcements(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Whether to refresh the cache before retrieving announcements (default: false)") @QueryParam("refreshCache") final boolean refreshCache,
            @Parameter(description = "Maximum number of announcements to return (default: no limit)") @QueryParam("limit") final int limit
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
            return new ResponseEntityAnnouncementListView(announcements); // 200
    }

}
