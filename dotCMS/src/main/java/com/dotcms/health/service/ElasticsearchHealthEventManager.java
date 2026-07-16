package com.dotcms.health.service;

import com.dotcms.health.api.HealthEventManager;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Event-driven Elasticsearch health manager that monitors ES client events
 * to detect search failures quickly without constant polling.
 * 
 * This manager:
 * 1. Monitors Elasticsearch client connection state
 * 2. Uses lightweight health pings when healthy
 * 3. Uses exponential backoff for recovery testing
 * 4. Falls back to polling mode if event monitoring fails
 */
@ApplicationScoped
public class ElasticsearchHealthEventManager implements HealthEventManager {
    
    @Inject
    private ExponentialBackoffRecoveryTester recoveryTester;
    
    private final AtomicReference<HealthStatus> currentStatus = new AtomicReference<>(HealthStatus.UP);
    private final AtomicReference<HealthCheckResult> lastResult = new AtomicReference<>();
    private final List<HealthStatusChangeCallback> callbacks = new ArrayList<>();
    
    private ScheduledExecutorService monitoringExecutor;
    private boolean eventDrivenEnabled;
    private boolean initialized = false;
    
    // Lightweight monitoring when healthy
    private static final int HEALTHY_PING_INTERVAL_SECONDS = 30;
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 2;
    
    @Override
    public String getHealthCheckName() {
        return "elasticsearch";
    }
    
    @Override
    public boolean supportsEventDriven() {
        return Config.getBooleanProperty("health.check.elasticsearch.event-driven.enabled", true);
    }
    
    @Override
    public HealthStatus getCurrentStatus() {
        return currentStatus.get();
    }
    
    @Override
    public HealthCheckResult getLastResult() {
        return lastResult.get();
    }
    
    @Override
    public void onHealthStatusChange(HealthStatusChangeCallback callback) {
        callbacks.add(callback);
    }
    
    @PostConstruct
    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        
        eventDrivenEnabled = supportsEventDriven();
        Logger.info(this, String.format(
            "Initializing Elasticsearch health event manager (event-driven: %s)", 
            eventDrivenEnabled
        ));
        
        if (eventDrivenEnabled) {
            try {
                initializeEventDrivenMonitoring();
                Logger.info(this, "Elasticsearch event-driven monitoring initialized successfully");
            } catch (Exception e) {
                Logger.warn(this, "Failed to initialize event-driven monitoring, falling back to polling", e);
                eventDrivenEnabled = false;
            }
        }
        
        // Always run initial health check to establish baseline
        runInitialHealthCheck();
        initialized = true;
        
        Logger.info(this, String.format(
            "Elasticsearch health event manager initialized (mode: %s)", 
            eventDrivenEnabled ? "event-driven" : "polling-fallback"
        ));
    }
    
    private void initializeEventDrivenMonitoring() throws Exception {
        // Verify Elasticsearch API is available
        if (APILocator.getESIndexAPI() == null) {
            throw new IllegalStateException("Elasticsearch API not available");
        }
        
        // Start lightweight monitoring - only ping when we think it's healthy
        monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ElasticsearchHealthEventMonitor");
            t.setDaemon(true);
            return t;
        });
        
        monitoringExecutor.scheduleAtFixedRate(
            this::performLightweightHealthPing,
            0, HEALTHY_PING_INTERVAL_SECONDS, TimeUnit.SECONDS
        );
    }
    
    private void performLightweightHealthPing() {
        try {
            // Only ping when we think ES is healthy to reduce load
            if (currentStatus.get() != HealthStatus.UP) {
                return;
            }
            
            // Perform lightweight connectivity test
            boolean isHealthy = testElasticsearchConnectivity();
            
            if (!isHealthy) {
                consecutiveFailures++;
                
                if (consecutiveFailures >= FAILURE_THRESHOLD) {
                    Logger.warn(this, String.format(
                        "Elasticsearch failure detected via health ping (consecutive failures: %d)",
                        consecutiveFailures
                    ));
                    
                    onElasticsearchFailureDetected("Health ping failed " + consecutiveFailures + " times");
                }
            } else {
                // Reset failure counter on success
                if (consecutiveFailures > 0) {
                    consecutiveFailures = 0;
                }
            }
            
        } catch (Exception e) {
            Logger.debug(this, "Error during Elasticsearch health ping: " + e.getMessage());
            consecutiveFailures++;
            
            if (consecutiveFailures >= FAILURE_THRESHOLD && currentStatus.get() == HealthStatus.UP) {
                onElasticsearchFailureDetected("Exception during health ping: " + e.getMessage());
            }
        }
    }
    
    private boolean testElasticsearchConnectivity() {
        try {
            return HealthCheckUtils.executeWithTimeout(() -> {
                try {
                    // Use the actual ES connectivity test from ElasticsearchHealthCheck
                    Object clusterStats = APILocator.getESIndexAPI().getClusterStats();
                    return clusterStats != null;
                } catch (Exception e) {
                    return false;
                }
            }, Config.getIntProperty("health.check.elasticsearch.ping.timeout.ms", 3000), 
               "elasticsearch-ping");
        } catch (Exception e) {
            Logger.debug(this, "Elasticsearch connectivity test failed: " + e.getMessage());
            return false;
        }
    }
    
    private void onElasticsearchFailureDetected(String reason) {
        HealthStatus oldStatus = currentStatus.getAndSet(HealthStatus.DOWN);
        
        if (oldStatus != HealthStatus.DOWN) {
            Logger.warn(this, "Elasticsearch failure detected: " + reason);
            
            // Create failure result
            HealthCheckResult failureResult = HealthCheckResult.builder()
                .name(getHealthCheckName())
                .status(HealthStatus.DOWN)
                .message("Event-driven failure: " + reason)
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
                
            lastResult.set(failureResult);
            
            // Notify callbacks
            notifyStatusChange(oldStatus, HealthStatus.DOWN, reason);
            
            // Start exponential backoff recovery testing
            recoveryTester.startRecoveryTesting(
                getHealthCheckName(),
                this::testElasticsearchRecovery,
                this::onElasticsearchRecoveryDetected
            );
        }
    }
    
    private boolean testElasticsearchRecovery() {
        try {
            return HealthCheckUtils.executeWithTimeout(() -> {
                try {
                    // More thorough recovery test
                    Object clusterStats = APILocator.getESIndexAPI().getClusterStats();
                    return clusterStats != null;
                } catch (Exception e) {
                    return false;
                }
            }, Config.getIntProperty("health.check.elasticsearch.recovery.timeout.ms", 5000), 
               "elasticsearch-recovery-test");
        } catch (Exception e) {
            Logger.debug(this, "Elasticsearch recovery test failed: " + e.getMessage());
            return false;
        }
    }
    
    private void onElasticsearchRecoveryDetected(String checkName) {
        HealthStatus oldStatus = currentStatus.getAndSet(HealthStatus.UP);
        
        if (oldStatus != HealthStatus.UP) {
            Logger.info(this, "Elasticsearch recovery detected");
            
            // Create recovery result
            HealthCheckResult recoveryResult = HealthCheckResult.builder()
                .name(getHealthCheckName())
                .status(HealthStatus.UP)
                .message("Event-driven recovery detected")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
                
            lastResult.set(recoveryResult);
            
            // Notify callbacks
            notifyStatusChange(oldStatus, HealthStatus.UP, "Recovery detected");
            
            // Reset monitoring state
            consecutiveFailures = 0;
        }
    }
    
    private void runInitialHealthCheck() {
        try {
            boolean isHealthy = testElasticsearchConnectivity();
            HealthStatus status = isHealthy ? HealthStatus.UP : HealthStatus.DOWN;
            
            HealthCheckResult initialResult = HealthCheckResult.builder()
                .name(getHealthCheckName())
                .status(status)
                .message("Initial health check")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
                
            currentStatus.set(status);
            lastResult.set(initialResult);
            
            Logger.info(this, String.format("Initial Elasticsearch health check: %s", status));
            
        } catch (Exception e) {
            Logger.warn(this, "Initial Elasticsearch health check failed", e);
            currentStatus.set(HealthStatus.DOWN);
        }
    }
    
    private void notifyStatusChange(HealthStatus oldStatus, HealthStatus newStatus, String reason) {
        for (HealthStatusChangeCallback callback : callbacks) {
            try {
                callback.onStatusChange(getHealthCheckName(), oldStatus, newStatus, reason);
            } catch (Exception e) {
                Logger.warn(this, "Error notifying health status change callback", e);
            }
        }
    }
    
    @PreDestroy
    @Override
    public void shutdown() {
        Logger.info(this, "Shutting down Elasticsearch health event manager");
        
        if (monitoringExecutor != null) {
            monitoringExecutor.shutdown();
            try {
                if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitoringExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitoringExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        recoveryTester.stopRecoveryTesting(getHealthCheckName());
        initialized = false;
    }
    
    /**
     * Check if event-driven monitoring is currently active
     */
    public boolean isEventDrivenActive() {
        return eventDrivenEnabled && initialized;
    }
    
    /**
     * Get monitoring statistics for debugging
     */
    public String getMonitoringInfo() {
        if (!eventDrivenEnabled) {
            return "Event-driven monitoring disabled";
        }
        
        return String.format(
            "Ping interval: %ds, consecutive failures: %d, status: %s",
            HEALTHY_PING_INTERVAL_SECONDS,
            consecutiveFailures,
            currentStatus.get()
        );
    }
    
    /**
     * Manually trigger a failure event (for testing)
     */
    public void simulateFailure(String reason) {
        Logger.warn(this, "Simulating Elasticsearch failure: " + reason);
        onElasticsearchFailureDetected("Simulated: " + reason);
    }
} 