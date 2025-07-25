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
import java.util.HashSet;

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
     * Enhanced HikariCP metrics registration with multiple naming patterns.
     */
    private void registerHikariMetrics(MeterRegistry registry) {
        try {
            Logger.info(this, "Starting HikariCP metrics registration...");
            
            // Try multiple patterns to find Pool MBeans (usage metrics)
            String[] poolPatterns = {
                "com.zaxxer.hikari:type=Pool*",           // Standard pattern
                "com.zaxxer.hikari:type=Pool (*)",        // Pattern with parentheses
                "com.zaxxer.hikari:type=Pool,*",          // Pattern with comma separator
                "*:type=Pool*",                           // Wildcard domain
                "com.zaxxer.hikari:*Pool*"                // Flexible pool matching
            };
            
            Set<ObjectName> poolMBeans = new HashSet<>();
            for (String pattern : poolPatterns) {
                try {
                    Set<ObjectName> found = mBeanServer.queryNames(new ObjectName(pattern), null);
                    poolMBeans.addAll(found);
                    if (!found.isEmpty()) {
                        Logger.info(this, "Found " + found.size() + " Pool MBeans with pattern: " + pattern);
                        for (ObjectName mbean : found) {
                            Logger.info(this, "  Pool MBean: " + mbean.toString());
                        }
                    }
                } catch (Exception e) {
                    Logger.debug(this, "Pattern " + pattern + " failed: " + e.getMessage());
                }
            }
            
            // Try multiple patterns to find PoolConfig MBeans (configuration metrics)  
            String[] configPatterns = {
                "com.zaxxer.hikari:type=PoolConfig*",     // Standard pattern
                "com.zaxxer.hikari:type=PoolConfig (*)",  // Pattern with parentheses
                "com.zaxxer.hikari:type=PoolConfig,*",    // Pattern with comma separator
                "*:type=PoolConfig*",                     // Wildcard domain
                "com.zaxxer.hikari:*PoolConfig*"          // Flexible config matching
            };
            
            Set<ObjectName> configMBeans = new HashSet<>();
            for (String pattern : configPatterns) {
                try {
                    Set<ObjectName> found = mBeanServer.queryNames(new ObjectName(pattern), null);
                    configMBeans.addAll(found);
                    if (!found.isEmpty()) {
                        Logger.info(this, "Found " + found.size() + " PoolConfig MBeans with pattern: " + pattern);
                        for (ObjectName mbean : found) {
                            Logger.info(this, "  PoolConfig MBean: " + mbean.toString());
                        }
                    }
                } catch (Exception e) {
                    Logger.debug(this, "Pattern " + pattern + " failed: " + e.getMessage());
                }
            }
            
            // List ALL HikariCP MBeans for debugging
            try {
                Set<ObjectName> allHikariMBeans = mBeanServer.queryNames(new ObjectName("com.zaxxer.hikari:*"), null);
                Logger.info(this, "Total HikariCP MBeans found: " + allHikariMBeans.size());
                for (ObjectName mbean : allHikariMBeans) {
                    String type = mbean.getKeyProperty("type");
                    String pool = mbean.getKeyProperty("Pool");
                    Logger.info(this, "  MBean: " + mbean.toString() + " [type=" + type + ", pool=" + pool + "]");
                }
            } catch (Exception e) {
                Logger.debug(this, "Error listing all HikariCP MBeans: " + e.getMessage());
            }
            
            // Register Pool metrics if found
            if (!poolMBeans.isEmpty()) {
                Logger.info(this, "Registering usage metrics from " + poolMBeans.size() + " Pool MBeans...");
                for (ObjectName poolMBean : poolMBeans) {
                    String poolName = extractPoolName(poolMBean);
                    Logger.info(this, "Registering usage metrics for pool: " + poolName);
                    registerPoolMetrics(registry, poolMBean, poolName);
                }
            } else {
                Logger.warn(this, "No Pool MBeans found - usage metrics (active/idle connections) will not be available");
            }
            
            // Register PoolConfig metrics if found
            if (!configMBeans.isEmpty()) {
                Logger.info(this, "Registering configuration metrics from " + configMBeans.size() + " PoolConfig MBeans...");
                for (ObjectName configMBean : configMBeans) {
                    String poolName = extractPoolName(configMBean);
                    Logger.info(this, "Registering config metrics for pool: " + poolName);
                    registerPoolConfigMetrics(registry, configMBean, poolName);
                }
            } else {
                Logger.warn(this, "No PoolConfig MBeans found - configuration metrics will not be available");
            }
            
            int totalRegistered = poolMBeans.size() + configMBeans.size();
            if (totalRegistered > 0) {
                Logger.info(this, "HikariCP metrics registration completed successfully. " +
                    "Pool MBeans: " + poolMBeans.size() + ", Config MBeans: " + configMBeans.size());
            } else {
                Logger.warn(this, "No HikariCP MBeans found. Ensure hikari.register.mbeans=true is set.");
            }
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register HikariCP metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register only HikariCP configuration metrics when Pool MBeans are not available.
     */
    private void registerHikariConfigOnlyMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> configMBeans = mBeanServer.queryNames(
                new ObjectName("com.zaxxer.hikari:type=PoolConfig*"), null);
            
            for (ObjectName configMBean : configMBeans) {
                String poolName = extractPoolName(configMBean);
                registerPoolConfigMetrics(registry, configMBean, poolName);
            }
            
            if (!configMBeans.isEmpty()) {
                Logger.info(this, "Registered HikariCP configuration metrics for " + configMBeans.size() + " pool(s)");
            }
        } catch (Exception e) {
            Logger.debug(this, "Failed to register HikariCP config metrics: " + e.getMessage());
        }
    }
    
    /**
     * Register Pool MBean metrics (runtime connection metrics).
     */
    private void registerPoolMetrics(MeterRegistry registry, ObjectName poolMBean, String poolName) {
        // Active connections
        Gauge.builder(METRIC_PREFIX + ".hikari.connections.active", this,
            metrics -> getHikariAttribute(poolMBean, "ActiveConnections"))
            .description("Number of active HikariCP connections")
            .tag("pool", poolName)
            .register(registry);
        
        // Idle connections
        Gauge.builder(METRIC_PREFIX + ".hikari.connections.idle", this,
            metrics -> getHikariAttribute(poolMBean, "IdleConnections"))
            .description("Number of idle HikariCP connections")
            .tag("pool", poolName)
            .register(registry);
        
        // Total connections
        Gauge.builder(METRIC_PREFIX + ".hikari.connections.total", this,
            metrics -> getHikariAttribute(poolMBean, "TotalConnections"))
            .description("Total number of HikariCP connections")
            .tag("pool", poolName)
            .register(registry);
        
        // Threads awaiting connection
        Gauge.builder(METRIC_PREFIX + ".hikari.threads.awaiting", this,
            metrics -> getHikariAttribute(poolMBean, "ThreadsAwaitingConnection"))
            .description("Number of threads awaiting HikariCP connections")
            .tag("pool", poolName)
            .register(registry);
    }
    
    /**
     * Register PoolConfig MBean metrics (configuration parameters).
     */
    private void registerPoolConfigMetrics(MeterRegistry registry, ObjectName poolConfigMBean, String poolName) {
        // Maximum pool size
        Gauge.builder(METRIC_PREFIX + ".hikari.config.max_pool_size", this,
            metrics -> getHikariAttribute(poolConfigMBean, "MaximumPoolSize"))
            .description("Maximum HikariCP pool size")
            .tag("pool", poolName)
            .register(registry);
        
        // Minimum idle connections
        Gauge.builder(METRIC_PREFIX + ".hikari.config.min_idle", this,
            metrics -> getHikariAttribute(poolConfigMBean, "MinimumIdle"))
            .description("Minimum HikariCP idle connections")
            .tag("pool", poolName)
            .register(registry);
        
        // Connection timeout
        Gauge.builder(METRIC_PREFIX + ".hikari.config.connection_timeout", this,
            metrics -> getHikariAttribute(poolConfigMBean, "ConnectionTimeout"))
            .description("HikariCP connection timeout (ms)")
            .tag("pool", poolName)
            .register(registry);
        
        // Idle timeout
        Gauge.builder(METRIC_PREFIX + ".hikari.config.idle_timeout", this,
            metrics -> getHikariAttribute(poolConfigMBean, "IdleTimeout"))
            .description("HikariCP idle connection timeout (ms)")
            .tag("pool", poolName)
            .register(registry);
        
        // Max lifetime
        Gauge.builder(METRIC_PREFIX + ".hikari.config.max_lifetime", this,
            metrics -> getHikariAttribute(poolConfigMBean, "MaxLifetime"))
            .description("HikariCP connection max lifetime (ms)")
            .tag("pool", poolName)
            .register(registry);
        
        // Validation timeout
        Gauge.builder(METRIC_PREFIX + ".hikari.config.validation_timeout", this,
            metrics -> getHikariAttribute(poolConfigMBean, "ValidationTimeout"))
            .description("HikariCP connection validation timeout (ms)")
            .tag("pool", poolName)
            .register(registry);
        
        // Leak detection threshold
        Gauge.builder(METRIC_PREFIX + ".hikari.config.leak_detection_threshold", this,
            metrics -> getHikariAttribute(poolConfigMBean, "LeakDetectionThreshold"))
            .description("HikariCP leak detection threshold (ms)")
            .tag("pool", poolName)
            .register(registry);
    }
    
    /**
     * Extract pool name from Pool MBean ObjectName.
     */
    private String extractPoolName(ObjectName poolMBean) {
        String poolName = poolMBean.getKeyProperty("Pool");
        if (poolName == null) {
            // Fallback: extract from full ObjectName string
            String objectNameStr = poolMBean.toString();
            if (objectNameStr.contains("(") && objectNameStr.contains(")")) {
                int start = objectNameStr.indexOf("(") + 1;
                int end = objectNameStr.indexOf(")", start);
                poolName = objectNameStr.substring(start, end);
            } else {
                poolName = "default";
            }
        }
        return poolName;
    }
    
    /**
     * Find the corresponding PoolConfig MBean for a given Pool MBean.
     */
    private ObjectName findPoolConfigMBean(ObjectName poolMBean) {
        try {
            String poolName = extractPoolName(poolMBean);
            
            // Construct the PoolConfig MBean name
            String poolConfigName = "com.zaxxer.hikari:type=PoolConfig (" + poolName + ")";
            ObjectName poolConfigMBean = new ObjectName(poolConfigName);
            
            // Verify the MBean exists
            if (mBeanServer.isRegistered(poolConfigMBean)) {
                return poolConfigMBean;
            } else {
                Logger.debug(this, "PoolConfig MBean not found: " + poolConfigName);
                return null;
            }
        } catch (Exception e) {
            Logger.debug(this, "Error finding PoolConfig MBean: " + e.getMessage());
            return null;
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