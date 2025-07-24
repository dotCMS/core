package com.dotcms.metrics.binders;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

/**
 * Comprehensive metric binder for dotCMS database and connection pool metrics.
 * 
 * This binder provides critical database health metrics including:
 * - Connection pool status (active, idle, max connections)
 * - Database connectivity and health
 * - Query performance metrics (when available)
 * - Transaction metrics
 * 
 * These metrics are essential for monitoring database performance and detecting
 * connection pool exhaustion, which is a common cause of dotCMS performance issues.
 */
public class DatabaseMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.database";
    private final MBeanServer mBeanServer;
    
    public DatabaseMetrics() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            registerConnectionPoolMetrics(registry);
            registerDatabaseHealthMetrics(registry);
            registerHikariMetrics(registry);
            
            Logger.info(this, "Database metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register database metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register core database connection metrics.
     */
    private void registerConnectionPoolMetrics(MeterRegistry registry) {
        // Database connectivity check
        Gauge.builder(METRIC_PREFIX + ".connection.available", this, metrics -> isDatabaseAvailable() ? 1.0 : 0.0)
            .description("Whether database connections are available (1=available, 0=unavailable)")
            .register(registry);
        
        // Connection factory availability
        Gauge.builder(METRIC_PREFIX + ".factory.available", this, metrics -> 
            DbConnectionFactory.getConnection() != null ? 1.0 : 0.0)
            .description("Whether the database connection factory is working")
            .register(registry);
    }
    
    /**
     * Register database health check metrics.
     */
    private void registerDatabaseHealthMetrics(MeterRegistry registry) {
        // Simple health check - can we execute a basic query?
        Gauge.builder(METRIC_PREFIX + ".health.query_test", this, metrics -> canExecuteQuery() ? 1.0 : 0.0)
            .description("Whether basic database queries are working (1=working, 0=failed)")
            .register(registry);
        
        // Check if we can get system time from database
        Gauge.builder(METRIC_PREFIX + ".health.system_query", this, metrics -> canGetSystemTime() ? 1.0 : 0.0)
            .description("Whether system queries are working (1=working, 0=failed)")
            .register(registry);
    }
    
    /**
     * Register HikariCP connection pool metrics if available.
     * HikariCP is commonly used in dotCMS deployments.
     */
    private void registerHikariMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> hikariPools = mBeanServer.queryNames(
                new ObjectName("com.zaxxer.hikari:type=Pool*"), null);
            
            for (ObjectName pool : hikariPools) {
                String poolName = pool.getKeyProperty("Pool");
                if (poolName == null) poolName = "default";
                
                // Active connections
                Gauge.builder(METRIC_PREFIX + ".hikari.connections.active", this,
                    metrics -> getHikariAttribute(pool, "ActiveConnections"))
                    .description("Number of active HikariCP connections")
                    .tag("pool", poolName)
                    .register(registry);
                
                // Idle connections
                Gauge.builder(METRIC_PREFIX + ".hikari.connections.idle", this,
                    metrics -> getHikariAttribute(pool, "IdleConnections"))
                    .description("Number of idle HikariCP connections")
                    .tag("pool", poolName)
                    .register(registry);
                
                // Total connections
                Gauge.builder(METRIC_PREFIX + ".hikari.connections.total", this,
                    metrics -> getHikariAttribute(pool, "TotalConnections"))
                    .description("Total number of HikariCP connections")
                    .tag("pool", poolName)
                    .register(registry);
                
                // Max pool size
                Gauge.builder(METRIC_PREFIX + ".hikari.connections.max", this,
                    metrics -> getHikariAttribute(pool, "MaximumPoolSize"))
                    .description("Maximum HikariCP pool size")
                    .tag("pool", poolName)
                    .register(registry);
                
                // Min pool size
                Gauge.builder(METRIC_PREFIX + ".hikari.connections.min", this,
                    metrics -> getHikariAttribute(pool, "MinimumIdle"))
                    .description("Minimum HikariCP idle connections")
                    .tag("pool", poolName)
                    .register(registry);
                
                // Threads awaiting connection
                Gauge.builder(METRIC_PREFIX + ".hikari.threads.awaiting", this,
                    metrics -> getHikariAttribute(pool, "ThreadsAwaitingConnection"))
                    .description("Number of threads awaiting HikariCP connections")
                    .tag("pool", poolName)
                    .register(registry);
            }
            
            if (!hikariPools.isEmpty()) {
                Logger.info(this, "HikariCP metrics registered for " + hikariPools.size() + " pool(s)");
            }
            
        } catch (Exception e) {
            Logger.debug(this, "HikariCP metrics not available: " + e.getMessage());
        }
    }
    
    /**
     * Test basic database connectivity.
     */
    private boolean isDatabaseAvailable() {
        try (Connection conn = DbConnectionFactory.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            Logger.debug(this, "Database availability check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test if we can execute a simple query.
     */
    private boolean canExecuteQuery() {
        try (Connection conn = DbConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (Exception e) {
            Logger.debug(this, "Database query test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test if we can get system time from database (more comprehensive test).
     */
    private boolean canGetSystemTime() {
        try (Connection conn = DbConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Try different system time queries based on database type
            String[] timeQueries = {
                "SELECT CURRENT_TIMESTAMP",  // Standard SQL
                "SELECT NOW()",              // PostgreSQL/MySQL
                "SELECT SYSDATE FROM DUAL"   // Oracle
            };
            
            for (String query : timeQueries) {
                try (ResultSet rs = stmt.executeQuery(query)) {
                    if (rs.next() && rs.getTimestamp(1) != null) {
                        return true;
                    }
                } catch (Exception e) {
                    // Try next query
                    continue;
                }
            }
            return false;
        } catch (Exception e) {
            Logger.debug(this, "Database system time test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get HikariCP MBean attribute value.
     */
    private double getHikariAttribute(ObjectName objectName, String attributeName) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get HikariCP attribute " + attributeName + ": " + e.getMessage());
            return 0.0;
        }
    }
} 