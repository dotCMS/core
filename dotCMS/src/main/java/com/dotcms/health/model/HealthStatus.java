package com.dotcms.health.model;

/**
 * Enumeration representing the health status of a check
 */
public enum HealthStatus {
    /**
     * The service/component is healthy and functioning normally
     */
    UP,
    
    /**
     * The service/component is experiencing degraded performance or non-critical issues
     * but is still functional. This status does NOT trigger liveness or readiness probe failures,
     * allowing monitoring/alerting without causing pod restarts or load balancer removal.
     * 
     * Use cases:
     * - Performance degradation (slow but working)
     * - Resource usage warnings (high but not critical)
     * - Testing new health checks safely
     * - Early warning conditions
     */
    DEGRADED,
    
    /**
     * The service/component is not healthy or experiencing critical issues
     * that require intervention. This status WILL trigger liveness/readiness failures.
     */
    DOWN,
    
    /**
     * The health status is unknown (e.g., check hasn't run yet or failed to determine status).
     * This status does NOT trigger liveness or readiness probe failures.
     */
    UNKNOWN
}