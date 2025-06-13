package com.dotcms.health.service;

import com.dotcms.health.api.HealthEventManager;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckUtils;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Event-driven database health manager that monitors HikariCP connection pool events
 * to detect database failures quickly without constant polling.
 * 
 * This manager:
 * 1. Monitors connection pool metrics via JMX
 * 2. Detects failure patterns (pool exhaustion, connection failures)
 * 3. Uses exponential backoff for recovery testing
 * 4. Falls back to polling mode if event monitoring fails
 */
@ApplicationScoped
public class DatabaseHealthEventManager implements HealthEventManager {
    
    @Inject
    private ExponentialBackoffRecoveryTester recoveryTester;
    
    private final AtomicReference<HealthStatus> currentStatus = new AtomicReference<>(HealthStatus.UP);
    private final AtomicReference<HealthCheckResult> lastResult = new AtomicReference<>();
    private final List<HealthStatusChangeCallback> callbacks = new ArrayList<>();
    
    private ScheduledExecutorService monitoringExecutor;
    private HikariPoolMXBean poolProxy;
    private boolean eventDrivenEnabled;
    private boolean initialized = false;
    
    // Thresholds for failure detection
    private int emptyPoolConsecutiveCount = 0;
    private static final int EMPTY_POOL_FAILURE_THRESHOLD = 3;
    private static final int MONITORING_INTERVAL_SECONDS = 5;
    
    @Override
    public String getHealthCheckName() {
        return "database";
    }
    
    @Override
    public boolean supportsEventDriven() {
        return Config.getBooleanProperty("health.check.database.event-driven.enabled", true);
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
            "Initializing database health event manager (event-driven: %s)", 
            eventDrivenEnabled
        ));
        
        if (eventDrivenEnabled) {
            try {
                initializeEventDrivenMonitoring();
                Logger.info(this, "Database event-driven monitoring initialized successfully");
            } catch (Exception e) {
                Logger.warn(this, "Failed to initialize event-driven monitoring, falling back to polling", e);
                eventDrivenEnabled = false;
            }
        }
        
        // Always run initial health check to establish baseline
        runInitialHealthCheck();
        initialized = true;
        
        Logger.info(this, String.format(
            "Database health event manager initialized (mode: %s)", 
            eventDrivenEnabled ? "event-driven" : "polling-fallback"
        ));
    }
    
    private void initializeEventDrivenMonitoring() throws Exception {
        // Get HikariCP datasource and setup JMX monitoring
        HikariDataSource dataSource = (HikariDataSource) DbConnectionFactory.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("HikariDataSource not available");
        }
        
        // Setup JMX monitoring for connection pool
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        String poolName = dataSource.getPoolName();
        if (poolName == null) {
            poolName = "HikariPool-1"; // Default pool name
        }
        
        ObjectName objectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
        poolProxy = JMX.newMXBeanProxy(server, objectName, HikariPoolMXBean.class);
        
        // Start lightweight monitoring
        monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DatabaseHealthEventMonitor");
            t.setDaemon(true);
            return t;
        });
        
        monitoringExecutor.scheduleAtFixedRate(
            this::monitorConnectionPoolHealth,
            0, MONITORING_INTERVAL_SECONDS, TimeUnit.SECONDS
        );
    }
    
    private void monitorConnectionPoolHealth() {
        try {
            if (poolProxy == null) {
                return;
            }
            
            int activeConnections = poolProxy.getActiveConnections();
            int totalConnections = poolProxy.getTotalConnections();
            int idleConnections = poolProxy.getIdleConnections();
            
            // Detect potential failure patterns
            boolean poolExhausted = (totalConnections > 0 && idleConnections == 0 && activeConnections == totalConnections);
            boolean poolEmpty = (totalConnections == 0 && activeConnections == 0);
            
            if (poolEmpty || poolExhausted) {
                emptyPoolConsecutiveCount++;
                
                if (emptyPoolConsecutiveCount >= EMPTY_POOL_FAILURE_THRESHOLD && currentStatus.get() == HealthStatus.UP) {
                    String reason = poolEmpty ? "Connection pool empty" : "Connection pool exhausted";
                    Logger.warn(this, String.format(
                        "Database failure detected via pool monitoring: %s (active: %d, total: %d, idle: %d)",
                        reason, activeConnections, totalConnections, idleConnections
                    ));
                    
                    onDatabaseFailureDetected(reason);
                }
            } else {
                // Reset failure counter when pool looks healthy
                if (emptyPoolConsecutiveCount > 0) {
                    emptyPoolConsecutiveCount = 0;
                    
                    // If we were previously failed and now pool looks healthy, trigger recovery check
                    if (currentStatus.get() == HealthStatus.DOWN) {
                        Logger.info(this, "Pool health improved, triggering recovery check");
                        testDatabaseRecovery();
                    }
                }
            }
            
        } catch (Exception e) {
            Logger.debug(this, "Error monitoring connection pool health: " + e.getMessage());
            // Don't consider this a database failure - just monitoring failure
        }
    }
    
    private void onDatabaseFailureDetected(String reason) {
        HealthStatus oldStatus = currentStatus.getAndSet(HealthStatus.DOWN);
        
        if (oldStatus != HealthStatus.DOWN) {
            Logger.warn(this, "Database failure detected: " + reason);
            
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
                this::testDatabaseRecovery,
                this::onDatabaseRecoveryDetected
            );
        }
    }
    
    private boolean testDatabaseRecovery() {
        try {
            return HealthCheckUtils.executeWithTimeout(() -> {
                try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
                    return conn.isValid(Config.getIntProperty("health.check.database.validation.timeout.seconds", 2));
                }
            }, Config.getIntProperty("health.check.database.recovery.timeout.ms", 3000), 
               "database-recovery-test");
        } catch (Exception e) {
            Logger.debug(this, "Database recovery test failed: " + e.getMessage());
            return false;
        }
    }
    
    private void onDatabaseRecoveryDetected(String checkName) {
        HealthStatus oldStatus = currentStatus.getAndSet(HealthStatus.UP);
        
        if (oldStatus != HealthStatus.UP) {
            Logger.info(this, "Database recovery detected");
            
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
            emptyPoolConsecutiveCount = 0;
        }
    }
    
    private void runInitialHealthCheck() {
        try {
            boolean isHealthy = testDatabaseRecovery();
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
            
            Logger.info(this, String.format("Initial database health check: %s", status));
            
        } catch (Exception e) {
            Logger.warn(this, "Initial database health check failed", e);
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
        Logger.info(this, "Shutting down database health event manager");
        
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
        
        if (poolProxy == null) {
            return "Pool monitoring not available";
        }
        
        try {
            return String.format(
                "Pool: active=%d, total=%d, idle=%d, failures=%d, status=%s",
                poolProxy.getActiveConnections(),
                poolProxy.getTotalConnections(),
                poolProxy.getIdleConnections(),
                emptyPoolConsecutiveCount,
                currentStatus.get()
            );
        } catch (Exception e) {
            return "Pool monitoring error: " + e.getMessage();
        }
    }
} 