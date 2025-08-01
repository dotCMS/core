package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.service.HealthCheckRegistry;
import com.dotcms.health.util.HealthCheckBase;

import javax.enterprise.inject.spi.CDI;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check that monitors CDI container initialization status.
 * This check is CRITICAL for readiness probes as the application cannot serve
 * traffic properly until CDI is fully initialized and all CDI health check beans
 * are discovered and registered.
 * 
 * This check is NOT included in liveness probes since CDI initialization
 * problems should not trigger pod restarts - they should only prevent
 * the pod from receiving traffic until CDI is ready.
 * 
 * Configuration Properties:
 * - health.check.cdi-initialization.mode = Safety mode (PRODUCTION, DEGRADED_SAFE, MONITORING_ONLY, DISABLED)
 * - health.check.cdi-initialization.timeout-ms = CDI check timeout (default: 2000ms)
 */
public class CdiInitializationHealthCheck extends HealthCheckBase {
    
    private volatile boolean cdiFullyInitialized = false;
    private volatile int discoveredCdiHealthChecks = 0;
    private volatile String initializationError = null;
    
    @Override
    protected CheckResult performCheck() throws Exception {
        return measureExecution(() -> {
            // Check CDI container status
            CdiStatus cdiStatus = checkCdiStatus();
            
            if (!cdiStatus.isReady) {
                throw new Exception("CDI not ready: " + cdiStatus.message);
            }
            
            // Build success message
            String baseMessage = "CDI container fully initialized";
            if (discoveredCdiHealthChecks > 0) {
                baseMessage += String.format(" (%d CDI health checks discovered)", discoveredCdiHealthChecks);
            }
            
            return baseMessage;
        });
    }
    
    private CdiStatus checkCdiStatus() {
        try {
            // Check if CDI container is available
            CDI<Object> cdi = CDI.current();
            if (cdi == null) {
                return new CdiStatus(false, "CDI container not available", null);
            }
            
            // Try to get the health check registry
            HealthCheckRegistry registry = null;
            try {
                registry = cdi.select(HealthCheckRegistry.class).get();
            } catch (Exception e) {
                return new CdiStatus(false, "HealthCheckRegistry not available", e.getMessage());
            }
            
            if (registry == null) {
                return new CdiStatus(false, "HealthCheckRegistry bean not found", null);
            }
            
            // Check if the registry has been properly initialized with state manager
            if (registry.getStateManager() == null) {
                return new CdiStatus(false, "HealthCheckRegistry not yet initialized", null);
            }
            
            // CDI is ready - cache the status
            cdiFullyInitialized = true;
            return new CdiStatus(true, "CDI fully initialized", null);
            
        } catch (IllegalStateException e) {
            // CDI container not ready yet
            return new CdiStatus(false, "CDI container not ready", e.getMessage());
        } catch (Exception e) {
            // Other CDI-related errors
            return new CdiStatus(false, "CDI initialization error", e.getMessage());
        }
    }
    
    /**
     * Called by HealthCheckRegistry when CDI health checks are discovered
     */
    public void setCdiHealthCheckCount(int count) {
        this.discoveredCdiHealthChecks = count;
    }
    
    /**
     * Manual override for testing or when CDI initialization is known to have failed
     */
    public void setCdiInitializationStatus(boolean initialized, String error) {
        this.cdiFullyInitialized = initialized;
        this.initializationError = error;
    }
    
    @Override
    public String getName() {
        return "cdi-initialization";
    }
    
    @Override
    public int getOrder() {
        return 5; // Very high priority for readiness
    }
    
    /**
     * NOT safe for liveness - CDI initialization issues should not trigger restarts.
     * The JVM and servlet container can be alive and responsive even if CDI isn't ready yet.
     */
    @Override
    public boolean isLivenessCheck() {
        return false;
    }
    
    /**
     * CRITICAL for readiness - application cannot serve traffic properly without CDI.
     * Most dotCMS functionality depends on CDI beans being available.
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        HealthCheckMode mode = getMode();
        return String.format("Monitors CDI container initialization and health check bean discovery (Mode: %s)", 
            mode.name());
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Include CDI initialization status
        data.put("cdiInitialized", cdiFullyInitialized);
        
        // Include discovered CDI health check count if available
        if (discoveredCdiHealthChecks > 0) {
            data.put("discoveredHealthChecks", discoveredCdiHealthChecks);
        }
        
        // Include initialization timing since it's mentioned in messages
        if (result.durationMs > 0) {
            data.put("initializationTimeMs", result.durationMs);
        }
        
        // Include error information if available
        if (initializationError != null) {
            data.put("initializationError", initializationError);
        }
        
        // Include error type for CDI-related failures
        if (result.error != null) {
            data.put("errorType", "cdi_initialization");
        }
        
        return data;
    }
    
    /**
     * Helper class to encapsulate CDI status information
     */
    private static class CdiStatus {
        final boolean isReady;
        final String message;
        final String error;
        
        CdiStatus(boolean isReady, String message, String error) {
            this.isReady = isReady;
            this.message = message;
            this.error = error;
        }
    }
} 