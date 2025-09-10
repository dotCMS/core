package com.dotcms.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility class for infrastructure endpoint detection and filtering.
 * 
 * SIMPLIFIED ARCHITECTURE: Only management endpoints are supported.
 * All infrastructure monitoring is consolidated under /dotmgt/* for security and consistency.
 * 
 * Infrastructure endpoints:
 * - /dotmgt/livez - Kubernetes liveness probe
 * - /dotmgt/readyz - Kubernetes readiness probe  
 * - /dotmgt/health - Detailed health status
 */
public final class InfrastructureEndpointUtil {
    
    /**
     * Management endpoint prefix for all infrastructure monitoring.
     */
    private static final String MANAGEMENT_ENDPOINT_PREFIX = "/dotmgt";
    
    /**
     * Private constructor to prevent instantiation
     */
    private InfrastructureEndpointUtil() {
        // Utility class
    }
    
    /**
     * Check if the given servlet path is an infrastructure endpoint.
     * All infrastructure endpoints are now under /dotmgt/* for consistency.
     * 
     * @param servletPath The servlet path from HttpServletRequest.getServletPath()
     * @return true if this is an infrastructure endpoint, false otherwise
     */
    public static boolean isInfrastructureEndpoint(String servletPath) {
        return isManagementEndpoint(servletPath);
    }
    
    /**
     * Check if the given servlet path is a management endpoint.
     * 
     * @param servletPath The servlet path from HttpServletRequest.getServletPath()
     * @return true if this is a management endpoint, false otherwise
     */
    public static boolean isManagementEndpoint(String servletPath) {
        return servletPath != null && servletPath.startsWith(MANAGEMENT_ENDPOINT_PREFIX);
    }
    
    /**
     * Legacy method - now always returns false since standalone health endpoints are deprecated.
     * All health functionality is available through /dotmgt/* endpoints.
     * 
     * @deprecated Use /dotmgt/livez and /dotmgt/readyz instead of standalone endpoints
     * @param servletPath The servlet path from HttpServletRequest.getServletPath()
     * @return always false
     */
    @Deprecated
    public static boolean isHealthCheckEndpoint(String servletPath) {
        return false; // All health checks are now under /dotmgt/*
    }
    
    /**
     * Get the management endpoint prefix.
     * 
     * @return the management endpoint prefix
     */
    public static String getManagementEndpointPrefix() {
        return MANAGEMENT_ENDPOINT_PREFIX;
    }
} 