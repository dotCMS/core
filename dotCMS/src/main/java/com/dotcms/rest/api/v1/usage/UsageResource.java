package com.dotcms.rest.api.v1.usage;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.telemetry.Metric;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.collectors.DashboardMetricsProvider;
import com.dotcms.telemetry.collectors.MetricStatsCollector;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.telemetry.ProfileType;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * 
     * <p>The profile parameter allows overriding the default metric collection profile.
     * Valid values: MINIMAL (default), STANDARD, FULL. This allows administrators to
     * view additional metrics beyond the default MINIMAL profile.</p>
     * 
     * @param profile optional profile override (MINIMAL, STANDARD, or FULL). Defaults to MINIMAL.
     */
    @Path("/summary")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(
            summary = "Get usage dashboard summary",
            description = "Retrieves key business metrics optimized for dashboard display including content, sites, users, and system metrics. " +
                          "Supports optional profile parameter (MINIMAL, STANDARD, FULL) to override the default metric collection profile.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Dashboard summary retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityUsageSummaryView.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid profile parameter"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public final Response getSummary(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   @QueryParam("profile") 
                                   @Parameter(
                                           description = "Metric collection profile to use. " +
                                                         "MINIMAL (default): Fast collection with 10-15 core metrics. " +
                                                         "STANDARD: Comprehensive collection with ~50 metrics. " +
                                                         "FULL: Complete collection with all available metrics. " +
                                                         "This allows administrators to view additional metrics beyond the default MINIMAL profile.",
                                           required = false,
                                           schema = @Schema(
                                                   type = "string",
                                                   allowableValues = {"MINIMAL", "STANDARD", "FULL"},
                                                   defaultValue = "MINIMAL"
                                           )
                                   )
                                   @DefaultValue("MINIMAL") final String profile) {

        Logger.debug(this, "Generating usage dashboard summary");
        
        // Initialize and validate user authentication/authorization
        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();

        try {
            // Parse and validate profile parameter
            ProfileType profileType = null;
            if (profile != null && !profile.isEmpty()) {
                try {
                    profileType = ProfileType.valueOf(profile.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Logger.warn(this, String.format("Invalid profile parameter '%s', using default MINIMAL", profile));
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(String.format("Invalid profile parameter: %s. Valid values: MINIMAL, STANDARD, FULL", profile))
                            .build();
                }
            }
            
            final User user = (User) request.getAttribute(com.liferay.portal.util.WebKeys.USER);
            final UsageSummary summary = collectUsageSummary(user, profileType);
            final Map<String, String> i18nMessagesMap = buildI18nMessagesMap(summary, user);
            return Response.ok(new ResponseEntityUsageSummaryView(summary, i18nMessagesMap)).build();
        } catch (final Exception e) {
            Logger.error(this, "Error generating usage summary: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error generating usage summary").build();
        }
    }

    /**
     * Collects metrics for dashboard display using {@link DashboardMetricsProvider}
     * to discover metrics annotated with {@link com.dotcms.telemetry.DashboardMetric}.
     *
     * <p>Metrics are collected via {@link MetricStatsCollector} which provides
     * configuration-driven caching for improved performance.</p>
     *
     * @param user the authenticated user (used for i18n)
     * @param profileOverride optional profile to override the default (null = use default from config)
     */
    private UsageSummary collectUsageSummary(final User user, final ProfileType profileOverride) {
        Logger.debug(this, () -> String.format("Collecting business metrics for usage dashboard (profile: %s)",
                profileOverride != null ? profileOverride : "default"));

        final DashboardMetricsProvider metricsProvider = CDIUtils.getBeanThrows(DashboardMetricsProvider.class);
        final Collection<MetricType> keyMetrics = metricsProvider.getDashboardMetrics(profileOverride);

        Logger.debug(this, () -> String.format("Found %d dashboard metrics to collect", keyMetrics.size()));

        // Collect metric names to pass to MetricStatsCollector
        final Set<String> metricNames = keyMetrics.stream()
                .map(MetricType::getName)
                .collect(java.util.stream.Collectors.toSet());

        // Use MetricStatsCollector which provides caching
        final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);
        final MetricsSnapshot snapshot = collector.getStats(metricNames, profileOverride);

        Logger.debug(this, () -> String.format("Collected %d metric values via MetricStatsCollector (with caching)",
                snapshot.getStats().size() + snapshot.getNotNumericStats().size()));

        // Build metric map from snapshot - combine numeric and non-numeric stats
        final Map<String, MetricValue> metricMap = new HashMap<>();

        // Add numeric stats
        for (MetricValue metricValue : snapshot.getStats()) {
            final String metricName = metricValue.getMetric().getName();

            // Handle duplicate "COUNT" keys by mapping to specific names based on feature
            // TODO: Remove this workaround once MetricType naming is standardized
            // See: https://github.com/dotCMS/core/issues/34042
            final String mapKey;
            if ("COUNT".equals(metricName)) {
                // Find the corresponding MetricType to get the feature
                final Optional<MetricType> metricTypeOpt = keyMetrics.stream()
                        .filter(mt -> "COUNT".equals(mt.getName()))
                        .findFirst();

                if (metricTypeOpt.isPresent()) {
                    switch (metricTypeOpt.get().getFeature()) {
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
            } else {
                mapKey = metricName;
            }

            metricMap.put(mapKey, metricValue);
            Logger.debug(this, () -> String.format("Collected metric: %s = %s (mapped to: %s)",
                    metricName, metricValue.getValue(), mapKey));
        }

        // Add non-numeric stats (getNotNumericStats returns Map<String, Object>)
        final Map<String, Object> nonNumericStatsMap = snapshot.getNotNumericStats();
        for (Map.Entry<String, Object> entry : nonNumericStatsMap.entrySet()) {
            final String metricName = entry.getKey();
            final Object value = entry.getValue();
            // Create a MetricValue wrapper for consistency
            final Metric metric = new Metric.Builder().name(metricName).build();
            final MetricValue metricValue = new MetricValue(metric, value);
            metricMap.put(metricName, metricValue);
            Logger.debug(this, () -> String.format("Collected non-numeric metric: %s = %s",
                    metricName, value));
        }

        Logger.debug(this, () -> String.format("Built metric map with %d total values", metricMap.size()));

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
     * 
     * <p>Display labels are provided as i18n keys (format: usage.metric.{METRIC_NAME}.label)
     * which are translated on the frontend using the i18nMessagesMap.</p>
     * 
     * @param keyMetrics the list of dashboard metrics with their annotations
     * @param metricMap the collected metric values keyed by mapped metric name
     * @param metricsProvider the DashboardMetricsProvider instance (reused to avoid repeated CDI lookups)
     * @return map of category names to maps of metric metadata (name, value, displayLabel as i18n key)
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
            
            // Get the i18n key for the display label instead of hardcoded English string
            final String displayLabelKey = getDisplayLabelI18nKey(mapKey);
            
            // Only include metrics that were successfully collected
            final MetricValue metricValue = metricMap.get(mapKey);
            if (metricValue != null) {
                // Use raw value to preserve numeric types for JSON serialization
                // This allows the frontend to properly format numbers (K, M suffixes, etc.)
                final Object rawValue = metricValue.getRawValue();
                
                // Create metric metadata object with name, value, and displayLabelKey (i18n key)
                final Map<String, Object> metricData = new HashMap<>();
                metricData.put("name", mapKey);
                metricData.put("value", rawValue);
                metricData.put("displayLabel", displayLabelKey); // Store i18n key, not translated string
                
                categoryMap.computeIfAbsent(category, k -> new HashMap<>())
                        .put(mapKey, metricData);
                
                Logger.debug(this, () -> String.format("Added metric %s (mapped from %s) to category %s with value %s, labelKey: %s",
                        mapKey, originalName, category, metricValue.getValue(), displayLabelKey));
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

    /**
     * Generates the i18n key for a metric's display label.
     * Format: usage.metric.{METRIC_NAME}.label
     * 
     * @param metricName the metric name (e.g., "COUNT_CONTENT")
     * @return the i18n key (e.g., "usage.metric.COUNT_CONTENT.label")
     */
    private String getDisplayLabelI18nKey(final String metricName) {
        return "usage.metric." + metricName + ".label";
    }

    /**
     * Generates the i18n key for a category's display title.
     * Format: usage.category.{category}.title
     * 
     * @param category the category name (e.g., "content")
     * @return the i18n key (e.g., "usage.category.content.title")
     */
    private String getCategoryTitleI18nKey(final String category) {
        return "usage.category." + category + ".title";
    }

    /**
     * Builds the i18n messages map containing all translation keys needed by the frontend.
     * This includes:
     * - Display labels for all metrics
     * - Category titles for all categories
     * 
     * @param summary the usage summary containing all metrics and categories
     * @param user the authenticated user (used for locale-specific translations)
     * @return map of i18n keys to translated strings
     */
    private Map<String, String> buildI18nMessagesMap(final UsageSummary summary, final User user) {
        final Map<String, String> i18nMap = new HashMap<>();
        
        if (summary == null || summary.getMetrics() == null) {
            return i18nMap;
        }
        
        // Collect all unique metric names and categories
        final Set<String> metricNames = new HashSet<>();
        final Set<String> categories = new HashSet<>();
        
        for (final Map.Entry<String, Map<String, Object>> categoryEntry : summary.getMetrics().entrySet()) {
            final String category = categoryEntry.getKey();
            categories.add(category);
            
            if (categoryEntry.getValue() != null) {
                for (final Map.Entry<String, Object> metricEntry : categoryEntry.getValue().entrySet()) {
                    final String metricName = metricEntry.getKey();
                    metricNames.add(metricName);
                }
            }
        }
        
        // Add metric display label translations
        for (final String metricName : metricNames) {
            final String i18nKey = getDisplayLabelI18nKey(metricName);
            try {
                final String translated = LanguageUtil.get(user, i18nKey);
                i18nMap.put(i18nKey, translated);
            } catch (Exception e) {
                Logger.debug(this, () -> String.format("Could not translate key %s, using key as fallback", i18nKey));
                // Fallback to the key itself if translation fails
                i18nMap.put(i18nKey, i18nKey);
            }
        }
        
        // Add category title translations
        for (final String category : categories) {
            final String i18nKey = getCategoryTitleI18nKey(category);
            try {
                final String translated = LanguageUtil.get(user, i18nKey);
                i18nMap.put(i18nKey, translated);
            } catch (Exception e) {
                Logger.debug(this, () -> String.format("Could not translate key %s, using key as fallback", i18nKey));
                // Fallback to the key itself if translation fails
                i18nMap.put(i18nKey, i18nKey);
            }
        }
        
        Logger.debug(this, () -> String.format("Built i18n messages map with %d translation keys", i18nMap.size()));
        
        return i18nMap;
    }

}