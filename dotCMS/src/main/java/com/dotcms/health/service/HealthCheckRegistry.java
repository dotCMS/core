package com.dotcms.health.service;

import com.dotcms.health.api.HealthCheck;
import com.dotcms.health.api.HealthCheckProvider;
import com.dotcms.health.checks.CdiInitializationHealthCheck;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * CDI-based registry for health checks that automatically discovers and registers
 * health checks and health check providers via CDI.
 * 
 * This registry integrates with the HealthStateManager to provide seamless
 * integration between manual registration and CDI-based discovery.
 * 
 * The registry discovers:
 * 1. Direct HealthCheck CDI beans
 * 2. HealthCheckProvider CDI beans (which provide multiple health checks)
 * 
 * The registry also coordinates with CdiInitializationHealthCheck to ensure
 * readiness probes return DOWN until CDI is fully initialized and all
 * CDI health check beans are discovered and registered.
 */
@ApplicationScoped
public class HealthCheckRegistry {
    
    @Inject
    private Instance<HealthCheck> healthCheckInstances;
    
    @Inject
    private Instance<HealthCheckProvider> healthCheckProviders;
    
    private HealthStateManager stateManager;
    private CdiInitializationHealthCheck cdiInitCheck;
    private boolean initialized = false;
    private int discoveredCdiHealthChecks = 0;
    
    /**
     * Sets the health state manager to register discovered health checks with
     */
    public synchronized void setStateManager(HealthStateManager stateManager) {
        this.stateManager = stateManager;
        if (!initialized) {
            discoverAndRegisterHealthChecks();
            initialized = true;
        }
    }
    
    /**
     * Sets the CDI initialization health check to coordinate initialization status
     */
    public void setCdiInitializationHealthCheck(CdiInitializationHealthCheck cdiInitCheck) {
        this.cdiInitCheck = cdiInitCheck;
    }
    
    /**
     * Discovers all CDI health check beans and providers, then registers them with the state manager
     */
    private void discoverAndRegisterHealthChecks() {
        if (stateManager == null) {
            Logger.warn(this, "HealthStateManager not set, cannot register CDI health checks");
            return;
        }
        
        try {
            List<HealthCheck> discoveredChecks = new ArrayList<>();
            
            // Discover direct HealthCheck CDI beans
            for (HealthCheck healthCheck : healthCheckInstances) {
                discoveredChecks.add(healthCheck);
                stateManager.registerHealthCheck(healthCheck);
                Logger.info(this, "Registered CDI health check: " + healthCheck.getName());
            }
            
            // Discover HealthCheckProvider CDI beans and register their health checks
            for (HealthCheckProvider provider : healthCheckProviders) {
                try {
                    List<HealthCheck> providerChecks = provider.getHealthChecks();
                    if (providerChecks != null) {
                        for (HealthCheck healthCheck : providerChecks) {
                            discoveredChecks.add(healthCheck);
                            stateManager.registerHealthCheck(healthCheck);
                            Logger.info(this, "Registered health check '" + healthCheck.getName() + 
                                "' from provider: " + provider.getProviderName());
                        }
                    }
                    Logger.info(this, "Processed health check provider: " + provider.getProviderName() + 
                        " (provided " + (providerChecks != null ? providerChecks.size() : 0) + " checks)");
                } catch (Exception e) {
                    Logger.error(this, "Failed to register health checks from provider: " + 
                        provider.getProviderName(), e);
                }
            }
            
            discoveredCdiHealthChecks = discoveredChecks.size();
            
            // Update CDI initialization check with the count of discovered checks
            if (cdiInitCheck != null) {
                cdiInitCheck.setCdiHealthCheckCount(discoveredCdiHealthChecks);
                Logger.info(this, "Updated CDI initialization check with " + discoveredCdiHealthChecks + " discovered checks");
            }
            
            Logger.info(this, "CDI health check discovery completed - discovered and registered " + 
                discoveredCdiHealthChecks + " CDI health checks");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to discover CDI health checks", e);
            
            // Notify CDI initialization check of the failure
            if (cdiInitCheck != null) {
                cdiInitCheck.setCdiInitializationStatus(false, "CDI health check discovery failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Manually register a health check (for programmatic registration)
     */
    public void registerHealthCheck(HealthCheck healthCheck) {
        if (stateManager != null) {
            stateManager.registerHealthCheck(healthCheck);
            Logger.info(this, "Manually registered health check: " + healthCheck.getName());
        } else {
            Logger.warn(this, "HealthStateManager not available, cannot register health check: " + 
                healthCheck.getName());
        }
    }
    
    /**
     * Get the current state manager instance
     */
    public HealthStateManager getStateManager() {
        return stateManager;
    }
    
    /**
     * Check if CDI health check discovery has been completed
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get the number of CDI health checks that were discovered
     */
    public int getDiscoveredCdiHealthCheckCount() {
        return discoveredCdiHealthChecks;
    }
} 