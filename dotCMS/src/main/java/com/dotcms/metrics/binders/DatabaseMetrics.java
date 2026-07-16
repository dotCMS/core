package com.dotcms.metrics.binders;

import com.dotcms.health.checks.cdi.DatabaseHealthCheck;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
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
    private HikariPoolMXBean poolProxy;
    private DatabaseHealthCheck healthCheck;
    
    public DatabaseMetrics() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
        this.healthCheck = new DatabaseHealthCheck();
        this.poolProxy = initializeHikariPoolProxy();
    }
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            registerConnectionPoolMetrics(registry);
            registerHikariMetrics(registry);
            
            Logger.info(this, "Database metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register database metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize HikariCP pool proxy for JMX monitoring without creating database connections.
     */
    private HikariPoolMXBean initializeHikariPoolProxy() {
        try {
            HikariDataSource dataSource = (HikariDataSource) DbConnectionFactory.getDataSource();
            if (dataSource == null) {
                Logger.warn(this, "HikariDataSource not available for metrics monitoring");
                return null;
            }
            
            String poolName = dataSource.getPoolName();
            if (poolName == null) {
                poolName = "HikariPool-1"; // Default pool name
            }
            
            ObjectName objectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
            return JMX.newMXBeanProxy(mBeanServer, objectName, HikariPoolMXBean.class);
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to initialize HikariCP pool proxy for metrics: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Register core database connection metrics using health check and HikariCP JMX.
     * IMPORTANT: No new database connections are created - uses existing health infrastructure.
     */
    private void registerConnectionPoolMetrics(MeterRegistry registry) {
        // Database health status from existing health check (no new connections)
        Gauge.builder(METRIC_PREFIX + ".health.available", this, metrics -> getDatabaseHealthStatus())
            .description("Database health status from health check system (1=healthy, 0=unhealthy)")
            .register(registry);
        
        // HikariCP connection pool metrics (JMX-based, no new connections)
        if (poolProxy != null) {
            Gauge.builder(METRIC_PREFIX + ".pool.active", this, metrics -> getActiveConnections())
                .description("Number of active database connections in the pool")
                .register(registry);
                
            Gauge.builder(METRIC_PREFIX + ".pool.idle", this, metrics -> getIdleConnections())
                .description("Number of idle database connections in the pool")
                .register(registry);
                
            Gauge.builder(METRIC_PREFIX + ".pool.total", this, metrics -> getTotalConnections())
                .description("Total number of database connections in the pool")
                .register(registry);
                
            Gauge.builder(METRIC_PREFIX + ".pool.waiting", this, metrics -> getThreadsAwaitingConnection())
                .description("Number of threads waiting for database connections")
                .register(registry);
        } else {
            Logger.warn(this, "HikariCP pool proxy not available - connection pool metrics disabled");
        }
    }
    
    /**
     * REMOVED: registerDatabaseHealthMetrics() method that created database connections.
     * Database health is now provided via the existing DatabaseHealthCheck system 
     * in registerConnectionPoolMetrics() as dotcms.database.health.available.
     */
    
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
     * Get database health status from existing health check system (no new connections).
     */
    private double getDatabaseHealthStatus() {
        try {
            var result = healthCheck.check();
            return result.status() == HealthStatus.UP ? 1.0 : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get database health status: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get active connections from HikariCP JMX (no new connections).
     */
    private double getActiveConnections() {
        try {
            return poolProxy != null ? poolProxy.getActiveConnections() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get active connections: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get idle connections from HikariCP JMX (no new connections).
     */
    private double getIdleConnections() {
        try {
            return poolProxy != null ? poolProxy.getIdleConnections() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get idle connections: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get total connections from HikariCP JMX (no new connections).
     */
    private double getTotalConnections() {
        try {
            return poolProxy != null ? poolProxy.getTotalConnections() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total connections: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get threads awaiting connections from HikariCP JMX (no new connections).
     */
    private double getThreadsAwaitingConnection() {
        try {
            return poolProxy != null ? poolProxy.getThreadsAwaitingConnection() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get threads awaiting connection: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * REMOVED: canGetSystemTime() method that created database connections.
     * Database health is now checked via the existing DatabaseHealthCheck system.
     */
    
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