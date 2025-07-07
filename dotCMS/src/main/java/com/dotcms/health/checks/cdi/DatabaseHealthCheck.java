package com.dotcms.health.checks.cdi;

import com.dotcms.health.api.HealthEventManager;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.service.DatabaseHealthEventManager;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Config;
import com.dotmarketing.db.DbConnectionFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * CDI-based health check for database availability.
 * This check is excluded from liveness probes since database issues should not 
 * trigger pod restarts, just remove the instance from load balancing.
 * 
 * Supports both event-driven and polling modes:
 * - Event-driven: Monitors HikariCP connection pool events for fast failure detection
 * - Polling: Traditional connection testing (fallback mode)
 */
@ApplicationScoped
public class DatabaseHealthCheck extends HealthCheckBase {

    @Inject
    private DatabaseHealthEventManager eventManager;

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public boolean isLivenessCheck() {
        // Database issues should not trigger pod restarts
        return false;
    }

    @Override
    protected boolean supportsEventDriven() {
        return true; // Database health check supports event-driven monitoring
    }
    
    @Override
    public HealthEventManager getEventManager() {
        return eventManager;
    }

    @Override
    protected CheckResult performCheck() throws Exception {
        // Skip expensive database operations during shutdown
        if (isShutdownInProgress()) {
            Logger.debug(this, "Skipping database connectivity test during shutdown");
            return new CheckResult(false, 0L, "Database health check skipped during shutdown to avoid connection attempts while database services are shutting down");
        }
        
        // Check if we should use event-driven result
        if (isEventDriven()) {
            Logger.debug(this, "Database health check using event-driven monitoring");
            // Event manager handles the actual monitoring
            // This method is only called for fallback or initial checks
        }
        
        // Use the traditional database connectivity test
        return measureExecution(() -> {
            // Test database connectivity by executing an actual query
            try (Connection conn = DbConnectionFactory.getConnection()) {
                if (conn == null) {
                    throw new Exception("Database connection is null");
                }
                
                // CRITICAL: Don't rely on conn.isValid() alone - it can be unreliable with stale connections
                // Execute a lightweight query to verify actual database connectivity
                try (var stmt = conn.prepareStatement("SELECT 1")) {
                    var rs = stmt.executeQuery();
                    if (!rs.next() || rs.getInt(1) != 1) {
                        throw new Exception("Database query 'SELECT 1' failed or returned unexpected result");
                    }
                }
                
                // Include database version information in the success message
                String dbVersion = Config.DB_VERSION > 0 ? String.valueOf(Config.DB_VERSION) : "unknown";
                return "Database connection OK (DB version: " + dbVersion + ", verified with query)";
            }
        });
    }

    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Always include database version since it's mentioned in messages
        if (Config.DB_VERSION > 0) {
            data.put("dbVersion", Config.DB_VERSION);
        }
        
        // Always include connection timing data since it's mentioned in messages
        if (result.durationMs > 0) {
            data.put("connectionTimeMs", result.durationMs);
        }
        
        // Include error type for database-specific failures
        if (result.error != null) {
            data.put("errorType", "database_connection");
        }
        
        return data;
    }

} 