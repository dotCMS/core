package com.dotcms.rest.api.v1.metrics;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.metrics.MetricsConfig;
import com.dotcms.metrics.MetricsService;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.util.Logger;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * REST resource for exposing metrics in various formats.
 * 
 * This resource provides endpoints for:
 * - Prometheus metrics scraping (/metrics)
 * - Health check integration with metrics status
 * 
 * The Prometheus endpoint is designed to be consumed by monitoring systems
 * and typically does not require authentication to allow external scraping.
 */
@Path("/v1/metrics")
public class MetricsResource {
    
    private static final String CLASS_NAME = MetricsResource.class.getSimpleName();
    
    /**
     * Expose Prometheus-formatted metrics for scraping.
     * 
     * This endpoint returns metrics in the Prometheus text exposition format
     * which can be scraped by Prometheus or compatible monitoring systems.
     * 
     * @param request HTTP request context
     * @param response HTTP response context
     * @return Response containing Prometheus-formatted metrics or error
     */
    @GET
    @Path("/prometheus")
    @Produces(MediaType.TEXT_PLAIN)
    @NoCache
    public Response getPrometheusMetrics(@Context HttpServletRequest request, 
                                       @Context HttpServletResponse response) {
        
        try {
            // Check if metrics are enabled
            if (!MetricsConfig.ENABLED) {
                Logger.debug(this, "Metrics collection is disabled");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("# Metrics collection is disabled\n")
                    .build();
            }
            
            // Check if Prometheus is enabled
            if (!MetricsConfig.PROMETHEUS_ENABLED) {
                Logger.debug(this, "Prometheus metrics are disabled");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("# Prometheus metrics are disabled\n")
                    .build();
            }
            
            // Get the metrics service
            Optional<MetricsService> metricsServiceOpt = CDIUtils.getBean(MetricsService.class);
            if (!metricsServiceOpt.isPresent()) {
                Logger.error(this, "MetricsService not available");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("# MetricsService not available\n")
                    .build();
            }
            
            MetricsService metricsService = metricsServiceOpt.get();
            PrometheusMeterRegistry prometheusRegistry = metricsService.getPrometheusRegistry();
            
            if (prometheusRegistry == null) {
                Logger.error(this, "Prometheus registry not configured");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("# Prometheus registry not configured\n")
                    .build();
            }
            
            // Get the metrics in Prometheus format
            String metricsContent = prometheusRegistry.scrape();
            
            Logger.debug(this, "Served Prometheus metrics (" + metricsContent.length() + " characters)");
            
            return Response.ok(metricsContent)
                .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                .build();
                
        } catch (Exception e) {
            Logger.error(this, "Error serving Prometheus metrics: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("# Error serving metrics: " + e.getMessage() + "\n")
                .build();
        }
    }
    
    /**
     * Get the status of the metrics system.
     * 
     * This endpoint provides information about the current state of the
     * metrics collection system, including enabled registries and configuration.
     * 
     * @param request HTTP request context
     * @param response HTTP response context
     * @return Response containing metrics system status
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getMetricsStatus(@Context HttpServletRequest request, 
                                   @Context HttpServletResponse response) {
        
        try {
            MetricsStatusView status = new MetricsStatusView();
            
            // Basic configuration
            status.setEnabled(MetricsConfig.ENABLED);
            status.setPrometheusEnabled(MetricsConfig.PROMETHEUS_ENABLED);
            status.setJmxEnabled(MetricsConfig.JMX_ENABLED);
            status.setPrometheusEndpoint(MetricsConfig.PROMETHEUS_ENDPOINT);
            status.setJmxDomain(MetricsConfig.JMX_DOMAIN);
            status.setMetricPrefix(MetricsConfig.METRIC_PREFIX);
            
            // Feature flags
            status.setJvmMetricsEnabled(MetricsConfig.JVM_METRICS_ENABLED);
            status.setSystemMetricsEnabled(MetricsConfig.SYSTEM_METRICS_ENABLED);
            status.setApplicationMetricsEnabled(MetricsConfig.APPLICATION_METRICS_ENABLED);
            status.setDatabaseMetricsEnabled(MetricsConfig.DATABASE_METRICS_ENABLED);
            status.setCacheMetricsEnabled(MetricsConfig.CACHE_METRICS_ENABLED);
            status.setHttpMetricsEnabled(MetricsConfig.HTTP_METRICS_ENABLED);
            
            // Runtime status
            Optional<MetricsService> metricsServiceOpt = CDIUtils.getBean(MetricsService.class);
            if (metricsServiceOpt.isPresent()) {
                MetricsService metricsService = metricsServiceOpt.get();
                status.setServiceInitialized(metricsService.isEnabled());
                status.setPrometheusRegistryActive(metricsService.getPrometheusRegistry() != null);
                status.setJmxRegistryActive(metricsService.getJmxRegistry() != null);
            } else {
                status.setServiceInitialized(false);
                status.setPrometheusRegistryActive(false);
                status.setJmxRegistryActive(false);
            }
            
            return Response.ok(status).build();
            
        } catch (Exception e) {
            Logger.error(this, "Error getting metrics status: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
    
    /**
     * View object for metrics status response.
     */
    public static class MetricsStatusView {
        private boolean enabled;
        private boolean prometheusEnabled;
        private boolean jmxEnabled;
        private String prometheusEndpoint;
        private String jmxDomain;
        private String metricPrefix;
        private boolean jvmMetricsEnabled;
        private boolean systemMetricsEnabled;
        private boolean applicationMetricsEnabled;
        private boolean databaseMetricsEnabled;
        private boolean cacheMetricsEnabled;
        private boolean httpMetricsEnabled;
        private boolean serviceInitialized;
        private boolean prometheusRegistryActive;
        private boolean jmxRegistryActive;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean isPrometheusEnabled() { return prometheusEnabled; }
        public void setPrometheusEnabled(boolean prometheusEnabled) { this.prometheusEnabled = prometheusEnabled; }
        
        public boolean isJmxEnabled() { return jmxEnabled; }
        public void setJmxEnabled(boolean jmxEnabled) { this.jmxEnabled = jmxEnabled; }
        
        public String getPrometheusEndpoint() { return prometheusEndpoint; }
        public void setPrometheusEndpoint(String prometheusEndpoint) { this.prometheusEndpoint = prometheusEndpoint; }
        
        public String getJmxDomain() { return jmxDomain; }
        public void setJmxDomain(String jmxDomain) { this.jmxDomain = jmxDomain; }
        
        public String getMetricPrefix() { return metricPrefix; }
        public void setMetricPrefix(String metricPrefix) { this.metricPrefix = metricPrefix; }
        
        public boolean isJvmMetricsEnabled() { return jvmMetricsEnabled; }
        public void setJvmMetricsEnabled(boolean jvmMetricsEnabled) { this.jvmMetricsEnabled = jvmMetricsEnabled; }
        
        public boolean isSystemMetricsEnabled() { return systemMetricsEnabled; }
        public void setSystemMetricsEnabled(boolean systemMetricsEnabled) { this.systemMetricsEnabled = systemMetricsEnabled; }
        
        public boolean isApplicationMetricsEnabled() { return applicationMetricsEnabled; }
        public void setApplicationMetricsEnabled(boolean applicationMetricsEnabled) { this.applicationMetricsEnabled = applicationMetricsEnabled; }
        
        public boolean isDatabaseMetricsEnabled() { return databaseMetricsEnabled; }
        public void setDatabaseMetricsEnabled(boolean databaseMetricsEnabled) { this.databaseMetricsEnabled = databaseMetricsEnabled; }
        
        public boolean isCacheMetricsEnabled() { return cacheMetricsEnabled; }
        public void setCacheMetricsEnabled(boolean cacheMetricsEnabled) { this.cacheMetricsEnabled = cacheMetricsEnabled; }
        
        public boolean isHttpMetricsEnabled() { return httpMetricsEnabled; }
        public void setHttpMetricsEnabled(boolean httpMetricsEnabled) { this.httpMetricsEnabled = httpMetricsEnabled; }
        
        public boolean isServiceInitialized() { return serviceInitialized; }
        public void setServiceInitialized(boolean serviceInitialized) { this.serviceInitialized = serviceInitialized; }
        
        public boolean isPrometheusRegistryActive() { return prometheusRegistryActive; }
        public void setPrometheusRegistryActive(boolean prometheusRegistryActive) { this.prometheusRegistryActive = prometheusRegistryActive; }
        
        public boolean isJmxRegistryActive() { return jmxRegistryActive; }
        public void setJmxRegistryActive(boolean jmxRegistryActive) { this.jmxRegistryActive = jmxRegistryActive; }
    }
}