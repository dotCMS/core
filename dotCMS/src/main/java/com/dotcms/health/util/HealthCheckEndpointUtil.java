package com.dotcms.health.util;

import java.util.Set;

/**
 * Utility class for health check endpoint detection and filtering.
 * Centralizes the logic for identifying health check endpoints to avoid duplication
 * across multiple servlet filters.
 * 
 * This ensures all filters consistently bypass health check endpoints to prevent
 * database access during Kubernetes probe requests.
 * 
 * Note: /healthz endpoint removed as it was deprecated since Kubernetes v1.16.
 * Current best practice is to use /livez and /readyz endpoints.
 */
public final class HealthCheckEndpointUtil {
    
    /**
     * Set of all health check endpoint paths that should bypass servlet filters
     */
    private static final Set<String> HEALTH_CHECK_ENDPOINTS = Set.of(
        "/api/v1/health",
        "/livez",
        "/readyz"
    );
    
    /**
     * Private constructor to prevent instantiation
     */
    private HealthCheckEndpointUtil() {
        // Utility class
    }
    
    /**
     * Check if the given servlet path is a health check endpoint that should bypass filters.
     * 
     * @param servletPath The servlet path from HttpServletRequest.getServletPath()
     * @return true if this is a health check endpoint, false otherwise
     */
    public static boolean isHealthCheckEndpoint(String servletPath) {
        return servletPath != null && HEALTH_CHECK_ENDPOINTS.contains(servletPath);
    }
    
    /**
     * Get all health check endpoint paths (for debugging/logging purposes)
     * 
     * @return immutable set of health check endpoint paths
     */
    public static Set<String> getHealthCheckEndpoints() {
        return HEALTH_CHECK_ENDPOINTS;
    }
} 