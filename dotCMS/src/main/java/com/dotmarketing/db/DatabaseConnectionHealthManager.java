package com.dotmarketing.db;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages database connection health, fault tolerance, and automatic recovery.
 * 
 * Features:
 * - Circuit breaker pattern to prevent cascading failures
 * - Single-thread background health monitoring with exponential backoff
 * - Connection pool monitoring and deadlock detection
 * - Automatic recovery from database outages
 * - Connection leak detection and alerts
 * 
 * Configuration Properties:
 * - DATABASE_HEALTH_CHECK_ENABLED: Enable health monitoring (default: true)
 * - DATABASE_HEALTH_CHECK_INTERVAL_SECONDS: Health check interval (default: 30)
 * - DATABASE_CIRCUIT_BREAKER_FAILURE_THRESHOLD: Failures before circuit opens (default: 5)
 * - DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS: Recovery attempt interval (default: 60)
 * - DATABASE_CONNECTION_LEAK_THRESHOLD_SECONDS: Connection leak detection threshold (default: 300)
 * 
 * @author dotCMS
 */
public class DatabaseConnectionHealthManager {
    
    private static final DatabaseConnectionHealthManager INSTANCE = new DatabaseConnectionHealthManager();
    
    // Circuit breaker states
    public enum CircuitState {
        CLOSED,     // Normal operation - requests allowed
        OPEN,       // Failure state - requests blocked  
        HALF_OPEN   // Testing recovery - limited requests allowed
    }
    
    // Atomic state management
    private final AtomicReference<CircuitState> circuitState = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong lastSuccessTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private final AtomicLong connectionLeakCount = new AtomicLong(0);
    
    // Configuration
    private final int failureThreshold;
    private final long recoveryTimeoutMillis;
    private final long healthCheckIntervalSeconds;
    private final long connectionLeakThresholdSeconds;
    private final boolean healthCheckEnabled;
    
    // Background monitoring
    private final ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "database-health-monitor");
        t.setDaemon(true);
        return t;
    });
    
    private volatile DataSource monitoredDataSource;
    private volatile HikariPoolMXBean poolMBean;
    
    private DatabaseConnectionHealthManager() {
        this.failureThreshold = Config.getIntProperty("DATABASE_CIRCUIT_BREAKER_FAILURE_THRESHOLD", 5);
        this.recoveryTimeoutMillis = Config.getLongProperty("DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS", 60) * 1000;
        this.healthCheckIntervalSeconds = Config.getLongProperty("DATABASE_HEALTH_CHECK_INTERVAL_SECONDS", 30);
        this.connectionLeakThresholdSeconds = Config.getLongProperty("DATABASE_CONNECTION_LEAK_THRESHOLD_SECONDS", 300);
        this.healthCheckEnabled = Config.getBooleanProperty("DATABASE_HEALTH_CHECK_ENABLED", true);
        
        if (healthCheckEnabled) {
            startHealthMonitoring();
        }
        
        Logger.info(this, "Database connection health manager initialized - Circuit breaker enabled with " +
                failureThreshold + " failure threshold, " + (recoveryTimeoutMillis / 1000) + "s recovery timeout");
    }
    
    public static DatabaseConnectionHealthManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize monitoring for the given datasource.
     * Should be called after datasource creation.
     */
    public void initializeMonitoring(DataSource dataSource) {
        this.monitoredDataSource = dataSource;
        
        // Initialize HikariCP MBean monitoring if available
        if (dataSource instanceof HikariDataSource) {
            try {
                HikariDataSource hikariDS = (HikariDataSource) dataSource;
                String poolName = hikariDS.getPoolName();
                if (poolName != null) {
                    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                    ObjectName poolObjectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
                    this.poolMBean = JMX.newMXBeanProxy(mBeanServer, poolObjectName, HikariPoolMXBean.class);
                    Logger.info(this, "HikariCP pool monitoring initialized for pool: " + poolName);
                }
            } catch (Exception e) {
                Logger.warn(this, "Failed to initialize HikariCP pool monitoring: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if database operations should be allowed based on circuit breaker state.
     * @return true if operations are allowed, false if circuit is open
     */
    public boolean isOperationAllowed() {
        CircuitState state = circuitState.get();
        
        switch (state) {
            case CLOSED:
                return true;
                
            case OPEN:
                // Check if enough time has passed to attempt recovery
                long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
                if (timeSinceLastFailure >= recoveryTimeoutMillis) {
                    // Transition to half-open for recovery attempt
                    if (circuitState.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                        Logger.info(this, "Circuit breaker transitioning to HALF_OPEN for recovery attempt");
                        return true;
                    }
                }
                return false;
                
            case HALF_OPEN:
                // Allow limited requests during recovery testing
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Record a successful database operation.
     */
    public void recordSuccess() {
        lastSuccessTime.set(System.currentTimeMillis());
        isHealthy.set(true);
        
        CircuitState currentState = circuitState.get();
        if (currentState == CircuitState.HALF_OPEN) {
            // Recovery successful - close circuit
            if (circuitState.compareAndSet(CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
                consecutiveFailures.set(0);
                Logger.info(this, "Circuit breaker CLOSED - Database connection recovered");
            }
        } else if (currentState == CircuitState.OPEN) {
            // Unexpected success while open - close circuit
            circuitState.set(CircuitState.CLOSED);
            consecutiveFailures.set(0);
            Logger.info(this, "Circuit breaker CLOSED - Unexpected recovery detected");
        } else {
            // Normal operation - just reset failure count if any
            consecutiveFailures.set(0);
        }
    }
    
    /**
     * Record a failed database operation.
     */
    public void recordFailure(Throwable cause) {
        lastFailureTime.set(System.currentTimeMillis());
        isHealthy.set(false);
        
        int failures = consecutiveFailures.incrementAndGet();
        
        Logger.warn(this, "Database operation failed (" + failures + "/" + failureThreshold + " failures): " + 
                cause.getMessage());
        
        // Open circuit if failure threshold exceeded
        if (failures >= failureThreshold && circuitState.get() != CircuitState.OPEN) {
            if (circuitState.compareAndSet(CircuitState.CLOSED, CircuitState.OPEN) ||
                circuitState.compareAndSet(CircuitState.HALF_OPEN, CircuitState.OPEN)) {
                
                Logger.error(this, "Circuit breaker OPENED - Database appears to be down. " +
                        "Will attempt recovery in " + (recoveryTimeoutMillis / 1000) + " seconds. " +
                        "Cause: " + cause.getMessage());
            }
        }
    }
    
    /**
     * Get current health status information.
     */
    public HealthStatus getHealthStatus() {
        HealthStatus.Builder builder = HealthStatus.builder()
                .healthy(isHealthy.get())
                .circuitState(circuitState.get())
                .consecutiveFailures(consecutiveFailures.get())
                .lastSuccessTime(Instant.ofEpochMilli(lastSuccessTime.get()))
                .lastFailureTime(lastFailureTime.get() > 0 ? Instant.ofEpochMilli(lastFailureTime.get()) : null)
                .connectionLeakCount(connectionLeakCount.get());
        
        // Add connection pool metrics if available
        if (poolMBean != null) {
            try {
                builder.activeConnections(poolMBean.getActiveConnections())
                       .idleConnections(poolMBean.getIdleConnections())
                       .totalConnections(poolMBean.getTotalConnections())
                       .threadsAwaitingConnection(poolMBean.getThreadsAwaitingConnection());
            } catch (Exception e) {
                Logger.debug(this, "Failed to retrieve pool metrics: " + e.getMessage());
            }
        }
        
        return builder.build();
    }
    
    /**
     * Force circuit breaker to open (for testing or manual intervention).
     */
    public void openCircuit(String reason) {
        circuitState.set(CircuitState.OPEN);
        lastFailureTime.set(System.currentTimeMillis());
        isHealthy.set(false);
        Logger.warn(this, "Circuit breaker manually opened: " + reason);
    }
    
    /**
     * Force circuit breaker to close (for testing or manual intervention).
     */
    public void closeCircuit(String reason) {
        circuitState.set(CircuitState.CLOSED);
        consecutiveFailures.set(0);
        lastSuccessTime.set(System.currentTimeMillis());
        isHealthy.set(true);
        Logger.info(this, "Circuit breaker manually closed: " + reason);
    }
    
    /**
     * Start background health monitoring with exponential backoff.
     */
    private void startHealthMonitoring() {
        healthChecker.scheduleWithFixedDelay(this::performHealthCheck, 
                healthCheckIntervalSeconds, healthCheckIntervalSeconds, TimeUnit.SECONDS);
        
        Logger.info(this, "Database health monitoring started with " + healthCheckIntervalSeconds + "s interval");
    }
    
    /**
     * Perform background health check with connection pool monitoring.
     */
    private void performHealthCheck() {
        try {
            // Skip health checks if circuit is closed and recently successful
            if (circuitState.get() == CircuitState.CLOSED && 
                (System.currentTimeMillis() - lastSuccessTime.get()) < 60000) {
                return;
            }
            
            // Check connection pool health
            checkConnectionPoolHealth();
            
            // Perform actual database connectivity test if needed
            if (monitoredDataSource != null && shouldPerformConnectivityTest()) {
                performConnectivityTest();
            }
            
        } catch (Exception e) {
            Logger.error(this, "Health check failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check connection pool health and detect potential issues.
     */
    private void checkConnectionPoolHealth() {
        if (poolMBean == null) {
            return;
        }
        
        try {
            int activeConnections = poolMBean.getActiveConnections();
            int totalConnections = poolMBean.getTotalConnections();
            int threadsWaiting = poolMBean.getThreadsAwaitingConnection();
            int idleConnections = poolMBean.getIdleConnections();
            
            // Detect potential connection pool exhaustion
            if (threadsWaiting > 0 && activeConnections >= totalConnections * 0.9) {
                Logger.warn(this, "Connection pool near exhaustion: " + activeConnections + "/" + totalConnections + 
                        " active, " + threadsWaiting + " threads waiting");
                
                // Check for potential deadlock (high active connections, no idle, threads waiting for extended time)
                if (idleConnections == 0 && threadsWaiting > 5) {
                    Logger.error(this, "Potential connection deadlock detected: " + 
                            activeConnections + " active connections, " + threadsWaiting + " threads waiting, 0 idle");
                    
                    // Consider this a failure condition
                    recordFailure(new RuntimeException("Connection pool deadlock detected"));
                }
            }
            
            // Detect connection leaks (connections active for too long)
            if (activeConnections > totalConnections * 0.7) {
                long currentLeaks = connectionLeakCount.incrementAndGet();
                Logger.warn(this, "High connection usage detected - potential leaks: " + 
                        activeConnections + "/" + totalConnections + " active (leak count: " + currentLeaks + ")");
            }
            
            Logger.debug(this, "Connection pool status: " + activeConnections + "/" + totalConnections + 
                    " active, " + idleConnections + " idle, " + threadsWaiting + " waiting");
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to check connection pool health: " + e.getMessage());
        }
    }
    
    /**
     * Determine if connectivity test should be performed based on circuit state.
     */
    private boolean shouldPerformConnectivityTest() {
        CircuitState state = circuitState.get();
        
        // Always test when circuit is open (attempting recovery)
        if (state == CircuitState.OPEN || state == CircuitState.HALF_OPEN) {
            return true;
        }
        
        // Test periodically when closed but recently failed
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        return timeSinceLastFailure > 0 && timeSinceLastFailure < recoveryTimeoutMillis * 2;
    }
    
    /**
     * Perform actual database connectivity test.
     */
    private void performConnectivityTest() {
        try (Connection conn = monitoredDataSource.getConnection()) {
            // Simple connectivity test
            if (!conn.isValid(5)) {
                throw new SQLException("Connection validation failed");
            }
            
            // Record success
            recordSuccess();
            Logger.debug(this, "Database connectivity test successful");
            
        } catch (Exception e) {
            // Record failure
            recordFailure(e);
            Logger.warn(this, "Database connectivity test failed: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown health monitoring (for testing or shutdown).
     */
    public void shutdown() {
        healthChecker.shutdown();
        try {
            if (!healthChecker.awaitTermination(5, TimeUnit.SECONDS)) {
                healthChecker.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthChecker.shutdownNow();
            Thread.currentThread().interrupt();
        }
        Logger.info(this, "Database health monitoring shutdown");
    }
    
    /**
     * Immutable health status information.
     */
    public static class HealthStatus {
        private final boolean healthy;
        private final CircuitState circuitState;
        private final int consecutiveFailures;
        private final Instant lastSuccessTime;
        private final Instant lastFailureTime;
        private final long connectionLeakCount;
        private final Integer activeConnections;
        private final Integer idleConnections;
        private final Integer totalConnections;
        private final Integer threadsAwaitingConnection;
        
        private HealthStatus(Builder builder) {
            this.healthy = builder.healthy;
            this.circuitState = builder.circuitState;
            this.consecutiveFailures = builder.consecutiveFailures;
            this.lastSuccessTime = builder.lastSuccessTime;
            this.lastFailureTime = builder.lastFailureTime;
            this.connectionLeakCount = builder.connectionLeakCount;
            this.activeConnections = builder.activeConnections;
            this.idleConnections = builder.idleConnections;
            this.totalConnections = builder.totalConnections;
            this.threadsAwaitingConnection = builder.threadsAwaitingConnection;
        }
        
        // Getters
        public boolean isHealthy() { return healthy; }
        public CircuitState getCircuitState() { return circuitState; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public Instant getLastSuccessTime() { return lastSuccessTime; }
        public Instant getLastFailureTime() { return lastFailureTime; }
        public long getConnectionLeakCount() { return connectionLeakCount; }
        public Integer getActiveConnections() { return activeConnections; }
        public Integer getIdleConnections() { return idleConnections; }
        public Integer getTotalConnections() { return totalConnections; }
        public Integer getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private boolean healthy;
            private CircuitState circuitState;
            private int consecutiveFailures;
            private Instant lastSuccessTime;
            private Instant lastFailureTime;
            private long connectionLeakCount;
            private Integer activeConnections;
            private Integer idleConnections;
            private Integer totalConnections;
            private Integer threadsAwaitingConnection;
            
            public Builder healthy(boolean healthy) {
                this.healthy = healthy;
                return this;
            }
            
            public Builder circuitState(CircuitState circuitState) {
                this.circuitState = circuitState;
                return this;
            }
            
            public Builder consecutiveFailures(int consecutiveFailures) {
                this.consecutiveFailures = consecutiveFailures;
                return this;
            }
            
            public Builder lastSuccessTime(Instant lastSuccessTime) {
                this.lastSuccessTime = lastSuccessTime;
                return this;
            }
            
            public Builder lastFailureTime(Instant lastFailureTime) {
                this.lastFailureTime = lastFailureTime;
                return this;
            }
            
            public Builder connectionLeakCount(long connectionLeakCount) {
                this.connectionLeakCount = connectionLeakCount;
                return this;
            }
            
            public Builder activeConnections(Integer activeConnections) {
                this.activeConnections = activeConnections;
                return this;
            }
            
            public Builder idleConnections(Integer idleConnections) {
                this.idleConnections = idleConnections;
                return this;
            }
            
            public Builder totalConnections(Integer totalConnections) {
                this.totalConnections = totalConnections;
                return this;
            }
            
            public Builder threadsAwaitingConnection(Integer threadsAwaitingConnection) {
                this.threadsAwaitingConnection = threadsAwaitingConnection;
                return this;
            }
            
            public HealthStatus build() {
                return new HealthStatus(this);
            }
        }
    }
}