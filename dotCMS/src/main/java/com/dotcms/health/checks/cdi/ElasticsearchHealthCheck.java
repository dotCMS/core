package com.dotcms.health.checks.cdi;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

/**
 * CDI-based health check for Elasticsearch availability.
 * This check is marked as ReadinessCheck only since search issues should not
 * trigger pod restarts, just remove the instance from load balancing.
 * 
 * Extends HealthCheckBase to get DEGRADED_SAFE mode logic and tolerance integration.
 * This performs real connectivity tests to verify Elasticsearch is responding.
 */
@ApplicationScoped
public class ElasticsearchHealthCheck extends HealthCheckBase {
    
    @Override
    protected CheckResult performCheck() throws Exception {
        // Skip expensive Elasticsearch operations during shutdown
        if (isShutdownInProgress()) {
            Logger.debug(this, "Skipping Elasticsearch connectivity test during shutdown");
            return new CheckResult(false, 0L, "Elasticsearch health check skipped during shutdown to avoid network calls while search services are shutting down");
        }
        
        // Use the utility method for consistent timing and error handling
        return measureExecution(() -> {
            // Test actual Elasticsearch connectivity
            boolean isHealthy = testElasticsearchConnectivity();
            
            if (isHealthy) {
                return "Elasticsearch API available";
            } else {
                throw new Exception("Elasticsearch API not available");
            }
        });
    }
    
    /**
     * Tests actual Elasticsearch connectivity by performing a cluster health check.
     * This makes a real network call to verify Elasticsearch is responding.
     */
    private boolean testElasticsearchConnectivity() {
        try {
            // Get the ES API
            var esAPI = APILocator.getESIndexAPI();
            if (esAPI == null) {
                return false;
            }
            
            // Perform a basic cluster health check - this makes a real network call
            var clusterStats = esAPI.getClusterStats();
            
            // If we get here without exception, Elasticsearch is responding
            return clusterStats != null;
            
        } catch (Exception e) {
            // Any exception means Elasticsearch is not responding properly
            Logger.debug(this, "Elasticsearch cluster health check failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getName() {
        return "elasticsearch";
    }
    
    @Override
    protected HealthCheckMode getDefaultMode() {
        return HealthCheckMode.MONITOR_MODE;
    }
    
    @Override
    public int getOrder() {
        return 40; // Medium priority - search dependency
    }
    
    /**
     * NOT safe for liveness - search issues should not trigger restarts
     */
    @Override
    public boolean isLivenessCheck() {
        return false;
    }
    
    /**
     * Important for readiness - search functionality is critical for content operations
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        return "Verifies Elasticsearch API availability and connectivity";
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Include API availability status
        data.put("apiAvailable", originalStatus == HealthStatus.UP);
        
        // Include response timing since it's relevant for performance monitoring
        if (result.durationMs > 0) {
            data.put("responseTimeMs", result.durationMs);
        }
        
        // Include timeout configuration for monitoring
        int timeoutMs = getTimeoutMs();
        data.put("timeoutMs", timeoutMs);
        
        // Include error type for Elasticsearch-related failures
        if (result.error != null) {
            data.put("errorType", "elasticsearch_connectivity");
        }
        
        return data;
    }
} 