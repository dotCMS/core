package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * System-level health check that monitors core system resources and performance.
 * This check is safe for both liveness and readiness probes as it only checks
 * internal system state without external dependencies.
 * 
 * Configuration Properties:
 * - health.check.system.mode = Safety mode (PRODUCTION, MONITOR_MODE, DISABLED)
 * - health.check.system.timeout-ms = System check timeout (default: 2000ms)
 * - health.check.system.skip-disk-check = Skip disk space checks (default: false)
 * - health.check.system.disk-check-interval-seconds = Disk check cache interval (default: 300)
 */
public class SystemHealthCheck extends HealthCheckBase {
    
    // Cached static values (initialization optimization)
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String JAVA_VERSION = System.getProperty("java.version");
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getOperatingSystemMXBean();
    private static final int AVAILABLE_PROCESSORS = RUNTIME.availableProcessors();
    
    // Cache for disk space checks with timestamp
    private static volatile long lastDiskCheckTime = 0;
    private static volatile String cachedDiskInfo = null;
    
    @Override
    protected CheckResult performCheck() throws Exception {
        return measureExecution(() -> {
            // Check basic system health indicators
            if (!checkSystemHealth()) {
                throw new Exception("System health check failed");
            }
            
            // Build status message
            StringBuilder message = new StringBuilder("System OK");
            
            // Add processor info
            message.append(" - Processors: ").append(AVAILABLE_PROCESSORS);
            
            // Add Java version
            message.append(", Java: ").append(JAVA_VERSION);
            
            // Add OS info
            message.append(", OS: ").append(OS_NAME);
            
            // Add disk space info (cached for performance)
            boolean skipDiskCheck = getConfigProperty("skip-disk-check", false);
            if (!skipDiskCheck) {
                String diskInfo = getCachedDiskInfo();
                if (diskInfo != null) {
                    message.append(", ").append(diskInfo);
                }
            }
            
            return message.toString();
        });
    }
    
    private boolean checkSystemHealth() {
        try {
            // Basic system availability checks
            // These are lightweight and indicate core system problems
            
            // Check if we can access basic system properties
            if (AVAILABLE_PROCESSORS <= 0) {
                return false;
            }
            
            // Check if memory information is accessible
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            if (memoryBean == null || memoryBean.getHeapMemoryUsage() == null) {
                return false;
            }
            
            // System is responsive if we get here
            return true;
            
        } catch (Exception e) {
            Logger.debug(this, "System health check failed: " + e.getMessage());
            return false;
        }
    }
    
    private String getCachedDiskInfo() {
        long currentTime = System.currentTimeMillis();
        int diskCheckIntervalSeconds = getConfigProperty("disk-check-interval-seconds", 300);
        long checkIntervalMs = diskCheckIntervalSeconds * 1000L;
        
        // Use cached value if still fresh
        if (cachedDiskInfo != null && (currentTime - lastDiskCheckTime) < checkIntervalMs) {
            return cachedDiskInfo;
        }
        
        try {
            // Update disk space info
            File root = new File("/");
            long freeSpace = root.getFreeSpace() / (1024 * 1024); // Convert to MB
            
            cachedDiskInfo = "Free disk: " + freeSpace + " MB";
            lastDiskCheckTime = currentTime;
            
            return cachedDiskInfo;
            
        } catch (Exception e) {
            Logger.debug(this, "Failed to get disk space info: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getName() {
        return "system";
    }
    
    @Override
    public int getOrder() {
        return 20; // High priority - core system check
    }
    
    /**
     * Safe for liveness - only checks internal system state
     */
    @Override
    public boolean isLivenessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    /**
     * Essential for readiness - system must be healthy to serve traffic
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        HealthCheckMode mode = getMode();
        boolean skipDiskCheck = getConfigProperty("skip-disk-check", false);
        int diskCheckInterval = getConfigProperty("disk-check-interval-seconds", 300);
        
        return String.format("Monitors core system resources%s (disk cache: %ds) (Mode: %s)", 
            skipDiskCheck ? " (no disk)" : "", diskCheckInterval, mode.name());
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Always include system information that appears in messages
        data.put("processors", AVAILABLE_PROCESSORS);
        data.put("javaVersion", JAVA_VERSION);
        data.put("osName", OS_NAME);
        
        // Include disk space information if not skipped
        boolean skipDiskCheck = getConfigProperty("skip-disk-check", false);
        if (!skipDiskCheck) {
            try {
                File root = new File("/");
                long freeSpaceMB = root.getFreeSpace() / (1024 * 1024);
                data.put("freeDiskMB", freeSpaceMB);
            } catch (Exception e) {
                Logger.debug(this, "Failed to get disk space for structured data: " + e.getMessage());
            }
        }
        
        // Include error type for system-related failures
        if (result.error != null) {
            data.put("errorType", "system_resources");
        }
        
        return data;
    }
}