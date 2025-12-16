package com.dotcms.telemetry.rest;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.collectors.MetricStatsCollector;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Tag(name = "Administration")
@Path("/v1/telemetry")
public class TelemetryResource {

    @Path("/stats")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Retrieves dotCMS usage data",
            description = "Collects telemetry metrics based on the specified profile. " +
                    "Defaults to FULL profile to return all available metrics. " +
                    "Use MINIMAL profile for dashboard (faster, ~10 metrics) or FULL for complete data (~100+ metrics).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation =
                                            ResponseEntityMetricsSnapshotView.class)))})
    public final Response getData(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @Parameter(description = "Comma-separated list of specific metric names to retrieve. " +
                                          "If not specified, all metrics matching the profile will be returned.")
                                  @QueryParam("metricNames") final String metricNames,
                                  @Parameter(description = "Profile type to use for metric collection. " +
                                          "Options: MINIMAL (10-15 core metrics, <5s), STANDARD (~50 metrics, <15s), " +
                                          "FULL (all 100+ metrics). Defaults to FULL for API access.")
                                  @QueryParam("profile") final String profile) throws DotDataException {

        Logger.debug(this, ()-> "Generating dotCMS Telemetry data");
        Logger.debug(this, ()-> "Client Info: " + Try.of(()->APILocator.getMetricsAPI().getClient()));
        Logger.debug(this, ()-> "Metric Names: " + metricNames);
        Logger.debug(this, ()-> "Profile: " + profile);
        new WebResource.InitBuilder(new WebResource())
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .init();

        final Set<String> metricNameSet = UtilMethods.isSet(metricNames)?
                Stream.of(metricNames.split(StringPool.COMMA)).collect(Collectors.toSet()) : Set.of();

        // Parse profile parameter, default to FULL for direct API access
        // (MINIMAL is used by dashboard for performance, configured via telemetry.default.profile)
        final ProfileType profileType = parseProfile(profile, ProfileType.FULL);
        Logger.debug(this, ()-> "Using profile: " + profileType);

        final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);
        return Response.ok(new ResponseEntityMetricsSnapshotView(collector.getStats(metricNameSet, profileType)))
                .build();
    }

    /**
     * Parses the profile parameter from the request.
     *
     * @param profileParam the profile query parameter value
     * @param defaultProfile the default profile to use if parameter is not set or invalid
     * @return the parsed ProfileType
     */
    private ProfileType parseProfile(final String profileParam, final ProfileType defaultProfile) {
        if (!UtilMethods.isSet(profileParam)) {
            return defaultProfile;
        }

        try {
            return ProfileType.valueOf(profileParam.toUpperCase());
        } catch (final IllegalArgumentException e) {
            Logger.warn(this, String.format("Invalid profile parameter '%s', using default: %s",
                    profileParam, defaultProfile), e);
            return defaultProfile;
        }
    }


}
