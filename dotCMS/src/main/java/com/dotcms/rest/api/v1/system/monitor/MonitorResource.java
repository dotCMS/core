package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * This REST Endpoint provides a set of probes to check the status of the different subsystems used
 * by dotCMS. This tool is crucial for Engineering Teams to check that dotCMS is running properly.
 *
 * @author Brent Griffin
 * @since Jul 18th, 2018
 */
@SwaggerCompliant(value = "System administration and configuration APIs", batch = 4)
@Path("/v1/{a:system-status|probes}")
@Tag(name = "System Monitoring")
public class MonitorResource {

    private static final int SERVICE_UNAVAILABLE = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
    private static final int FORBIDDEN           = HttpServletResponse.SC_FORBIDDEN;

    /**
     * This /startup and /ready probe is heavy - it is intended to report on when dotCMS first comes up
     * and can serve traffic. It gives a report on dotCMS and subsystems status
     * and should not be run as an alive check
     *
     * it tests
     * 1. dotCMS can connect to the db
     * 2. dotCMS can connect to elasticsearch
     * 3. dotCMS can write to the file systems, both local and nfs
     * 4. that the cache is responding
     * 5. that dotCMS has started up.
     *
     * It is important to note that this resource runs through the CMSFilter and the rules engine before responding.
     *
     * @param request
     * @return
     */
    @Operation(
        summary = "Heavy system check",
        description = "Comprehensive health check for dotCMS startup and subsystems status"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "System is healthy",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMonitorStatsView.class))),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - access not granted",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "503", 
                    description = "Service unavailable - system not healthy",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @CloseDBIfOpened
    public Response heavyCheck(final @Context HttpServletRequest request)  {
        final MonitorHelper helper = new MonitorHelper(request , true);
        if(!helper.accessGranted) {
            return Response.status(FORBIDDEN).entity(Map.of()).build();
        }

        if(!helper.isStartedUp()) {
            return Response.status(SERVICE_UNAVAILABLE).build();
        }

        if(!helper.getMonitorStats(request).isDotCMSHealthy()) {
            return Response.status(SERVICE_UNAVAILABLE).build();
        }

        return Response.ok(helper.getMonitorStats(request).toMap()).build();
    }

    @Operation(
        summary = "System ready check",
        description = "Checks if the system is ready to serve traffic (alias for heavy check)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "System is ready",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMonitorStatsView.class))),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - access not granted",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "503", 
                    description = "Service unavailable - system not ready",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @JSONP
    @Path("/{a:|startup|ready|heavy}")
    @Produces(MediaType.APPLICATION_JSON)
    @CloseDBIfOpened
    public Response ready(final @Context HttpServletRequest request)  {
        return heavyCheck(request);
    }

    /**
     * This /alive probe is lightweight - it checks if the server is up by requesting a common object from
     * the dotCMS cache layer twice in a row.  By the time a request gets here it has
     * already run through the CMSFilter) .
     *
     * It is intended to be used by kubernetes and load balancers to determine if the server is up.
     * @param request
     * @return
     */
    @Operation(
        summary = "Light system check",
        description = "Lightweight health check for load balancers and kubernetes"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "System is alive (no body)"
                    ),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - access not granted (no body)"),
        @ApiResponse(responseCode = "503", 
                    description = "Service unavailable - system not alive (no body)")
    })
    @GET
    @Path("/{a:alive|light}")
    @CloseDBIfOpened
    @Produces(MediaType.APPLICATION_JSON)
    public Response lightCheck(final @Context HttpServletRequest request)  {
        final MonitorHelper helper = new MonitorHelper(request, false);
        if(!helper.accessGranted) {
            return Response.status(FORBIDDEN).build();
        }
        if(!helper.isStartedUp()) {
            return Response.status(SERVICE_UNAVAILABLE).build();
        }

        //try this twice as it is an imperfect test
        if(helper.isCacheHealthy() ) {
            return Response.ok().build();
        }

        return Response.status(SERVICE_UNAVAILABLE).build();
    }

}
