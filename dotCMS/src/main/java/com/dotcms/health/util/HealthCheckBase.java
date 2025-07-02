package com.dotcms.health.util;

import com.dotcms.health.api.HealthCheck;
import com.dotcms.health.config.HealthCheckConfig;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Base utility class providing common functionality for all health checks.
 * 
 * This class eliminates duplication by providing:
 * - Safety mode handling (PRODUCTION, MONITOR_MODE, DISABLED)
 * - Timing and measurement utilities
 * - Configuration reading with standardized naming
 * - Message formatting with conditional details
 * - Error handling patterns
 * 
 * Each health check remains decoupled by managing its own configuration
 * through the standard dotCMS Config class with check-specific property names.
 * 
 * Property Naming Convention:
 * health.check.{check-name}.mode = Safety mode (PRODUCTION, MONITOR_MODE, DISABLED)
 * health.check.{check-name}.timeout-ms = Operation timeout in milliseconds
 * health.check.{check-name}.{custom-property} = Check-specific properties
 */
public abstract class HealthCheckBase implements HealthCheck {
    
    
    /**
     * Result of a health check execution with timing information
     */
    protected static class CheckResult {
        public final boolean healthy;
        public final long durationMs;
        public final String details;
        public final Exception error;
        
        public CheckResult(boolean healthy, long durationMs, String details) {
            this.healthy = healthy;
            this.durationMs = durationMs;
            this.details = details;
            this.error = null;
        }
        
        public CheckResult(boolean healthy, long durationMs, String details, Exception error) {
            this.healthy = healthy;
            this.durationMs = durationMs;
            this.details = details;
            this.error = error;
        }
    }
    
    /**
     * Template method that handles all common functionality.
     * Subclasses only need to implement performCheck().
     */
    @Override
    public final HealthCheckResult check() {
        HealthCheckMode mode = getMode();
        
        // Handle disabled checks
        if (mode == HealthCheckMode.DISABLED) {
            return createDisabledResult();
        }
        
        long startTime = System.nanoTime();
        CheckResult result;
        
        try {
            result = performCheck();
        } catch (Exception e) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            result = new CheckResult(false, duration, "Check failed: " + e.getMessage(), e);
            Logger.warn(this, getName() + " health check failed: " + e.getMessage(), e);
        }
        
        HealthStatus originalStatus = result.healthy ? HealthStatus.UP : HealthStatus.DOWN;
        HealthStatus finalStatus = applySafetyMode(originalStatus, mode);
        
        // Monitor mode is considered "applied" whenever the check is running in MONITOR_MODE
        // This flag is used by the health state manager to indicate monitor mode presence in the system
        // Note: The tolerance manager uses isMonitorModeResult() for its own logic
        boolean monitorModeApplied = (mode == HealthCheckMode.MONITOR_MODE);
        
        String message = formatMessage(result, mode, finalStatus, monitorModeApplied);
        
        // Log safety mode conversions
        logSafetyModeConversion(originalStatus, finalStatus, mode);
        
        // Build structured data if available
        Map<String, Object> structuredData = buildStructuredData(result, originalStatus, finalStatus, mode);
        
        HealthCheckResult.Builder builder = HealthCheckResult.builder()
            .name(getName())
            .status(finalStatus)
            .message(message)
            .lastChecked(Instant.now())
            .durationMs(result.durationMs)
            .monitorModeApplied(monitorModeApplied);
        
        // Add structured data if available
        if (structuredData != null && !structuredData.isEmpty()) {
            builder.data(structuredData);
        }
            
        if (result.error != null && finalStatus == HealthStatus.DOWN) {
            builder.error(result.error.getMessage());
        }
        
        return builder.build();
    }
    
    /**
     * Subclasses implement this method to perform the actual health check.
     * No need to handle timing, safety modes, or error formatting.
     */
    protected abstract CheckResult performCheck() throws Exception;
    
    /**
     * Check if system shutdown is in progress.
     * Health checks can use this to avoid expensive operations during shutdown.
     */
    protected boolean isShutdownInProgress() {
        try {
            return com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted();
        } catch (Exception e) {
            // If we can't check shutdown status, assume not shutting down
            return false;
        }
    }
    
    /**
     * Returns whether system details should be included in health check results.
     * This is controlled by the global health.include.system-details property.
     */
    protected boolean shouldIncludeSystemDetails() {
        return HealthCheckConfig.INCLUDE_SYSTEM_DETAILS;
    }
    
    /**
     * Returns whether performance metrics should be included in health check results.
     * This is controlled by the global health.include.performance-metrics property.
     */
    protected boolean shouldIncludePerformanceMetrics() {
        return HealthCheckConfig.INCLUDE_PERFORMANCE_METRICS;
    }
    
    /**
     * Gets the safety mode for this health check from configuration.
     * Uses the naming convention: health.check.{getName()}.mode
     */
    protected HealthCheckMode getMode() {
        String modeString = Config.getStringProperty(
            "health.check." + getName() + ".mode", 
            getDefaultMode().name()
        );
        return HealthCheckConfig.parseMode(modeString);
    }
    
    /**
     * Returns the default mode for this health check.
     * Override this method to provide a different default mode.
     * 
     * @return The default HealthCheckMode for this health check
     */
    protected HealthCheckMode getDefaultMode() {
        return HealthCheckMode.PRODUCTION;
    }
    
    /**
     * Gets a configuration property specific to this health check.
     * Uses the naming convention: health.check.{getName()}.{property}
     */
    protected String getConfigProperty(String property, String defaultValue) {
        return Config.getStringProperty("health.check." + getName() + "." + property, defaultValue);
    }
    
    protected int getConfigProperty(String property, int defaultValue) {
        return Config.getIntProperty("health.check." + getName() + "." + property, defaultValue);
    }
    
    protected boolean getConfigProperty(String property, boolean defaultValue) {
        return Config.getBooleanProperty("health.check." + getName() + "." + property, defaultValue);
    }
    
    protected long getConfigProperty(String property, long defaultValue) {
        return Config.getLongProperty("health.check." + getName() + "." + property, defaultValue);
    }
    
    /**
     * Gets the timeout for this health check operations.
     * Uses the naming convention: health.check.{getName()}.timeout-ms
     */
    protected int getTimeoutMs() {
        return getConfigProperty("timeout-ms", 2000); // 2 second default
    }
    
    /**
     * Utility method for measuring execution time of operations.
     */
    protected CheckResult measureExecution(CheckExecution execution) {
        long startTime = System.nanoTime();
        try {
            String details = execution.execute();
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            return new CheckResult(true, duration, details);
        } catch (Exception e) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            return new CheckResult(false, duration, "Operation failed", e);
        }
    }
    
    @FunctionalInterface
    protected interface CheckExecution {
        String execute() throws Exception;
    }
    
    
    private HealthStatus applySafetyMode(HealthStatus originalStatus, HealthCheckMode mode) {
        switch (mode) {
            case MONITOR_MODE:
                // Monitor mode: DOWN -> DEGRADED to prevent K8s probe failures
                return originalStatus == HealthStatus.DOWN ? HealthStatus.DEGRADED : originalStatus;
            case PRODUCTION:
            case DISABLED:
            default:
                return originalStatus; // No modification
        }
    }
    
    private String formatMessage(CheckResult result, HealthCheckMode mode, HealthStatus finalStatus, boolean monitorModeApplied) {
        StringBuilder message = new StringBuilder();
        
        // Add mode information if not production and status wasn't converted by monitor mode
        // (avoid redundant "[MONITOR_MODE mode]" when the conversion message already indicates monitor mode)
        boolean statusConverted = (mode == HealthCheckMode.MONITOR_MODE && finalStatus == HealthStatus.DEGRADED && !result.healthy);
        if (mode != HealthCheckMode.PRODUCTION && !statusConverted) {
            message.append("[").append(mode.name()).append(" mode] ");
        }
        
        boolean includeDetails = shouldIncludeSystemDetails();
        
        if (!includeDetails) {
            return message.toString() + createBasicMessage(result.healthy, mode, finalStatus);
        }
        
        // Detailed message
        String baseMessage = result.details;
        if (finalStatus == HealthStatus.DEGRADED && !result.healthy) {
            baseMessage += " (converted to DEGRADED for MONITOR_MODE)";
        }
        
        message.append(baseMessage);
        
        // Add performance metrics if enabled
        boolean includeMetrics = shouldIncludePerformanceMetrics();
        if (includeMetrics && result.healthy) {
            message.append(String.format(" [%dms]", result.durationMs));
        }
        
        return message.toString();
    }
    
    private String createBasicMessage(boolean healthy, HealthCheckMode mode, HealthStatus finalStatus) {
        if (mode == HealthCheckMode.MONITOR_MODE) {
            return getName() + " monitoring active";
        } else if (healthy) {
            return getName() + " operational";
        } else if (finalStatus == HealthStatus.DEGRADED) {
            return getName() + " degraded";
        } else {
            return getName() + " not operational";
        }
    }
    
    private void logSafetyModeConversion(HealthStatus originalStatus, HealthStatus finalStatus, HealthCheckMode mode) {
        if (mode == HealthCheckMode.MONITOR_MODE && originalStatus == HealthStatus.DOWN && finalStatus == HealthStatus.DEGRADED) {
            Logger.warn(this, getName() + " health check: MONITOR_MODE - would normally be DOWN but converted to DEGRADED for safe deployment");
        }
    }
    
    private HealthCheckResult createDisabledResult() {
        return HealthCheckResult.builder()
            .name(getName())
            .status(HealthStatus.UP)
            .message(getName() + " health check disabled")
            .lastChecked(Instant.now())
            .durationMs(0L)
            .build();
    }
    
    /**
     * Determine if this health check should use event-driven monitoring.
     * Override this method to enable event-driven mode for specific health checks.
     * 
     * @return true if event-driven monitoring is supported and enabled
     */
    protected boolean supportsEventDriven() {
        String checkName = getName().toLowerCase();
        return Config.getBooleanProperty("health.check." + checkName + ".event-driven.enabled", false);
    }
    
    /**
     * Get the associated event manager if event-driven monitoring is enabled.
     * Override this method to provide a custom event manager.
     * 
     * @return HealthEventManager instance or null if not using event-driven mode
     */
    public com.dotcms.health.api.HealthEventManager getEventManager() {
        // Default implementation returns null - individual health checks can override
        return null;
    }
    
    /**
     * Check if this health check is currently using event-driven monitoring.
     * This affects how often the traditional check() method is called.
     */
    public final boolean isEventDriven() {
        if (!supportsEventDriven()) {
            return false;
        }
        
        com.dotcms.health.api.HealthEventManager eventManager = getEventManager();
        return eventManager != null && eventManager.supportsEventDriven();
    }
    
    /**
     * Get the current status from event manager if available, otherwise perform traditional check.
     * This allows the health framework to get cached results from event-driven monitoring.
     */
    public final HealthCheckResult getEventDrivenResult() {
        if (!isEventDriven()) {
            return null;
        }
        
        com.dotcms.health.api.HealthEventManager eventManager = getEventManager();
        if (eventManager != null) {
            HealthCheckResult cachedResult = eventManager.getLastResult();
            if (cachedResult != null) {
                Logger.debug(this, String.format(
                    "Using cached result from event manager for '%s': %s", 
                    getName(), cachedResult.status()
                ));
                return cachedResult;
            }
        }
        
        return null;
    }
    
    /**
     * Subclasses can override this method to provide structured data specific to their health check.
     * This data will be included in the health check result for machine parsing.
     * 
     * Focus on error-related information and threshold data rather than comprehensive metrics.
     * The checkName and basic metadata are already available at the health check level.
     * 
     * @param result the check result containing health check execution details
     * @param originalStatus the original status before safety mode processing
     * @param finalStatus the final status after safety mode processing
     * @param mode the current health check mode
     * @return a map of structured data, or null if no structured data is available
     */
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        // Default implementation only includes error information when relevant
        if (result.error != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("hasError", true);
            return data;
        }
        
        // Return null if no relevant structured data
        return null;
    }
} 