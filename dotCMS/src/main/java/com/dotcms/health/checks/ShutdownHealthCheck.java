package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotcms.shutdown.ShutdownCoordinator;
import com.dotmarketing.util.Config;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check that monitors system shutdown status for Kubernetes integration.
 * 
 * This check ensures proper traffic routing during shutdown:
 * - READINESS: Fails immediately when shutdown begins to stop new traffic
 * - LIVENESS: Continues to pass while system can still process existing requests
 * 
 * This allows for graceful shutdown with request draining:
 * 1. Shutdown begins → /readyz fails → load balancer stops routing new traffic
 * 2. Existing requests continue to be processed → /livez still passes
 * 3. Once request draining completes → /livez can optionally fail for faster pod termination
 * 
 * Configuration Properties:
 * - health.check.shutdown.mode = Safety mode (PRODUCTION, MONITOR_MODE, DISABLED)
 * - health.check.shutdown.fail-liveness-on-completion = Whether to fail liveness after shutdown completes (default: false)
 */
public class ShutdownHealthCheck extends HealthCheckBase {
    
    private static final String CHECK_NAME = "shutdown";
    
    @Override
    public String getName() {
        return CHECK_NAME;
    }
    
    @Override
    protected CheckResult performCheck() throws Exception {
        ShutdownCoordinator.ShutdownStatus status = ShutdownCoordinator.getShutdownStatus();
        
        boolean shutdownInProgress = status.isShutdownInProgress();
        boolean requestDrainingInProgress = status.isRequestDrainingInProgress();
        boolean shutdownCompleted = status.isShutdownCompleted();
        long activeRequests = status.getActiveRequestCount();
        
        // Determine if system is healthy based on shutdown state
        boolean healthy;
        String message;
        
        if (shutdownCompleted) {
            // System has completed shutdown - still functional but terminating
            healthy = shouldFailLivenessOnCompletion(); // Only fail if configured to do so
            message = "System shutdown completed - terminating soon";
        } else if (shutdownInProgress) {
            if (requestDrainingInProgress) {
                // Currently draining requests - system is degraded but still functional for readiness
                healthy = false; // This will cause readiness to fail, stopping new traffic
                message = String.format("System shutdown in progress - draining %d active requests", activeRequests);
            } else {
                // Shutdown started but not draining yet, or draining completed
                healthy = false; // This will cause readiness to fail
                message = "System shutdown in progress - component shutdown phase";
            }
        } else {
            // Normal operation
            healthy = true;
            message = "System operating normally";
        }
        
        return new CheckResult(healthy, 1L, message);
    }
    
    /**
     * This is NOT a liveness check by default.
     * Liveness checks should only fail for unrecoverable conditions that require restart.
     * Shutdown is a controlled process, not a failure requiring restart.
     * 
     * However, can be configured to fail liveness after shutdown completion
     * for faster pod termination in some deployment scenarios.
     */
    @Override
    public boolean isLivenessCheck() {
        // Only participate in liveness if configured to fail on completion
        return shouldFailLivenessOnCompletion();
    }
    
    /**
     * This IS a readiness check - critical for proper traffic routing.
     * As soon as shutdown begins, readiness should fail to stop new traffic
     * while allowing existing requests to complete.
     */
    @Override
    public boolean isReadinessCheck() {
        return getMode() != HealthCheckMode.DISABLED;
    }
    
    /**
     * Check if liveness should fail when shutdown is completed.
     * This can speed up pod termination in some Kubernetes deployments.
     */
    private boolean shouldFailLivenessOnCompletion() {
        return Config.getBooleanProperty("health.check.shutdown.fail-liveness-on-completion", false);
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                     HealthStatus finalStatus, HealthCheckMode mode) {
        // Build structured data for monitoring
        ShutdownCoordinator.ShutdownStatus status = ShutdownCoordinator.getShutdownStatus();
        
        Map<String, Object> data = new HashMap<>();
        data.put("shutdownInProgress", status.isShutdownInProgress());
        data.put("requestDrainingInProgress", status.isRequestDrainingInProgress());
        data.put("shutdownCompleted", status.isShutdownCompleted());
        data.put("activeRequestCount", status.getActiveRequestCount());
        data.put("readinessImpact", status.isShutdownInProgress() ? "FAIL" : "PASS");
        data.put("livenessImpact", shouldFailLivenessOnCompletion() && status.isShutdownCompleted() ? "FAIL" : "PASS");
        data.put("originalStatus", originalStatus.name());
        data.put("finalStatus", finalStatus.name());
        data.put("mode", mode.name());
        
        return data;
    }
    
    @Override
    public String getDescription() {
        boolean failLiveness = shouldFailLivenessOnCompletion();
        return String.format("Monitors system shutdown status for Kubernetes traffic routing " +
            "(readiness: always, liveness: %s) (Mode: %s)", 
            failLiveness ? "on completion" : "never", getMode().name());
    }
    
    @Override
    public int getOrder() {
        // Run early in the health check sequence since it's very fast
        return 10;
    }
} 