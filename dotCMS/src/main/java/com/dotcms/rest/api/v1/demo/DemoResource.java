package com.dotcms.rest.api.v1.demo;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.util.Logger;
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
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

/**
 * Demo endpoint — illustrates the dotCMS JAX-RS REST pattern.
 * Requires authentication. Used to validate the full dev workflow:
 * incremental core rebuild → restart → frontend API integration.
 */
@Tag(name = "Demo", description = "Development workflow demo endpoint")
@Path("/v1/demo")
public class DemoResource {

    private final WebResource webResource = new WebResource();

    @Operation(
        summary = "Get server greeting",
        description = "Returns a greeting with server timestamp. Authenticated endpoint " +
                      "used to verify the REST API is reachable from the frontend dev server."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Greeting returned",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ResponseEntityDemoGreetingView.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/greet")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ResponseEntityView<Map<String, String>> greet(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {

        webResource.init(request, response, true);

        Logger.info(this, "Demo greet endpoint called");

        final long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        return new ResponseEntityView<>(Map.of(
            "message", "dotCMS is live",
            "serverTime", Instant.now().toString(),
            "uptimeSeconds", String.valueOf(uptimeSeconds)
        ));
    }
}
