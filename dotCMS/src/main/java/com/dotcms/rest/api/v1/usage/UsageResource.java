package com.dotcms.rest.api.v1.usage;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.collectors.DashboardMetricsProvider;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST resource for usage dashboard data.
 * 
 * <p>Collects metrics via {@link com.dotcms.telemetry.collectors.DashboardMetricsProvider},
 * which automatically discovers all {@link com.dotcms.telemetry.DashboardMetric}-annotated
 * MetricType implementations. To include a metric in the dashboard, annotate it with
 * {@code @DashboardMetric}.</p>
 */
@Tag(name = "Usage", 
     description = "Provides business intelligence metrics for dashboard usage")
@Path("/v1/usage")
public class UsageResource {

    private final WebResource webResource;

    public UsageResource() {
        this(new WebResource());
    }

    public UsageResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * Retrieves a summary of key business metrics for the usage dashboard.
     * This endpoint provides optimized, dashboard-ready business intelligence data
     * including content counts, site metrics, user activity, and system configuration.
     */
    @Path("/summary")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(
            summary = "Get usage dashboard summary",
            description = "Retrieves key business metrics optimized for dashboard display including content, sites, users, and system metrics",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Dashboard summary retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityUsageSummaryView.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public final Response getSummary(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response) {

        Logger.debug(this, "Generating usage dashboard summary");
        
        // Initialize and validate user authentication/authorization
        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();

        try {
            final UsageSummary summary = collectUsageSummary();
            return Response.ok(new ResponseEntityUsageSummaryView(summary)).build();
        } catch (final Exception e) {
            Logger.error(this, "Error generating usage summary: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error generating usage summary").build();
        }
    }

    /**
     * Collects metrics for dashboard display using {@link DashboardMetricsProvider}
     * to discover metrics annotated with {@link com.dotcms.telemetry.DashboardMetric}.
     */
    private UsageSummary collectUsageSummary() {
        Logger.info(this, "Collecting business metrics for usage dashboard");

        final DashboardMetricsProvider metricsProvider = CDIUtils.getBeanThrows(DashboardMetricsProvider.class);
        final Collection<MetricType> keyMetrics = metricsProvider.getDashboardMetrics();
        
        Logger.info(this, () -> String.format("Found %d dashboard metrics to collect", keyMetrics.size()));

        // Collect metric values - use metricType.getName() as the map key
        final Map<String, MetricValue> metricMap = new HashMap<>();
        for (MetricType metricType : keyMetrics) {
            try {
                final String metricName = metricType.getName();
                final Optional<MetricValue> metricValueOpt = metricType.getStat();
                if (metricValueOpt.isPresent()) {
                    final MetricValue metricValue = metricValueOpt.get();
                    
                    // Handle duplicate "COUNT" keys by mapping to specific names based on feature
                    final String mapKey;
                    if ("COUNT".equals(metricName)) {
                        // Use feature to disambiguate generic "COUNT" metric
                        switch (metricType.getFeature()) {
                            case CONTENTLETS:
                                mapKey = "COUNT_CONTENT";
                                break;
                            case LANGUAGES:
                                mapKey = "COUNT_LANGUAGES";
                                break;
                            default:
                                mapKey = metricName;
                        }
                    } else {
                        mapKey = metricName;
                    }
                    
                    metricMap.put(mapKey, metricValue);
                    Logger.debug(this, () -> String.format("Collected metric: %s = %s (mapped to: %s)", 
                            metricName, metricValue.getValue(), mapKey));
                } else {
                    Logger.info(this, () -> String.format("Metric %s returned empty value", metricName));
                }
            } catch (Exception e) {
                Logger.warn(this, "Failed to collect metric: " + metricType.getName(), e);
            }
        }
        
        Logger.info(this, () -> String.format("Collected %d metric values from %d dashboard metrics", metricMap.size(), keyMetrics.size()));

        // Build summary from collected metrics
        return UsageSummary.builder()
                .contentMetrics(buildContentMetrics(metricMap))
                .siteMetrics(buildSiteMetrics(metricMap))
                .userMetrics(buildUserMetrics(metricMap))
                .systemMetrics(buildSystemMetrics(metricMap))
                .lastUpdated(Instant.now())
                .build();
    }

    private UsageSummary.ContentMetrics buildContentMetrics(final Map<String, MetricValue> metricMap) {
        final long totalContent = getMetricValueAsLong(metricMap, "COUNT_CONTENT", 0L);
        final long recentlyEdited = getMetricValueAsLong(metricMap, "CONTENTS_RECENTLY_EDITED", 0L);
        final long contentTypesWithWorkflows = getMetricValueAsLong(metricMap, "CONTENT_TYPES_ASSIGNED", 0L);
        final String lastContentEdited = getMetricValueAsString(metricMap, "LAST_CONTENT_EDITED", "N/A");
        
        // For now, using totalContent as contentTypes placeholder - can be enhanced later
        final long contentTypes = totalContent > 0 ? 1 : 0;
        
        Logger.info(this, () -> String.format("Content metrics - totalContent: %d, recentlyEdited: %d, contentTypesWithWorkflows: %d", 
                totalContent, recentlyEdited, contentTypesWithWorkflows));
        
        return new UsageSummary.ContentMetrics(totalContent, contentTypes, recentlyEdited, contentTypesWithWorkflows, lastContentEdited);
    }

    private UsageSummary.SiteMetrics buildSiteMetrics(final Map<String, MetricValue> metricMap) {
        final long totalSites = getMetricValueAsLong(metricMap, "COUNT_OF_SITES", 0L);
        final long activeSites = getMetricValueAsLong(metricMap, "COUNT_OF_ACTIVE_SITES", totalSites);
        final long templates = getMetricValueAsLong(metricMap, "COUNT_OF_TEMPLATES", 0L);
        final long siteAliases = getMetricValueAsLong(metricMap, "ALIASES_SITES_COUNT", 0L);
        
        Logger.info(this, () -> String.format("Site metrics - totalSites: %d, activeSites: %d, templates: %d, aliases: %d", 
                totalSites, activeSites, templates, siteAliases));
        
        return new UsageSummary.SiteMetrics(totalSites, activeSites, templates, siteAliases);
    }

    private UsageSummary.UserMetrics buildUserMetrics(final Map<String, MetricValue> metricMap) {
        final long activeUsers = getMetricValueAsLong(metricMap, "ACTIVE_USERS_COUNT", 0L);
        final long totalUsers = getMetricValueAsLong(metricMap, "COUNT_OF_USERS", 0L);
        // LAST_LOGIN_COUNT metric doesn't exist, using 0 as default
        final long recentLogins = 0L;
        final String lastLogin = getMetricValueAsString(metricMap, "LAST_LOGIN", "N/A");
        
        Logger.info(this, () -> String.format("User metrics - activeUsers: %d, totalUsers: %d, lastLogin: %s", 
                activeUsers, totalUsers, lastLogin));
        
        return new UsageSummary.UserMetrics(activeUsers, totalUsers, recentLogins, lastLogin);
    }

    private UsageSummary.SystemMetrics buildSystemMetrics(final Map<String, MetricValue> metricMap) {
        final long languages = getMetricValueAsLong(metricMap, "COUNT_LANGUAGES", 1L);
        final long workflowSchemes = getMetricValueAsLong(metricMap, "SCHEMES_COUNT", 0L);
        final long workflowSteps = getMetricValueAsLong(metricMap, "STEPS_COUNT", 0L);
        final long liveContainers = getMetricValueAsLong(metricMap, "COUNT_OF_LIVE_CONTAINERS", 0L);
        final long builderTemplates = getMetricValueAsLong(metricMap, "COUNT_OF_TEMPLATE_BUILDER_TEMPLATES", 0L);
        
        Logger.info(this, () -> String.format("System metrics - languages: %d, workflowSchemes: %d, workflowSteps: %d, liveContainers: %d, builderTemplates: %d", 
                languages, workflowSchemes, workflowSteps, liveContainers, builderTemplates));
        
        return new UsageSummary.SystemMetrics(languages, workflowSchemes, workflowSteps, liveContainers, builderTemplates);
    }

    private long getMetricValueAsLong(final Map<String, MetricValue> metricMap, final String metricName, final long defaultValue) {
        final MetricValue metric = metricMap.get(metricName);
        if (metric == null) {
            return defaultValue;
        }
        
        try {
            final Object value = metric.getValue();
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            Logger.warn(this, "Unable to parse metric value as long: " + metricName + " = " + metric.getValue());
            return defaultValue;
        }
    }

    private String getMetricValueAsString(final Map<String, MetricValue> metricMap, final String metricName, final String defaultValue) {
        final MetricValue metric = metricMap.get(metricName);
        return metric != null ? metric.getValue().toString() : defaultValue;
    }
}