package com.dotcms.health.api;

import java.util.List;

/**
 * Provider interface for health checks that can be injected via CDI.
 * This allows other modules to contribute health checks without tight coupling.
 */
public interface HealthCheckProvider {
    
    /**
     * Returns a list of health checks provided by this provider.
     * 
     * @return list of health checks
     */
    List<HealthCheck> getHealthChecks();
    
    /**
     * Returns the name of this provider for logging/debugging purposes.
     * 
     * @return provider name
     */
    String getProviderName();
}