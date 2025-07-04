package com.dotcms.health.service;

import com.dotcms.health.config.HealthCheckToleranceConfig;
import com.dotcms.health.model.HealthCheckFailureWindow;
import com.dotcms.health.model.HealthCheckFailureWindowFactory;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages failure tolerance windows for health checks.
 * This service implements circuit breaker-like functionality, allowing health checks
 * to report DEGRADED status during temporary failures before escalating to DOWN.
 * 
 * Key Features:
 * - Configurable tolerance periods for readiness vs liveness escalation
 * - Tracks consecutive failure counts and durations
 * - Provides safety valve for rapid failures (immediate DOWN after threshold)
 * - Per-health-check configuration support
 */
public class HealthCheckToleranceManager {
    
    private final ConcurrentHashMap<String, HealthCheckFailureWindow> failureWindows = new ConcurrentHashMap<>();
    
    /**
     * Evaluates a health check result and applies failure tolerance logic.
     * Returns a potentially modified result that reports DEGRADED instead of DOWN
     * if the check is within its tolerance window.
     * 
     * @param originalResult the original health check result
     * @param isForLiveness whether this evaluation is for a liveness probe (uses longer tolerance)
     * @param isInStartupPhase whether the system is still in startup phase
     * @return the result after applying tolerance logic
     */
    public HealthCheckResult evaluateWithTolerance(HealthCheckResult originalResult, boolean isForLiveness, boolean isInStartupPhase) {
        String checkName = originalResult.name();
        HealthStatus rawStatus = originalResult.status();
        Instant now = Instant.now();
        
        // Check if this is a monitor mode result (DEGRADED status from monitor mode conversion)
        boolean isMonitorModeConversion = rawStatus == HealthStatus.DEGRADED && isMonitorModeResult(checkName);
        
        // CRITICAL: During startup phase, do NOT apply tolerance logic
        // Startup failures should return genuine DOWN status (503) so K8s doesn't route traffic
        if (isInStartupPhase) {
            Logger.debug(this, String.format(
                "Health check '%s' in startup phase - tolerance logic disabled, returning actual status: %s",
                checkName, originalResult.status()
            ));
            return originalResult;
        }
        
        // If tolerance is disabled for this check, return original result
        if (!HealthCheckToleranceConfig.isToleranceEnabled(checkName)) {
            return originalResult;
        }
        
        // IMPORTANT: Check if this is a DEGRADED result from MONITOR_MODE
        // MONITOR_MODE converts DOWN->DEGRADED and we should preserve this conversion
        // Don't apply additional tolerance logic to results that are already converted by monitor mode
        if (originalResult.status() == HealthStatus.DEGRADED && isMonitorModeResult(checkName)) {
            Logger.debug(this, String.format(
                "Health check '%s' already converted to DEGRADED by monitor mode - preserving status",
                checkName
            ));
            return originalResult;
        }
        
        // Get or create failure window for this check
        HealthCheckFailureWindow window = failureWindows.computeIfAbsent(
            checkName,
                HealthCheckFailureWindowFactory::passing
        );
        
        // Update the failure window based on current result
        HealthCheckFailureWindow updatedWindow = updateFailureWindow(window, originalResult, isForLiveness);
        failureWindows.put(checkName, updatedWindow);
        
        // Return potentially modified result based on tolerance logic
        return applyToleranceToResult(originalResult, updatedWindow);
    }
    
    /**
     * Backward compatibility method - assumes operational phase (not startup)
     * @deprecated Use evaluateWithTolerance(HealthCheckResult, boolean, boolean) instead
     */
    @Deprecated
    public HealthCheckResult evaluateWithTolerance(HealthCheckResult originalResult, boolean isForLiveness) {
        return evaluateWithTolerance(originalResult, isForLiveness, false);
    }
    
    /**
     * Updates the failure window based on the current health check result
     */
    private HealthCheckFailureWindow updateFailureWindow(HealthCheckFailureWindow currentWindow, 
                                                        HealthCheckResult result, 
                                                        boolean isForLiveness) {
        String checkName = result.name();
        HealthStatus rawStatus = result.status();
        Instant now = Instant.now();
        
        // Check if this DEGRADED status is from MONITOR_MODE conversion (underlying failure)
        boolean isMonitorModeConversion = rawStatus == HealthStatus.DEGRADED && isMonitorModeResult(checkName);
        
        // If check is genuinely passing (UP), reset the failure window
        // NOTE: DEGRADED status should NOT be treated as recovery - it indicates ongoing issues
        if (rawStatus == HealthStatus.UP) {
            if (currentWindow.failureSequenceStart().isPresent()) {
                // Log recovery from failure window
                Duration failureDuration = Duration.between(currentWindow.failureSequenceStart().get(), now);
                
                Logger.info(this, String.format(
                    "Health check '%s' recovered after %dm %ds of failures (%d consecutive failures)",
                    checkName, failureDuration.toMinutes(), failureDuration.toSeconds() % 60,
                    currentWindow.consecutiveFailures()
                ));
            }
            return HealthCheckFailureWindowFactory.passing(checkName);
        }
        
        // Check is failing (DOWN or DEGRADED from monitor mode conversion)
        if (currentWindow.failureSequenceStart().isEmpty()) {
            // Starting a new failure sequence
            String message = result.error().orElse(result.message().orElse("Health check failed"));
            Logger.info(this, String.format(
                "Health check '%s' starting failure tolerance window: %s",
                checkName, message
            ));
            
            return HealthCheckFailureWindowFactory.startingFailure(checkName, rawStatus, message);
        } else {
            // Continuing existing failure sequence
            int newConsecutiveFailures = currentWindow.consecutiveFailures() + 1;
            Duration failureDuration = Duration.between(currentWindow.failureSequenceStart().get(), now);
            
            // Determine effective status based on tolerance settings
            HealthStatus effectiveStatus = determineEffectiveStatus(
                checkName, rawStatus, failureDuration, newConsecutiveFailures, isForLiveness
            );
            
            String toleranceMessage = createToleranceMessage(
                checkName, failureDuration, newConsecutiveFailures, effectiveStatus, isForLiveness
            );
            
            // Check if circuit breaker is now active (for logging state tracking)  
            int maxFailures = HealthCheckToleranceConfig.getMaxConsecutiveFailures(checkName);
            boolean circuitBreakerActive = newConsecutiveFailures >= maxFailures;
            boolean shouldLogCircuitBreaker = circuitBreakerActive && !currentWindow.circuitBreakerLogged();
            
            return HealthCheckFailureWindow.builder()
                .from(currentWindow)
                .consecutiveFailures(newConsecutiveFailures)
                .rawStatus(rawStatus)
                .effectiveStatus(effectiveStatus)
                .lastEvaluated(now)
                .toleranceMessage(toleranceMessage)
                .circuitBreakerLogged(currentWindow.circuitBreakerLogged() || shouldLogCircuitBreaker)
                .build();
        }
    }
    
    /**
     * Determines the effective status based on tolerance configuration and failure characteristics
     */
    private HealthStatus determineEffectiveStatus(String checkName, HealthStatus rawStatus, 
                                                 Duration failureDuration, int consecutiveFailures,
                                                 boolean isForLiveness) {
        
        // IMMEDIATE FAILURE CONDITIONS - No tolerance for obvious problems
        
        // 1. Safety valve - too many consecutive failures (circuit breaker pattern)
        int maxFailures = HealthCheckToleranceConfig.getMaxConsecutiveFailures(checkName);
        if (consecutiveFailures >= maxFailures) {
            // Only log circuit breaker activation once per failure sequence
            HealthCheckFailureWindow currentWindow = failureWindows.get(checkName);
            if (currentWindow == null || !currentWindow.circuitBreakerLogged()) {
                Logger.warn(this, String.format(
                    "Health check '%s' circuit breaker activated: exceeded max consecutive failures (%d)",
                    checkName, maxFailures
                ));
            }
            return HealthStatus.DOWN;
        }
        
        // 2. Fast failure for database-specific severe conditions
        if ("database".equals(checkName)) {
            HealthCheckFailureWindow currentWindow = failureWindows.get(checkName);
            if (currentWindow != null && shouldFailDatabaseFast(currentWindow, consecutiveFailures, failureDuration)) {
                Logger.warn(this, String.format(
                    "Database health check failing fast due to severe conditions (failures: %d, duration: %s)",
                    consecutiveFailures, formatDuration(failureDuration)
                ));
                return HealthStatus.DOWN;
            }
        }
        
        // 3. Quick failure threshold for readiness (different from liveness)
        if (!isForLiveness) {
            Duration quickFailThreshold = HealthCheckToleranceConfig.getQuickFailThreshold(checkName);
            if (failureDuration.compareTo(quickFailThreshold) >= 0) {
                // Only log on first occurrence of quick fail threshold exceeded
                HealthCheckFailureWindow currentWindow = failureWindows.get(checkName);
                if (currentWindow == null || currentWindow.effectiveStatus() != HealthStatus.DOWN) {
                    Logger.warn(this, String.format(
                        "Health check '%s' exceeded quick fail threshold (%s), failing readiness",
                        checkName, formatDuration(quickFailThreshold)
                    ));
                }
                return HealthStatus.DOWN;
            }
        }
        
        // TOLERANCE WINDOW - Allow degraded operation for recoverable issues
        
        // Get appropriate tolerance period
        Duration tolerancePeriod;
        if (isForLiveness && HealthCheckToleranceConfig.useDifferentLivenessTolerance(checkName)) {
            tolerancePeriod = HealthCheckToleranceConfig.getLivenessTolerance(checkName);
        } else {
            tolerancePeriod = HealthCheckToleranceConfig.getReadinessTolerance(checkName);
        }
        
        // Check if we're still within tolerance window
        if (failureDuration.compareTo(tolerancePeriod) < 0) {
            return HealthStatus.DEGRADED; // Still within tolerance
        } else {
            // Only log on first occurrence of tolerance window exceeded
            HealthCheckFailureWindow currentWindow = failureWindows.get(checkName);
            if (currentWindow == null || currentWindow.effectiveStatus() != HealthStatus.DOWN) {
                Logger.warn(this, String.format(
                    "Health check '%s' exceeded %s tolerance window (%s), escalating to DOWN",
                    checkName, isForLiveness ? "liveness" : "readiness", formatDuration(tolerancePeriod)
                ));
            }
            return HealthStatus.DOWN; // Tolerance window exceeded
        }
    }
    
    /**
     * Determines if database health check should fail fast based on failure characteristics
     */
    private boolean shouldFailDatabaseFast(HealthCheckFailureWindow window, int consecutiveFailures, Duration failureDuration) {
        // Fast failure conditions for database:
        
        // 1. Multiple consecutive failures in short time (likely complete outage)
        if (consecutiveFailures >= 3 && failureDuration.compareTo(Duration.ofSeconds(30)) < 0) {
            return true; // 3 failures in 30 seconds = fast failure
        }
        
        // 2. Any failure lasting more than 45 seconds (likely not a blip)
        if (failureDuration.compareTo(Duration.ofSeconds(45)) >= 0) {
            return true; // 45+ seconds = stop tolerating
        }
        
        // 3. Check error message for severe conditions (if available)
        if (window.toleranceMessage().isPresent()) {
            String message = window.toleranceMessage().get().toLowerCase();
            if (message.contains("connection refused") || 
                message.contains("connection timeout") ||
                message.contains("unknown host") ||
                message.contains("network unreachable")) {
                return true; // Network-level failures = fast failure
            }
        }
        
        return false; // Continue with normal tolerance
    }
    
    /**
     * Formats duration for human-readable logging
     */
    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.toSeconds() % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Creates a descriptive tolerance message for logging and monitoring
     */
    private String createToleranceMessage(String checkName, Duration failureDuration, 
                                        int consecutiveFailures, HealthStatus effectiveStatus,
                                        boolean isForLiveness) {
        long minutes = failureDuration.toMinutes();
        long seconds = failureDuration.toSeconds() % 60;
        
        String durationStr = minutes > 0 ? 
            String.format("%dm %ds", minutes, seconds) : 
            String.format("%ds", seconds);
        
        if (effectiveStatus == HealthStatus.DEGRADED) {
            Duration tolerancePeriod = isForLiveness && HealthCheckToleranceConfig.useDifferentLivenessTolerance(checkName) ?
                HealthCheckToleranceConfig.getLivenessTolerance(checkName) :
                HealthCheckToleranceConfig.getReadinessTolerance(checkName);
                
            long remainingMinutes = tolerancePeriod.minus(failureDuration).toMinutes();
            
            return String.format(
                "Failing for %s (%d consecutive), %dm tolerance remaining (%s probe)",
                durationStr, consecutiveFailures, remainingMinutes, isForLiveness ? "liveness" : "readiness"
            );
        } else {
            return String.format(
                "Failed for %s (%d consecutive), tolerance window exceeded",
                durationStr, consecutiveFailures
            );
        }
    }
    
    /**
     * Applies tolerance logic to create the final health check result
     */
    private HealthCheckResult applyToleranceToResult(HealthCheckResult originalResult, 
                                                   HealthCheckFailureWindow window) {
        // If effective status is the same as raw status, return original
        if (window.effectiveStatus() == originalResult.status()) {
            return originalResult;
        }
        
        // Create modified result with tolerance-adjusted status and message
        String enhancedMessage = window.toleranceMessage()
            .map(tolerance -> originalResult.message()
                .map(original -> original + " [" + tolerance + "]")
                .orElse(tolerance))
            .orElse(originalResult.message().orElse(""));
        
        return HealthCheckResult.builder()
            .from(originalResult)
            .status(window.effectiveStatus())
            .message(enhancedMessage)
            .build();
    }
    
    /**
     * Gets the current failure window for a health check (for monitoring/debugging)
     */
    public HealthCheckFailureWindow getFailureWindow(String checkName) {
        return failureWindows.get(checkName);
    }
    
    
    /**
     * Clears failure window for a specific health check
     */
    public void clearFailureWindow(String checkName) {
        HealthCheckFailureWindow removed = failureWindows.remove(checkName);
        if (removed != null) {
            Logger.info(this, String.format("Failure window cleared for health check '%s'", checkName));
        }
    }
    
    /**
     * Called when transitioning from startup to operational phase.
     * Clears all failure windows since startup failures don't count towards operational tolerance.
     */
    public void transitionToOperationalPhase() {
        int clearedWindows = failureWindows.size();
        
        // Log which specific health checks had failure windows before clearing
        if (clearedWindows > 0) {
            String clearedCheckNames = failureWindows.keySet().stream()
                .collect(Collectors.joining(", "));
            Logger.info(this, String.format(
                "Transitioned to operational phase - clearing %d startup failure windows for checks: [%s]. Tolerance logic now active.",
                clearedWindows, clearedCheckNames
            ));
        }
        
        failureWindows.clear();
        
        if (clearedWindows == 0) {
            Logger.info(this, "Transitioned to operational phase - tolerance logic now active");
        }
    }
    
    /**
     * Gets count of current failure windows (for monitoring)
     */
    public int getActiveFailureWindowCount() {
        return (int) failureWindows.values().stream()
            .filter(window -> window.failureSequenceStart().isPresent())
            .count();
    }
    
    /**
     * Checks if a health check is configured with MONITOR_MODE.
     * This is used to detect when DEGRADED status is the result of monitor mode conversion
     * rather than a natural DEGRADED state, avoiding double application of tolerance logic.
     */
    private boolean isMonitorModeResult(String checkName) {
        String mode = Config.getStringProperty("health.check." + checkName + ".mode", "PRODUCTION");
        return "MONITOR_MODE".equalsIgnoreCase(mode.trim());
    }
    
    /**
     * Removes the failure window for a specific health check.
     * This should be called when a health check is permanently removed
     * to prevent memory leaks in dynamic environments.
     * 
     * @param checkName the name of the health check to remove
     * @return true if a failure window was removed, false if none existed
     */
    public boolean removeFailureWindow(String checkName) {
        HealthCheckFailureWindow removed = failureWindows.remove(checkName);
        if (removed != null) {
            Logger.debug(this, String.format("Removed failure window for health check: %s", checkName));
            return true;
        }
        return false;
    }
    
    /**
     * Clears all failure windows. This is primarily for testing or complete system reset.
     * In production, use removeFailureWindow() for individual health check cleanup.
     */
    public void clearAllFailureWindows() {
        int count = failureWindows.size();
        failureWindows.clear();
        Logger.info(this, String.format("Cleared all failure windows (%d removed)", count));
    }
    
    /**
     * Gets the current number of tracked failure windows.
     * This is useful for monitoring memory usage and debugging.
     * 
     * @return the number of health checks with active failure windows
     */
    public int getFailureWindowCount() {
        return failureWindows.size();
    }
}