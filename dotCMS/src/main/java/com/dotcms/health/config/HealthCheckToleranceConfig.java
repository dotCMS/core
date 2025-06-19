package com.dotcms.health.config;

import com.dotmarketing.util.Config;
import java.time.Duration;

/**
 * Configuration for health check failure tolerance settings.
 * This class provides configurable failure windows that allow health checks to report
 * DEGRADED status for a period of time before escalating to DOWN status.
 * 
 * Different tolerance periods can be configured for different escalation scenarios:
 * - Readiness tolerance: How long a check can fail before affecting readiness probes
 * - Liveness tolerance: How long a check can fail before affecting liveness probes (typically longer)
 * 
 * This provides circuit breaker-like functionality to prevent temporary issues from
 * immediately triggering pod restarts or traffic removal.
 */
public class HealthCheckToleranceConfig {
    
    // Global default tolerance settings - optimized for production responsiveness
    private static final Duration DEFAULT_READINESS_TOLERANCE = Duration.ofSeconds(30);
    private static final Duration DEFAULT_LIVENESS_TOLERANCE = Duration.ofMinutes(2);
    
    /**
     * Gets the failure tolerance period for readiness probes for a specific health check.
     * During this period, failing checks report DEGRADED instead of DOWN.
     * 
     * @param checkName the name of the health check
     * @return Duration to tolerate failures before affecting readiness
     */
    public static Duration getReadinessTolerance(String checkName) {
        // Check for check-specific configuration first
        String checkSpecificKey = "health.check." + checkName + ".readiness.tolerance.minutes";
        int checkSpecificMinutes = Config.getIntProperty(checkSpecificKey, -1);
        if (checkSpecificMinutes > 0) {
            return Duration.ofMinutes(checkSpecificMinutes);
        }
        
        // Check for seconds-based configuration
        int globalSeconds = Config.getIntProperty("health.tolerance.readiness.seconds", -1);
        if (globalSeconds > 0) {
            return Duration.ofSeconds(globalSeconds);
        }
        
        // Fall back to minutes-based configuration or default
        int globalMinutes = Config.getIntProperty("health.tolerance.readiness.minutes", -1);
        if (globalMinutes > 0) {
            return Duration.ofMinutes(globalMinutes);
        }
        
        // Use hardcoded default
        return DEFAULT_READINESS_TOLERANCE;
    }
    
    /**
     * Gets the failure tolerance period for liveness probes for a specific health check.
     * During this period, failing checks report DEGRADED instead of DOWN.
     * This is typically longer than readiness tolerance since liveness failures trigger restarts.
     * 
     * @param checkName the name of the health check
     * @return Duration to tolerate failures before affecting liveness
     */
    public static Duration getLivenessTolerance(String checkName) {
        // Check for check-specific configuration first
        String checkSpecificKey = "health.check." + checkName + ".liveness.tolerance.minutes";
        int checkSpecificMinutes = Config.getIntProperty(checkSpecificKey, -1);
        if (checkSpecificMinutes > 0) {
            return Duration.ofMinutes(checkSpecificMinutes);
        }
        
        // Fall back to global default
        int globalMinutes = Config.getIntProperty("health.tolerance.liveness.minutes", 
            (int) DEFAULT_LIVENESS_TOLERANCE.toMinutes());
        return Duration.ofMinutes(globalMinutes);
    }
    
    /**
     * Determines if failure tolerance is enabled for a specific health check.
     * Tolerance can be disabled per-check or globally.
     * 
     * @param checkName the name of the health check
     * @return true if failure tolerance is enabled
     */
    public static boolean isToleranceEnabled(String checkName) {
        // Check for check-specific disable
        String checkSpecificKey = "health.check." + checkName + ".tolerance.enabled";
        boolean checkSpecificEnabled = Config.getBooleanProperty(checkSpecificKey, true);
        if (!checkSpecificEnabled) {
            return false;
        }
        
        // Check global setting
        return Config.getBooleanProperty("health.tolerance.enabled", true);
    }
    
    /**
     * Gets the maximum number of consecutive failures allowed before immediate escalation to DOWN.
     * This provides a safety valve for rapidly failing checks.
     * 
     * @param checkName the name of the health check
     * @return maximum consecutive failures before immediate DOWN
     */
    public static int getMaxConsecutiveFailures(String checkName) {
        // Check for check-specific configuration
        String checkSpecificKey = "health.check." + checkName + ".max.consecutive.failures";
        int checkSpecific = Config.getIntProperty(checkSpecificKey, -1);
        if (checkSpecific > 0) {
            return checkSpecific;
        }
        
        // Fall back to global default
        return Config.getIntProperty("health.tolerance.max.consecutive.failures", 3);
    }
    
    /**
     * Determines if a health check should use different tolerance for liveness vs readiness.
     * When false, both use the same (readiness) tolerance period.
     * 
     * @param checkName the name of the health check
     * @return true if different tolerance periods should be used
     */
    public static boolean useDifferentLivenessTolerance(String checkName) {
        String checkSpecificKey = "health.check." + checkName + ".use.different.liveness.tolerance";
        boolean checkSpecific = Config.getBooleanProperty(checkSpecificKey, true);
        
        // Check global setting
        boolean globalSetting = Config.getBooleanProperty("health.tolerance.use.different.liveness", true);
        
        return checkSpecific && globalSetting;
    }
    
    /**
     * Gets a human-readable description of the tolerance settings for a health check
     */
    public static String getToleranceDescription(String checkName) {
        if (!isToleranceEnabled(checkName)) {
            return "Tolerance disabled - immediate failure escalation";
        }
        
        Duration readinessTolerance = getReadinessTolerance(checkName);
        Duration livenessTolerance = getLivenessTolerance(checkName);
        int maxFailures = getMaxConsecutiveFailures(checkName);
        
        if (useDifferentLivenessTolerance(checkName) && !readinessTolerance.equals(livenessTolerance)) {
            return String.format("Readiness tolerance: %dm, Liveness tolerance: %dm, Max failures: %d",
                readinessTolerance.toMinutes(), livenessTolerance.toMinutes(), maxFailures);
        } else {
            return String.format("Tolerance: %dm, Max failures: %d",
                readinessTolerance.toMinutes(), maxFailures);
        }
    }
    
    /**
     * Gets the quick failure threshold for readiness probes.
     * This is a shorter period for obvious failures that should fail fast.
     * 
     * @param checkName the name of the health check
     * @return Duration after which readiness should fail immediately for obvious issues
     */
    public static Duration getQuickFailThreshold(String checkName) {
        // Check for check-specific configuration first
        String checkSpecificKey = "health.check." + checkName + ".quick.fail.threshold.seconds";
        int checkSpecificSeconds = Config.getIntProperty(checkSpecificKey, -1);
        if (checkSpecificSeconds > 0) {
            return Duration.ofSeconds(checkSpecificSeconds);
        }
        
        // Check for check-specific threshold in different unit
        checkSpecificKey = "health.check." + checkName + ".quick.fail.threshold.minutes";
        int checkSpecificMinutes = Config.getIntProperty(checkSpecificKey, -1);
        if (checkSpecificMinutes > 0) {
            return Duration.ofMinutes(checkSpecificMinutes);
        }
        
        // Use database-specific defaults
        if ("database".equals(checkName)) {
            int defaultSeconds = Config.getIntProperty("health.tolerance.database.quick.fail.seconds", 15);
            return Duration.ofSeconds(defaultSeconds);
        }
        
        // Use cache-specific defaults  
        if ("cache".equals(checkName)) {
            int defaultSeconds = Config.getIntProperty("health.tolerance.cache.quick.fail.seconds", 30);
            return Duration.ofSeconds(defaultSeconds);
        }
        
        // Fall back to global default
        int globalSeconds = Config.getIntProperty("health.tolerance.quick.fail.seconds", 60);
        return Duration.ofSeconds(globalSeconds);
    }
}