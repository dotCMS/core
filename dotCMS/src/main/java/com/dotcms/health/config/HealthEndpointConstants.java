package com.dotcms.health.config;

import com.dotcms.management.config.InfrastructureConstants;

/**
 * Health service specific endpoint constants.
 * 
 * This class defines health-specific endpoints that build upon the shared
 * management infrastructure prefix. The health service is aware of the
 * management infrastructure, but the infrastructure is NOT aware of 
 * health-specific details.
 * 
 * Dependency Direction: HealthService -> Infrastructure (one-way)
 */
public final class HealthEndpointConstants {
    
    private HealthEndpointConstants() {
        throw new AssertionError("HealthEndpointConstants should not be instantiated");
    }
    
    /**
     * Health service endpoints under the shared management prefix.
     * 
     * These endpoints are managed by HealthProbeServlet and should be
     * mapped in web.xml to that servlet.
     */
    public static final class Endpoints {
        private Endpoints() {}
        
        // Path suffixes (without prefix) - eliminates magic strings
        public static final String LIVENESS_SUFFIX = "/livez";
        public static final String READINESS_SUFFIX = "/readyz";
        public static final String HEALTH_SUFFIX = "/health";
        
        // Full endpoint paths (with prefix)
        public static final String LIVENESS = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + LIVENESS_SUFFIX;
        public static final String READINESS = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + READINESS_SUFFIX;
        public static final String HEALTH = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + HEALTH_SUFFIX;
    }
    
    /**
     * Health service specific response constants.
     */
    public static final class Responses {
        private Responses() {}
        
        public static final String CONTENT_TYPE_JSON = "application/json";
        public static final String ALIVE_RESPONSE = "alive";
        public static final String UNHEALTHY_RESPONSE = "unhealthy";
        public static final String READY_RESPONSE = "ready";
        public static final String NOT_READY_RESPONSE = "not ready";
    }
    
    /**
     * Returns all health endpoints for testing/validation.
     * Used by health service tests and configuration validation.
     */
    public static String[] getAllHealthEndpoints() {
        return new String[] {
            Endpoints.LIVENESS,
            Endpoints.READINESS,
            Endpoints.HEALTH
        };
    }
} 