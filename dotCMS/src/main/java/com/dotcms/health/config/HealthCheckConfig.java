package com.dotcms.health.config;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * Configuration for global health check system settings only.
 * 
 * Individual health checks now manage their own configuration through the standard
 * dotCMS Config class using the naming convention: health.check.{check-name}.{property}
 * 
 * This approach eliminates tight coupling between health checks and provides:
 * - Decoupled configuration management
 * - Each health check is self-contained
 * - Easy to add new health checks without modifying this class
 * - Follows dotCMS configuration conventions
 * 
 * Global health system configuration only:
 * - health.include.system-details: Whether to include detailed system information
 * - health.include.performance-metrics: Whether to include performance metrics
 * - health.interval-seconds: How often to run health checks
 * - health.thread-pool-size: Thread pool size for background health checks
 */
public final class HealthCheckConfig {
    
    // Global Health System Settings
    public static final boolean INCLUDE_SYSTEM_DETAILS = 
        Config.getBooleanProperty("health.include.system-details", true);
    
    public static final boolean INCLUDE_PERFORMANCE_METRICS = 
        Config.getBooleanProperty("health.include.performance-metrics", true);
    
    public static final int CHECK_INTERVAL_SECONDS = 
        Config.getIntProperty("health.interval-seconds", 30);
    
    public static final int THREAD_POOL_SIZE = 
        Config.getIntProperty("health.thread-pool-size", 2);
    
    // Cache test key constant
    public static final String CACHE_TEST_KEY = "health.check.cache.test.key";
    
    // Database health check configuration
    public static final int DATABASE_RETRY_COUNT = 
        Config.getIntProperty("health.check.database.retry-count", 3);
    
    // Thread health check configuration
    public static final boolean THREAD_DEADLOCK_DETECTION = 
        Config.getBooleanProperty("health.check.threads.deadlock-detection", true);
    
    public static final int THREAD_POOL_THRESHOLD_MULTIPLIER = 
        Config.getIntProperty("health.check.threads.pool-threshold-multiplier", 4);
    
    // Memory threshold configuration
    public static final int MEMORY_CRITICAL_THRESHOLD_PERCENT = 
        Config.getIntProperty("health.check.memory.critical-threshold-percent", 90);
    
    public static final int MEMORY_WARNING_THRESHOLD_PERCENT = 
        Config.getIntProperty("health.check.memory.warning-threshold-percent", 80);
    
    // Garbage collection threshold configuration
    public static final int GC_TIME_THRESHOLD_PERCENT = 
        Config.getIntProperty("health.check.garbage-collection.time-threshold-percent", 10);
    
    public static final int GC_FREQUENCY_THRESHOLD = 
        Config.getIntProperty("health.check.garbage-collection.frequency-threshold", 5);
    
    // System health check configuration
    public static final boolean SKIP_DISK_CHECK = 
        Config.getBooleanProperty("health.check.system.skip-disk-check", false);
    
    public static final int DISK_CHECK_INTERVAL_SECONDS = 
        Config.getIntProperty("health.check.system.disk-check-interval-seconds", 300);
    
    /**
     * Health check operating modes for controlling behavior in different environments
     */
    public enum HealthCheckMode {
        /**
         * Production mode - health checks report true status, can fail readiness/liveness
         */
        PRODUCTION,
        
        /**
         * Monitor mode - DOWN status converted to DEGRADED to prevent K8s probe failures during deployments
         */
        MONITOR_MODE,
        
        /**
         * Disabled - health check always returns UP with disabled message
         */
        DISABLED
    }
    
    // Private constructor to prevent instantiation
    private HealthCheckConfig() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Convenience method to check if system details should be included in responses
     */
    public static boolean shouldIncludeSystemDetails() {
        return INCLUDE_SYSTEM_DETAILS;
    }
    
    /**
     * Convenience method to check if performance metrics should be included in responses
     */
    public static boolean shouldIncludePerformanceMetrics() {
        return INCLUDE_PERFORMANCE_METRICS;
    }
    
    /**
     * Get the operating mode for cache health checks
     */
    public static HealthCheckMode getCacheCheckMode() {
        return parseMode(Config.getStringProperty("health.check.cache.mode", "PRODUCTION"));
    }
    
    /**
     * Get the operating mode for database health checks
     */
    public static HealthCheckMode getDatabaseCheckMode() {
        return parseMode(Config.getStringProperty("health.check.database.mode", "PRODUCTION"));
    }
    
    /**
     * Get the operating mode for thread health checks
     */
    public static HealthCheckMode getThreadCheckMode() {
        return parseMode(Config.getStringProperty("health.check.threads.mode", "PRODUCTION"));
    }
    
    /**
     * Get the operating mode for garbage collection health checks
     */
    public static HealthCheckMode getGcCheckMode() {
        return parseMode(Config.getStringProperty("health.check.garbage-collection.mode", "PRODUCTION"));
    }
    
    /**
     * Parse mode string to HealthCheckMode enum with proper error handling
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