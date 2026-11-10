package com.dotcms.metrics.config;

import com.dotcms.management.config.InfrastructureConstants;

/**
 * Metrics service specific endpoint constants.
 * 
 * This class derives metrics endpoints from shared infrastructure routing constants and
 * defines the service's responses. Infrastructure does not depend on metrics implementations.
 * 
 * Dependency Direction: MetricsService -> Infrastructure (one-way)
 */
public final class MetricsEndpointConstants {
    
    private MetricsEndpointConstants() {
        throw new AssertionError("MetricsEndpointConstants should not be instantiated");
    }
    
    /**
     * Metrics service endpoints under the shared management prefix.
     * 
     * These endpoints are managed by ManagementMetricsServlet and should be
     * mapped in web.xml to that servlet.
     */
    public static final class Endpoints {
        private Endpoints() {}
        
        // Path suffixes (without prefix) - eliminates magic strings
        public static final String METRICS_SUFFIX = InfrastructureConstants.METRICS_SUFFIX;
        public static final String PROMETHEUS_SUFFIX = METRICS_SUFFIX + "/prometheus";
        
        // Full endpoint paths (with prefix)
        public static final String METRICS = InfrastructureConstants.METRICS_PATH;
        public static final String PROMETHEUS = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + PROMETHEUS_SUFFIX;
    }
    
    /**
     * Metrics service specific response constants.
     */
    public static final class Responses {
        private Responses() {}
        
        public static final String CONTENT_TYPE_PROMETHEUS = "text/plain; version=0.0.4; charset=utf-8";
        public static final String CHARSET_UTF8 = "UTF-8";
        public static final String METRICS_DISABLED_MESSAGE = "# Metrics collection is disabled\n";
        public static final String PROMETHEUS_DISABLED_MESSAGE = "# Prometheus metrics are disabled\n";
        public static final String SERVICE_UNAVAILABLE_MESSAGE = "# MetricsService not available\n";
        public static final String REGISTRY_NOT_CONFIGURED_MESSAGE = "# Prometheus registry not configured\n";
    }
    
    /**
     * Get all metrics endpoints for validation/testing.
     * 
     * @return Array of all metrics endpoints
     */
    public static String[] getAllMetricsEndpoints() {
        return new String[] {
            Endpoints.METRICS,
            Endpoints.PROMETHEUS
        };
    }
} 