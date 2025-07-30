package com.dotcms.health.config;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * Global configuration for the dotCMS health check system.
 * 
 * This class provides centralized access to system-wide health check settings and
 * utility methods for health check configuration. Individual health checks should
 * manage their own specific configuration using the standard dotCMS Config class
 * with the naming convention: health.check.{check-name}.{property}
 * 
 * Key Features:
 * - Global health system settings (logging, metrics, intervals)
 * - Startup phase configuration (grace periods, stability thresholds)
 * - Failure tolerance settings (circuit breaker behavior)
 * - Health check mode management (PRODUCTION, MONITOR_MODE, DISABLED)
 * 
 * Usage:
 * 1. For global settings: Use the constants directly
 *    Example: if (HealthCheckConfig.INCLUDE_SYSTEM_DETAILS) { ... }
 * 
 * 2. For health check modes: Use getMode() in HealthCheckBase
 *    Example: HealthCheckMode mode = getMode(); // In health check class
 * 
 * 3. For check-specific settings: Use Config directly
 *    Example: Config.getIntProperty("health.check.database.timeout-ms", 2000)
 * 
 * Note: This class is designed to be extended through configuration properties
 * rather than code changes. New health checks should follow the established
 * naming conventions and patterns.
 */
public final class HealthCheckConfig {
    
    // ====================================================================
    // GLOBAL HEALTH SYSTEM SETTINGS
    // ====================================================================
    
    /**
     * Global health system settings
     */
    public static final boolean INCLUDE_SYSTEM_DETAILS = 
        Config.getBooleanProperty("health.include.system-details", true);
        
    public static final boolean INCLUDE_PERFORMANCE_METRICS = 
        Config.getBooleanProperty("health.include.performance-metrics", true);
        
    public static final int CHECK_INTERVAL_SECONDS = 
        Config.getIntProperty("health.check.interval-seconds", 30);
        
    public static final int THREAD_POOL_SIZE = 
        Config.getIntProperty("health.thread-pool.size", 5);
        
    public static final int THREAD_POOL_QUEUE_SIZE = 
        Config.getIntProperty("health.thread-pool.queue-size", 100);
    
    /**
     * Global framework timeout for all health checks (in milliseconds).
     * Individual checks can override this with their own timeout.
     * Default: 10000 (10 seconds) - optimized for production responsiveness
     */
    public static final int FRAMEWORK_TIMEOUT_MS = 
        Config.getIntProperty("health.framework.timeout-ms", 10000);
    
    // ====================================================================
    // STARTUP PHASE CONFIGURATION
    // ====================================================================
    
    /**
     * How long to consider the system "starting up" for logging context.
     * Reduce this for faster operational transition.
     * Default: 5 minutes
     */
    public static final int STARTUP_GRACE_PERIOD_MINUTES = 
        Config.getIntProperty("health.startup.grace.period.minutes", 5);
    
    /**
     * Number of successful checks required to consider the system stable.
     * Reduce this for faster operational transition.
     * Default: 3
     */
    public static final int STABLE_OPERATION_THRESHOLD = 
        Config.getIntProperty("health.stable.operation.threshold", 3);
    
    /**
     * Minimum startup time before allowing early exit due to stability.
     * Prevents premature operational transition during very fast startups.
     * Default: 30 seconds
     */
    public static final int STARTUP_MINIMUM_SECONDS = 
        Config.getIntProperty("health.startup.minimum.seconds", 30);
    
    // ====================================================================
    // FAILURE TOLERANCE CONFIGURATION
    // ====================================================================
    
    /**
     * Whether to enable failure tolerance for health checks.
     * When true, allows some failures before marking a check as failed.
     * Default: true
     */
    public static final boolean TOLERANCE_ENABLED = 
        Config.getBooleanProperty("health.tolerance.enabled", true);
    
    /**
     * How long to tolerate failures in readiness checks (in minutes).
     * Default: 1 minute (optimized for production responsiveness)
     * Note: Can also be configured in seconds via health.tolerance.readiness.seconds
     */
    public static final int READINESS_TOLERANCE_MINUTES = 
        Config.getIntProperty("health.tolerance.readiness.minutes", 1);
    
    /**
     * How long to tolerate failures in liveness checks (in minutes).
     * Default: 2 minutes (optimized for production responsiveness)
     */
    public static final int LIVENESS_TOLERANCE_MINUTES = 
        Config.getIntProperty("health.tolerance.liveness.minutes", 2);
    
    /**
     * Maximum number of consecutive failures allowed before failing.
     * Default: 3 (optimized for production responsiveness)
     */
    public static final int MAX_CONSECUTIVE_FAILURES = 
        Config.getIntProperty("health.tolerance.max.consecutive.failures", 3);
    
    /**
     * Health check operating modes.
     * 
     * PRODUCTION: Standard operation, fails on issues
     * MONITOR_MODE: Logs issues but doesn't fail (safe for deployment)
     * DISABLED: Check is not performed
     */
    public enum HealthCheckMode {
        PRODUCTION,    // Standard operation, fails on issues
        MONITOR_MODE,  // Logs issues but doesn't fail (safe for deployment)
        DISABLED       // Check is not performed
    }
    
    /**
     * Get the operating mode for a health check.
     * 
     * @param checkName The name of the health check (e.g., "database", "cache")
     * @param defaultMode The default mode to use if not configured
     * @return The configured or default health check mode
     * 
     * Example:
     * HealthCheckMode mode = HealthCheckConfig.getMode("database", HealthCheckMode.MONITOR_MODE);
     */
    public static HealthCheckMode getMode(String checkName, HealthCheckMode defaultMode) {
        String modeString = Config.getStringProperty("health.check." + checkName + ".mode", defaultMode.name());
        return parseMode(modeString);
    }
    
    /**
     * Parse mode string to HealthCheckMode enum with proper error handling.
     * 
     * @param modeString The mode string to parse (case-insensitive)
     * @return The parsed HealthCheckMode, or PRODUCTION if invalid
     * 
     * Example:
     * HealthCheckMode mode = HealthCheckConfig.parseMode("MONITOR_MODE");
     */
    public static HealthCheckMode parseMode(String modeString) {
        if (modeString == null || modeString.trim().isEmpty()) {
            return HealthCheckMode.PRODUCTION;
        }
        
        try {
            return HealthCheckMode.valueOf(modeString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log warning and return default
            Logger.warn(HealthCheckConfig.class, "Invalid health check mode '" + modeString + 
                "', defaulting to PRODUCTION. Valid modes: " + java.util.Arrays.toString(HealthCheckMode.values()));
            return HealthCheckMode.PRODUCTION;
        }
    }
} 