package com.dotcms.rest.api.v1.usage;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.collectors.content.LastContentEditedDatabaseMetricType;
import com.dotcms.telemetry.collectors.content.RecentlyEditedContentDatabaseMetricType;
import com.dotcms.telemetry.collectors.content.TotalContentsDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.TotalActiveSitesDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.TotalSitesDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.TotalAliasesAllSitesDatabaseMetricType;
import com.dotcms.telemetry.collectors.template.TotalBuilderTemplatesDatabaseMetricType;
import com.dotcms.telemetry.collectors.template.TotalTemplatesDatabaseMetricType;
import com.dotcms.telemetry.collectors.theme.TotalLiveContainerDatabaseMetricType;
import com.dotcms.telemetry.collectors.user.ActiveUsersDatabaseMetricType;
import com.dotcms.telemetry.collectors.user.LastLoginDatabaseMetricType;
import com.dotcms.telemetry.collectors.user.TotalUsersDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.ContentTypesDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.SchemesDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.StepsDatabaseMetricType;
import com.dotcms.telemetry.collectors.language.TotalLanguagesDatabaseMetricType;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST resource for usage dashboard data.
 * Provides optimized business metrics for dashboard consumption.
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
        
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
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
     * Collect and process business metrics for dashboard display
     */
    private UsageSummary collectUsageSummary() {
        Logger.debug(this, "Collecting business metrics for usage dashboard");

        // Define key metric collectors we need
        final Collection<com.dotcms.telemetry.MetricType> keyMetrics = Arrays.asList(
            new TotalContentsDatabaseMetricType(),
            new RecentlyEditedContentDatabaseMetricType(),
            new ContentTypesDatabaseMetricType(),
            new TotalSitesDatabaseMetricType(), 
            new TotalActiveSitesDatabaseMetricType(),
            new TotalAliasesAllSitesDatabaseMetricType(),
            new ActiveUsersDatabaseMetricType(),
            new TotalUsersDatabaseMetricType(),
            new LastLoginDatabaseMetricType(),
            new LastContentEditedDatabaseMetricType(),
            new TotalTemplatesDatabaseMetricType(),
            new TotalBuilderTemplatesDatabaseMetricType(),
            new TotalLiveContainerDatabaseMetricType(),
            new TotalLanguagesDatabaseMetricType(),
            new SchemesDatabaseMetricType(),
            new StepsDatabaseMetricType()
        );

        // Collect metric values - handle duplicate keys by using metric type to create unique keys
        final Map<String, MetricValue> metricMap = new HashMap<>();
        for (com.dotcms.telemetry.MetricType metricType : keyMetrics) {
            try {
                final Optional<MetricValue> metricValueOpt = metricType.getStat();
                if (metricValueOpt.isPresent()) {
                    final MetricValue metricValue = metricValueOpt.get();
                    final String baseName = metricValue.getMetric().getName();
                    
                    // Handle duplicate "COUNT" keys by using metric type class name
                    String mapKey = baseName;
                    if ("COUNT".equals(baseName)) {
                        if (metricType instanceof TotalContentsDatabaseMetricType) {
                            mapKey = "COUNT_CONTENT";
                        } else if (metricType instanceof TotalLanguagesDatabaseMetricType) {
                            mapKey = "COUNT_LANGUAGES";
                        }
                    }
                    
                    metricMap.put(mapKey, metricValue);
                }
            } catch (Exception e) {
                Logger.warn(this, "Failed to collect metric: " + metricType.getName(), e);
            }
        }

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
        
        return new UsageSummary.ContentMetrics(totalContent, contentTypes, recentlyEdited, contentTypesWithWorkflows, lastContentEdited);
    }

    private UsageSummary.SiteMetrics buildSiteMetrics(final Map<String, MetricValue> metricMap) {
        final long totalSites = getMetricValueAsLong(metricMap, "COUNT_OF_SITES", 0L);
        final long activeSites = getMetricValueAsLong(metricMap, "COUNT_OF_ACTIVE_SITES", totalSites);
        final long templates = getMetricValueAsLong(metricMap, "COUNT_OF_TEMPLATES", 0L);
        final long siteAliases = getMetricValueAsLong(metricMap, "ALIASES_SITES_COUNT", 0L);
        
        return new UsageSummary.SiteMetrics(totalSites, activeSites, templates, siteAliases);
    }

    private UsageSummary.UserMetrics buildUserMetrics(final Map<String, MetricValue> metricMap) {
        final long activeUsers = getMetricValueAsLong(metricMap, "ACTIVE_USERS_COUNT", 0L);
        final long totalUsers = getMetricValueAsLong(metricMap, "COUNT_OF_USERS", 0L);
        // LAST_LOGIN_COUNT metric doesn't exist, using 0 as default
        final long recentLogins = 0L;
        final String lastLogin = getMetricValueAsString(metricMap, "LAST_LOGIN", "N/A");
        
        return new UsageSummary.UserMetrics(activeUsers, totalUsers, recentLogins, lastLogin);
    }

    private UsageSummary.SystemMetrics buildSystemMetrics(final Map<String, MetricValue> metricMap) {
        final long languages = getMetricValueAsLong(metricMap, "COUNT_LANGUAGES", 1L);
        final long workflowSchemes = getMetricValueAsLong(metricMap, "SCHEMES_COUNT", 0L);
        final long workflowSteps = getMetricValueAsLong(metricMap, "STEPS_COUNT", 0L);
        final long liveContainers = getMetricValueAsLong(metricMap, "COUNT_OF_LIVE_CONTAINERS", 0L);
        final long builderTemplates = getMetricValueAsLong(metricMap, "COUNT_OF_TEMPLATE_BUILDER_TEMPLATES", 0L);
        
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