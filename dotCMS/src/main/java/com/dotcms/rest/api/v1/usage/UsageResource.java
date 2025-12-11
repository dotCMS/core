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
        Logger.debug(this, "Collecting business metrics for usage dashboard");

        final DashboardMetricsProvider metricsProvider = CDIUtils.getBeanThrows(DashboardMetricsProvider.class);
        final Collection<MetricType> keyMetrics = metricsProvider.getDashboardMetrics();

        Logger.debug(this, () -> String.format("Found %d dashboard metrics to collect", keyMetrics.size()));

        // Collect metric values - use metricType.getName() as the map key
        final Map<String, MetricValue> metricMap = new HashMap<>();
        for (MetricType metricType : keyMetrics) {
            try {
                final String metricName = metricType.getName();
                final Optional<MetricValue> metricValueOpt = metricType.getStat();
                if (metricValueOpt.isPresent()) {
                    final MetricValue metricValue = metricValueOpt.get();
                    
                    // Handle duplicate "COUNT" keys by mapping to specific names based on feature
                    // TODO: Remove this workaround once MetricType naming is standardized
                    // See: https://github.com/dotCMS/core/issues/34042
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
                    Logger.debug(this, () -> String.format("Metric %s returned empty value", metricName));
                }
            } catch (Exception e) {
                Logger.warn(this, "Failed to collect metric: " + metricType.getName(), e);
            }
        }

        Logger.debug(this, () -> String.format("Collected %d metric values from %d dashboard metrics", metricMap.size(), keyMetrics.size()));

        // Build dynamic summary organized by category
        // Pass metricsProvider to avoid repeated CDI lookups in the loop
        final Map<String, Map<String, Object>> metricsByCategory = buildMetricsByCategory(keyMetrics, metricMap, metricsProvider);

        Logger.debug(this, () -> String.format("Organized metrics into %d categories", metricsByCategory.size()));

        return UsageSummary.builder()
                .metrics(metricsByCategory)
                .lastUpdated(Instant.now())
                .build();
    }

    /**
     * Organizes metrics by their category as defined by @DashboardMetric annotation.
     * Only includes metrics that were successfully collected (present in metricMap).
     * Uses the mapped metric name (e.g., "COUNT_CONTENT" instead of "COUNT") for consistency.
     * Includes display labels from MetricType.getDisplayLabel().
     * 
     * @param keyMetrics the list of dashboard metrics with their annotations
     * @param metricMap the collected metric values keyed by mapped metric name
     * @param metricsProvider the DashboardMetricsProvider instance (reused to avoid repeated CDI lookups)
     * @return map of category names to maps of metric metadata (name, value, displayLabel)
     */
    private Map<String, Map<String, Object>> buildMetricsByCategory(
            final Collection<MetricType> keyMetrics,
            final Map<String, MetricValue> metricMap,
            final DashboardMetricsProvider metricsProvider) {
        
        final Map<String, Map<String, Object>> categoryMap = new HashMap<>();
        
        for (final MetricType metricType : keyMetrics) {
            final String originalName = metricType.getName();
            
            // Get the mapped key (e.g., "COUNT" -> "COUNT_CONTENT" for CONTENTLETS feature)
            final String mapKey = getMappedMetricName(metricType);
            
            // Get the category from @DashboardMetric annotation, or use "other" as default
            // Pass metricsProvider to avoid repeated CDI lookups
            final String category = getCategoryFromMetric(metricType, metricsProvider);
            
            // Get the display label from MetricType interface
            final String displayLabel = metricType.getDisplayLabel();
            
            // Only include metrics that were successfully collected
            final MetricValue metricValue = metricMap.get(mapKey);
            if (metricValue != null) {
                // Use raw value to preserve numeric types for JSON serialization
                // This allows the frontend to properly format numbers (K, M suffixes, etc.)
                final Object rawValue = metricValue.getRawValue();
                
                // Create metric metadata object with name, value, and displayLabel
                final Map<String, Object> metricData = new HashMap<>();
                metricData.put("name", mapKey);
                metricData.put("value", rawValue);
                metricData.put("displayLabel", displayLabel);
                
                categoryMap.computeIfAbsent(category, k -> new HashMap<>())
                        .put(mapKey, metricData);
                
                Logger.debug(this, () -> String.format("Added metric %s (mapped from %s) to category %s with value %s, label: %s",
                        mapKey, originalName, category, metricValue.getValue(), displayLabel));
            }
        }
        
        return categoryMap;
    }


    /**
     * Gets the mapped metric name, handling special cases like "COUNT" -> "COUNT_CONTENT".
     */
    private String getMappedMetricName(final MetricType metricType) {
        final String metricName = metricType.getName();
        
        // Handle duplicate "COUNT" keys by mapping to specific names based on feature
        if ("COUNT".equals(metricName)) {
            switch (metricType.getFeature()) {
                case CONTENTLETS:
                    return "COUNT_CONTENT";
                case LANGUAGES:
                    return "COUNT_LANGUAGES";
                default:
                    return metricName;
            }
        }
        
        return metricName;
    }

    /**
     * Extracts the category from a metric's @DashboardMetric annotation.
     * If no category is specified, returns "other".
     * 
     * @param metricType the metric to get the category for
     * @param metricsProvider the DashboardMetricsProvider instance (passed to avoid repeated CDI lookups)
     * @return the category name, or "other" if not specified
     */
    private String getCategoryFromMetric(final MetricType metricType, final DashboardMetricsProvider metricsProvider) {
        final String category = metricsProvider.getCategory(metricType);
        return category != null && !category.isEmpty() ? category : "other";
    }


}