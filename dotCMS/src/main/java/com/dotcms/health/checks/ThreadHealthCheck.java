package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread health check that monitors for deadlocks and thread pool exhaustion.
 * This check has configurable safety modes to prevent unwanted probe failures
 * during initial deployment.
 * 
 * Configuration Properties:
 * - health.check.threads.mode = Safety mode (PRODUCTION, DEGRADED_SAFE, MONITORING_ONLY, DISABLED)
 * - health.check.threads.timeout-ms = Thread check timeout (default: 2000ms)
 * - health.check.threads.deadlock-detection = Enable deadlock detection (default: true)
 * - health.check.threads.pool-threshold-multiplier = Thread count threshold multiplier (default: 20)
 * 
 * Note: This check is disabled by default (DISABLED mode) to allow for testing and tuning
 * of the thread threshold multiplier in production environments. Once an appropriate
 * threshold is determined, the mode can be changed to PRODUCTION or DEGRADED_SAFE.
 */
public class ThreadHealthCheck extends HealthCheckBase {
    
    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    
    @Override
    protected CheckResult performCheck() throws Exception {
        boolean enableDeadlockDetection = getConfigProperty("deadlock-detection", true);
        int threadThresholdMultiplier = getConfigProperty("pool-threshold-multiplier", 20);
        
        // Use utility for the actual thread health check
        return measureExecution(() -> 
            checkThreadHealth(enableDeadlockDetection, threadThresholdMultiplier)
        );
    }
    
    /**
     * Checks thread health for deadlocks and excessive thread count specific to this health check.
     * 
     * @param enableDeadlockDetection whether to check for deadlocks
     * @param threadThresholdMultiplier multiplier for available processors to determine thread count threshold
     * @return thread health status message
     * @throws Exception if critical thread issues are detected
     */
    private String checkThreadHealth(boolean enableDeadlockDetection, int threadThresholdMultiplier) throws Exception {
        StringBuilder issues = new StringBuilder();
        boolean hasIssues = false;
        
        // Check for deadlocks if enabled
        long[] deadlockedThreads = null;
        if (enableDeadlockDetection) {
            deadlockedThreads = THREAD_BEAN.findDeadlockedThreads();
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                hasIssues = true;
                issues.append("Deadlock detected (").append(deadlockedThreads.length).append(" threads); ");
            }
        }
        
        // Check thread count against threshold
        int currentThreadCount = THREAD_BEAN.getThreadCount();
        int threshold = AVAILABLE_PROCESSORS * threadThresholdMultiplier;
        
        if (currentThreadCount > threshold) {
            hasIssues = true;
            issues.append("High thread count (").append(currentThreadCount)
                  .append(" > ").append(threshold).append("); ");
        }
        
        if (hasIssues) {
            throw new Exception("Thread system issues: " + issues.toString().trim());
        }
        
        return String.format("Thread system healthy: %d threads (threshold: %d)%s", 
            currentThreadCount, threshold, 
            enableDeadlockDetection ? ", no deadlocks" : "");
    }
    
    @Override
    public String getName() {
        return "threads";
    }

    @Override
    protected HealthCheckMode getDefaultMode() {
        // Further testing to see if any problems arise with this check
        return HealthCheckMode.DISABLED;
    }

    @Override
    public int getOrder() {
        return 40; // Medium priority - advanced monitoring
    }
    
    /**
     * Configurable for liveness based on safety mode
     * - PRODUCTION mode: Used for liveness (can trigger restarts)
     * - DEGRADED_SAFE mode: Used for liveness (but never fails probes)
     * - MONITORING_ONLY: Used for liveness (always passes)
     * - DISABLED: Not used
     */
    @Override
    public boolean isLivenessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    /**
     * Always used for readiness (safe with degraded mode)
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        HealthCheckMode mode = getMode();
        boolean deadlockDetection = getConfigProperty("deadlock-detection", true);
        int threadThresholdMultiplier = getConfigProperty("pool-threshold-multiplier", 20);
        
        return String.format("Monitors thread health (deadlock detection: %s, threshold multiplier: %dx) (Mode: %s)", 
            deadlockDetection, threadThresholdMultiplier, mode.name());
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Always include thread count and threshold as they're threshold-based checks
        int currentThreadCount = THREAD_BEAN.getThreadCount();
        int threadThresholdMultiplier = getConfigProperty("pool-threshold-multiplier", 20);
        int threshold = AVAILABLE_PROCESSORS * threadThresholdMultiplier;
        boolean enableDeadlockDetection = getConfigProperty("deadlock-detection", true);
        
        data.put("currentThreadCount", currentThreadCount);
        data.put("threshold", threshold);
        
        // Include deadlock information when detection is enabled
        if (enableDeadlockDetection) {
            long[] deadlockedThreads = THREAD_BEAN.findDeadlockedThreads();
            boolean hasDeadlocks = deadlockedThreads != null && deadlockedThreads.length > 0;
            data.put("hasDeadlocks", hasDeadlocks);
            if (hasDeadlocks) {
                data.put("deadlockedThreadCount", deadlockedThreads.length);
            }
        }
        
        // Include error type for thread-related failures
        if (result.error != null) {
            data.put("errorType", "thread_system");
        }
        
        return data;
    }
} 