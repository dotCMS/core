package com.dotcms.health.checks.cdi;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotcms.health.util.HealthCheckUtils;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;
import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

/**
 * CDI-based health check for cache availability and performance.
 * 
 * This health check can block and take time if the cache is slow or unavailable.
 * That's intentional - slow cache = slow health check = accurate monitoring.
 * 
 * The HealthStateManager ensures these checks run in background threads so
 * health endpoints never block, while health check execution reflects reality.
 * 
 * Configuration Properties:
 * - health.check.cache.mode = Safety mode (PRODUCTION, DEGRADED_SAFE, MONITORING_ONLY, DISABLED)
 * - health.check.cache.timeout-ms = Cache operation timeout (default: 2000ms)
 * - health.check.cache.test-key = Cache key for testing (default: health-check-cache-test)
 * - health.check.cache.test-group = Cache group for testing (default: health.check)
 */
@ApplicationScoped
public class CacheHealthCheck extends HealthCheckBase {
    
    @Override
    protected CheckResult performCheck() throws Exception {
        // Skip cache operations during shutdown to avoid accessing cache systems that are shutting down
        if (isShutdownInProgress()) {
            Logger.debug(this, "Skipping cache operations test during shutdown");
            return new CheckResult(false, 0L, "Cache health check skipped during shutdown to avoid cache operations while cache systems are shutting down");
        }
        
        int timeoutMs = getTimeoutMs();
        String testKey = getConfigProperty("test-key", "health-check-cache-test");
        String testGroup = getConfigProperty("test-group", "health.check");
        
        // Use utility for the actual cache test
        // This can block and be slow - that accurately reflects cache performance
        return measureExecution(() -> 
            testCacheOperations(testKey, testGroup, timeoutMs)
        );
    }
    
    @Override
    public String getName() {
        return "cache";
    }
    
    @Override
    protected HealthCheckMode getDefaultMode() {
        return HealthCheckMode.MONITOR_MODE;
    }
    
    @Override
    public int getOrder() {
        return 30; // Medium priority - dependency check
    }
    
    /**
     * NOT safe for liveness - cache issues should not trigger pod restarts
     */
    @Override
    public boolean isLivenessCheck() {
        return false;
    }
    
    /**
     * Essential for readiness - application performance degrades without cache
     * In DEGRADED_SAFE mode, will never fail readiness probes
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        HealthCheckMode mode = getMode();
        int timeoutMs = getTimeoutMs();
        String testGroup = getConfigProperty("test-group", "health.check");
        
        return String.format("Verifies cache operations with %dms timeout using group '%s' (Mode: %s)", 
            timeoutMs, testGroup, mode.name());
    }

    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Include cache operation timing since it's relevant for performance monitoring
        if (result.durationMs > 0) {
            data.put("operationTimeMs", result.durationMs);
        }
        
        // Include timeout configuration for monitoring
        int timeoutMs = getTimeoutMs();
        data.put("timeoutMs", timeoutMs);
        
        // Include test configuration
        String testGroup = getConfigProperty("test-group", "health.check");
        data.put("testGroup", testGroup);
        
        // Include error type for cache-related failures
        if (result.error != null) {
            data.put("errorType", "cache_operations");
        }
        
        return data;
    }

    /**
     * Tests cache functionality with a round-trip write/read/delete operation.
     *
     * @param testKey the key to use for testing
     * @param testGroup the cache group to use
     * @param timeoutMs timeout in milliseconds
     * @return descriptive message about the cache status
     * @throws Exception if cache operations fail
     */
    private  String testCacheOperations(String testKey, String testGroup, long timeoutMs) throws Exception {
        return HealthCheckUtils.executeWithTimeout(() -> {
            DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
            String testValue = "health-check-" + System.currentTimeMillis();

            // Test write
            cache.put(testKey, testValue, testGroup);

            // Test read
            String cachedValue = (String) cache.get(testKey, testGroup);

            // Test delete
            cache.remove(testKey, testGroup);

            if (testValue.equals(cachedValue)) {
                return "Cache write/read/delete successful";
            } else {
                throw new Exception("Cache read returned unexpected value");
            }
        }, timeoutMs, "Cache operations test");
    }
} 