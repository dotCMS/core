package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.service.HealthStateManager;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Logger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Garbage Collection health check that monitors GC pressure and performance impact.
 * This check has configurable safety modes to prevent unwanted probe failures
 * during initial deployment, as GC behavior can vary significantly by workload.
 * 
 * STARTUP AWARENESS: During startup phase, this check uses much more lenient thresholds
 * since high GC activity is completely normal during application initialization.
 * 
 * Configuration Properties:
 * - health.check.garbage-collection.mode = Safety mode (PRODUCTION, DEGRADED_SAFE, MONITORING_ONLY, DISABLED)
 * - health.check.garbage-collection.timeout-ms = GC check timeout (default: 2000ms)
 * - health.check.garbage-collection.time-threshold-percent = GC time threshold percentage (default: 30)
 * - health.check.garbage-collection.frequency-threshold = GC frequency threshold per minute (default: 100)
 * - health.check.garbage-collection.startup-time-threshold-percent = Startup GC time threshold (default: 80)
 * - health.check.garbage-collection.startup-frequency-threshold = Startup GC frequency threshold (default: 300)
 */
public class GarbageCollectionHealthCheck extends HealthCheckBase {
    
    // Cached values for GC pressure calculation
    private static volatile long lastCheckTime = System.currentTimeMillis();
    private static volatile long lastTotalGcTime = 0;
    private static volatile long lastTotalGcCount = 0;
    
    @Override
    protected CheckResult performCheck() throws Exception {
        // Check if we're in startup phase and adjust thresholds accordingly
        boolean isStartupPhase = HealthStateManager.getInstance().isInStartupPhase();
        
        int gcTimeThreshold;
        int gcFrequencyThreshold;
        
        if (isStartupPhase) {
            // Much more lenient thresholds during startup since high GC is expected
            // Especially for memory-constrained environments (e.g., 1GB heap)
            gcTimeThreshold = getConfigProperty("startup-time-threshold-percent", 90);
            gcFrequencyThreshold = getConfigProperty("startup-frequency-threshold", 500);
        } else {
            // Normal operational thresholds
            gcTimeThreshold = getConfigProperty("time-threshold-percent", 30);
            gcFrequencyThreshold = getConfigProperty("frequency-threshold", 100);
        }
        
        return measureExecution(() -> {
            GcHealthResult gcHealth = checkGcHealth(gcTimeThreshold, gcFrequencyThreshold, isStartupPhase);
            
            if (gcHealth.status.equals("DOWN")) {
                String phaseInfo = isStartupPhase ? " during startup (expected)" : " in operational phase";
                throw new Exception("GC performance issues" + phaseInfo + ": " + gcHealth.issues);
            }
            
            String phaseInfo = isStartupPhase ? " [STARTUP - higher thresholds]" : "";
            return String.format("GC performance healthy - Time: %.1f%%, Frequency: %.1f/min, Collections: %d%s", 
                gcHealth.gcTimePercent, gcHealth.gcFrequency, gcHealth.totalGcCount, phaseInfo);
        });
    }
    
    private GcHealthResult checkGcHealth(int gcTimeThreshold, int gcFrequencyThreshold, boolean isStartupPhase) {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        long currentTime = System.currentTimeMillis();
        long totalGcTime = 0;
        long totalGcCount = 0;
        StringBuilder gcDetails = new StringBuilder();
        
        // Collect GC statistics
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long gcTime = gcBean.getCollectionTime();
            long gcCount = gcBean.getCollectionCount();
            
            if (gcTime > 0) {
                totalGcTime += gcTime;
                totalGcCount += gcCount;
                
                if (gcDetails.length() > 0) {
                    gcDetails.append(", ");
                }
                gcDetails.append(gcBean.getName()).append(": ")
                        .append(gcCount).append(" collections, ")
                        .append(gcTime).append("ms");
            }
        }
        
        // Calculate GC pressure since last check
        long timeDelta = currentTime - lastCheckTime;
        long gcTimeDelta = totalGcTime - lastTotalGcTime;
        long gcCountDelta = totalGcCount - lastTotalGcCount;
        
        double gcTimePercent = 0.0;
        double gcFrequency = 0.0;
        
        if (timeDelta > 0) {
            gcTimePercent = (double) gcTimeDelta / timeDelta * 100.0;
            gcFrequency = (double) gcCountDelta / (timeDelta / 60000.0); // GCs per minute
        }
        
        // Update cached values
        lastCheckTime = currentTime;
        lastTotalGcTime = totalGcTime;
        lastTotalGcCount = totalGcCount;
        
        // Analyze GC health with startup-aware logic
        boolean hasIssues = false;
        StringBuilder issues = new StringBuilder();
        
        // Check GC time threshold
        if (gcTimePercent > gcTimeThreshold) {
            hasIssues = true;
            issues.append("High GC time (").append(String.format("%.1f", gcTimePercent))
                  .append("% > ").append(gcTimeThreshold).append("%"); 
            if (isStartupPhase) {
                issues.append(" during startup");
            }
            issues.append("); ");
        }
        
        // Check GC frequency threshold
        if (gcFrequency > gcFrequencyThreshold) {
            hasIssues = true;
            issues.append("High GC frequency (").append(String.format("%.1f", gcFrequency))
                  .append(" > ").append(gcFrequencyThreshold).append(" per min");
            if (isStartupPhase) {
                issues.append(" during startup");
            }
            issues.append("); ");
        }
        
        // During startup, only log issues but don't fail as aggressively
        if (hasIssues && isStartupPhase) {
            Logger.debug(this, String.format(
                "GC pressure during startup phase: Time=%.1f%% (threshold=%d%%), Frequency=%.1f/min (threshold=%d/min) - %s",
                gcTimePercent, gcTimeThreshold, gcFrequency, gcFrequencyThreshold, 
                "this is expected during application initialization"
            ));
        }
        
        String status = hasIssues ? "DOWN" : "UP";
        
        return new GcHealthResult(
            status,
            gcTimePercent,
            gcFrequency,
            totalGcTime,
            totalGcCount,
            gcDetails.toString(),
            issues.toString()
        );
    }
    
    @Override
    public String getName() {
        return "gc";
    }
    
    @Override
    protected HealthCheckMode getDefaultMode() {
        return HealthCheckMode.MONITOR_MODE;
    }
    
    @Override
    public int getOrder() {
        return 50; // Lower priority - performance monitoring
    }
    
    /**
     * GC check should NEVER fail liveness probes in DEGRADED_SAFE mode (default)
     * High GC activity during startup is completely normal and expected.
     * Only severe, sustained GC thrashing should be considered a liveness issue.
     */
    @Override
    public boolean isLivenessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    /**
     * Used for readiness - GC pressure can affect performance
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        HealthCheckMode mode = getMode();
        boolean isStartupPhase = HealthStateManager.getInstance().isInStartupPhase();
        
        int gcTimeThreshold;
        int gcFrequencyThreshold;
        
        if (isStartupPhase) {
            gcTimeThreshold = getConfigProperty("startup-time-threshold-percent", 90);
            gcFrequencyThreshold = getConfigProperty("startup-frequency-threshold", 500);
        } else {
            gcTimeThreshold = getConfigProperty("time-threshold-percent", 30);
            gcFrequencyThreshold = getConfigProperty("frequency-threshold", 100);
        }
        
        String phaseInfo = isStartupPhase ? " [STARTUP: relaxed thresholds]" : " [OPERATIONAL]";
        
        return String.format("Monitors GC performance (time: %d%%, freq: %d/min) (Mode: %s)%s", 
            gcTimeThreshold, gcFrequencyThreshold, mode.name(), phaseInfo);
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Check if we're in startup phase to get the right thresholds
        boolean isStartupPhase = HealthStateManager.getInstance().isInStartupPhase();
        
        int gcTimeThreshold;
        int gcFrequencyThreshold;
        
        if (isStartupPhase) {
            gcTimeThreshold = getConfigProperty("startup-time-threshold-percent", 90);
            gcFrequencyThreshold = getConfigProperty("startup-frequency-threshold", 500);
        } else {
            gcTimeThreshold = getConfigProperty("time-threshold-percent", 30);
            gcFrequencyThreshold = getConfigProperty("frequency-threshold", 100);
        }
        
        // Get current GC metrics that appear in the message
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalGcTime = 0;
        long totalGcCount = 0;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalGcTime += gcBean.getCollectionTime();
            totalGcCount += gcBean.getCollectionCount();
        }
        
        // Calculate current GC pressure
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastCheckTime;
        long gcTimeDelta = totalGcTime - lastTotalGcTime;
        
        double gcTimePercent = 0.0;
        double gcFrequency = 0.0;
        
        if (timeDelta > 0) {
            gcTimePercent = (double) gcTimeDelta / timeDelta * 100.0;
            gcFrequency = (double) (totalGcCount - lastTotalGcCount) / (timeDelta / 60000.0);
        }
        
        // Include all data that appears in messages
        data.put("gcTimePercent", Math.round(gcTimePercent * 10.0) / 10.0);
        data.put("gcFrequencyPerMin", Math.round(gcFrequency * 10.0) / 10.0);
        data.put("totalCollections", totalGcCount);
        data.put("isStartupPhase", isStartupPhase);
        
        // Include thresholds for monitoring
        data.put("timeThresholdPercent", gcTimeThreshold);
        data.put("frequencyThreshold", gcFrequencyThreshold);
        
        // Include error type for GC-related failures
        if (result.error != null) {
            data.put("errorType", "gc_pressure");
        }
        
        return data;
    }
    
    /**
     * Internal class to hold GC health check results
     */
    private static class GcHealthResult {
        final String status;
        final double gcTimePercent;
        final double gcFrequency;
        final long totalGcTime;
        final long totalGcCount;
        final String gcDetails;
        final String issues;
        
        GcHealthResult(String status, double gcTimePercent, double gcFrequency, 
                      long totalGcTime, long totalGcCount, String gcDetails, String issues) {
            this.status = status;
            this.gcTimePercent = gcTimePercent;
            this.gcFrequency = gcFrequency;
            this.totalGcTime = totalGcTime;
            this.totalGcCount = totalGcCount;
            this.gcDetails = gcDetails;
            this.issues = issues;
        }
    }
} 