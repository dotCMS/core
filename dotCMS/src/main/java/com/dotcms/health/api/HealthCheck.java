package com.dotcms.health.api;

import com.dotcms.health.model.HealthCheckResult;

/**
 * Interface for health check implementations in dotCMS.
 * 
 * Health checks can be categorized for different Kubernetes probe types:
 * - LIVENESS: Core application health only (failure triggers pod restart)
 * - READINESS: Application + dependencies (failure removes from load balancer)
 * 
 * Guidelines:
 * - Liveness checks should NEVER check external dependencies
 * - Readiness checks can safely check external dependencies
 * - A health check can be both liveness and readiness
 */
public interface HealthCheck {
    
    /**
     * Performs the health check and returns the result.
     * This method should be fast and non-blocking.
     * 
     * @return HealthCheckResult with status and details
     */
    HealthCheckResult check();
    
    /**
     * Returns the unique name of this health check.
     * Used for identification and deduplication.
     * 
     * @return unique health check name
     */
    String getName();
    
    /**
     * Returns the execution order for this health check.
     * Lower numbers execute first.
     * 
     * @return execution order (default: 100)
     */
    default int getOrder() {
        return 100;
    }
    
    /**
     * Indicates if this health check is safe for Kubernetes liveness probes.
     * 
     * Liveness checks should ONLY verify core application health:
     * - JVM responsiveness
     * - Memory availability  
     * - Thread health
     * - Servlet container status
     * 
     * NEVER check external dependencies in liveness probes as this can cause
     * cascade failures across the cluster.
     * 
     * @return true if safe for liveness probes, false otherwise
     */
    default boolean isLivenessCheck() {
        return false; // Safe default - assume it checks dependencies
    }
    
    /**
     * Indicates if this health check should be included in readiness probes.
     * 
     * Readiness checks can safely include:
     * - All liveness checks
     * - Database connectivity
     * - Cache availability
     * - External service dependencies
     * - Initialization status
     * 
     * @return true if should be included in readiness probes, false otherwise
     */
    default boolean isReadinessCheck() {
        return true; // Most checks are readiness checks
    }
    
    /**
     * Returns a description of what this health check verifies.
     * Used for documentation and monitoring.
     * 
     * @return description of the health check
     */
    default String getDescription() {
        return "Health check for " + getName();
    }
}