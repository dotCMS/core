package com.dotcms.health.api;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;

/**
 * Interface for health event management that supports both event-driven and polling modes.
 * 
 * Event-driven mode: Monitors underlying system events (connection pool changes, client failures)
 * and only performs health checks when state changes are detected.
 * 
 * Polling mode: Traditional fixed-interval health checking (fallback mode).
 */
public interface HealthEventManager {
    
    /**
     * Gets the name of the health check this manager handles
     */
    String getHealthCheckName();
    
    /**
     * Returns true if this manager supports event-driven monitoring
     */
    boolean supportsEventDriven();
    
    /**
     * Gets the current health status (cached from last event or check)
     */
    HealthStatus getCurrentStatus();
    
    /**
     * Gets the last known health check result
     */
    HealthCheckResult getLastResult();
    
    /**
     * Initialize the event manager (register listeners, start monitoring)
     */
    void initialize();
    
    /**
     * Shutdown the event manager (cleanup listeners, stop monitoring)
     */
    void shutdown();
    
    /**
     * Register a callback for when health status changes
     */
    void onHealthStatusChange(HealthStatusChangeCallback callback);
    
    /**
     * Callback interface for health status changes
     */
    @FunctionalInterface
    interface HealthStatusChangeCallback {
        void onStatusChange(String checkName, HealthStatus oldStatus, HealthStatus newStatus, String reason);
    }
} 