package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Application-level health check that monitors JVM memory and core application state.
 * This check is safe for both liveness and readiness probes as it only monitors
 * internal application state without external dependencies.
 * 
 * Configuration Properties:
 * - health.check.application.mode = Safety mode (PRODUCTION, MONITOR_MODE, DISABLED)
 * - health.check.application.memory-critical-threshold = Critical memory threshold percentage (default: 90)
 * - health.check.application.memory-warning-threshold = Warning memory threshold percentage (default: 80)
 */
public class ApplicationHealthCheck extends HealthCheckBase {
    
    // Cached static values for performance
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    
    @Override
    protected CheckResult performCheck() throws Exception {
        // Use utility for memory status and perform checks
        return measureExecution(() -> {
            // Check memory usage for critical thresholds
            double usagePercent = getMemoryUsagePercent();
            int criticalThreshold = getConfigProperty("memory-critical-threshold", 90);
            int warningThreshold = getConfigProperty("memory-warning-threshold", 80);
            
            // Determine health status based on memory usage
            if (usagePercent >= criticalThreshold) {
                throw new Exception("Critical memory usage: " + String.format("%.1f%%", usagePercent));
            } else if (usagePercent >= warningThreshold) {
                return "Memory usage warning: " + getMemoryStatus();
            } else {
                return "Application healthy: " + getMemoryStatus();
            }
        });
    }
    
    /**
     * Gets memory usage percentage specific to this health check.
     * 
     * @return memory usage as percentage (0-100)
     */
    private double getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return (double) usedMemory / maxMemory * 100;
    }
    
    /**
     * Gets memory status information specific to this health check.
     * 
     * @return memory usage information
     */
    private String getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double usagePercent = (double) usedMemory / maxMemory * 100;
        
        return String.format("Memory usage: %.1f%% (%dMB used / %dMB max)", 
            usagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
    }
    
    @Override
    public String getName() {
        return "application";
    }
    
    @Override
    public int getOrder() {
        return 10; // Highest priority - core application check
    }
    
    /**
     * Safe for liveness - only checks internal JVM state, never external dependencies
     */
    @Override
    public boolean isLivenessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    /**
     * Essential for readiness - application must be healthy to serve traffic
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        HealthCheckMode mode = getMode();
        int criticalThreshold = getConfigProperty("memory-critical-threshold", 90);
        int warningThreshold = getConfigProperty("memory-warning-threshold", 80);
        
        return String.format("Monitors JVM memory usage (warn: %d%%, critical: %d%%) (Mode: %s)", 
            warningThreshold, criticalThreshold, mode.name());
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Get current memory information
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        double usagePercent = (double) usedMemory / maxMemory * 100;
        
        // Always include memory usage and thresholds as they're referenced in messages
        data.put("memoryUsagePercent", Math.round(usagePercent * 10.0) / 10.0);
        data.put("memoryUsedMB", usedMemory / 1024 / 1024);
        data.put("memoryMaxMB", maxMemory / 1024 / 1024);
        
        int criticalThreshold = getConfigProperty("memory-critical-threshold", 90);
        int warningThreshold = getConfigProperty("memory-warning-threshold", 80);
        data.put("warningThreshold", warningThreshold);
        data.put("criticalThreshold", criticalThreshold);
        
        // Include error type for memory-related failures
        if (result.error != null) {
            data.put("errorType", "memory_pressure");
        }
        
        return data;
    }
}