package com.dotcms.telemetry.rest;

import com.dotcms.telemetry.collectors.MetricStatsCollector;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/telemetry")
public class TelemetryResource {

    @Path("/stats")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Retrieves dotCMS usage data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation =
                                            ResponseEntityMetricsSnapshotView.class)))})
    public final Response getData(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response) {
        new WebResource.InitBuilder(new WebResource())
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .init();

        return Response.ok(new ResponseEntityMetricsSnapshotView(MetricStatsCollector.getStats()))
                .build();
    }

}
