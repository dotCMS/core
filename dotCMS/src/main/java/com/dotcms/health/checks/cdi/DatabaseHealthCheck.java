package com.dotcms.health.checks.cdi;

import com.dotcms.health.api.HealthEventManager;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.service.DatabaseHealthEventManager;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Config;
import com.dotmarketing.db.DbConnectionFactory;

import java.sql.SQLException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

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
        return false;
    }

    @Override
    protected boolean supportsEventDriven() {
        return true;
    }

    @Override
    public HealthEventManager getEventManager() {
        return eventManager;
    }

    @Override
    protected CheckResult performCheck() throws Exception {
        if (isShutdownInProgress()) {
            Logger.debug(this, "Skipping database connectivity test during shutdown");
            return new CheckResult(false, 0L, "Database health check skipped during shutdown to avoid connection attempts while database services are shutting down");
        }

        if (isEventDriven()) {
            Logger.debug(this, "Database health check using event-driven monitoring");
        }

        return measureExecution(() -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(() -> {
                try (Connection conn = DbConnectionFactory.getConnection()) {
                    if (conn == null) {
                        throw new SQLException("Database connection is null");
                    }
                    try (var stmt = conn.prepareStatement("SELECT 1")) {
                        var rs = stmt.executeQuery();
                        if (!rs.next() || rs.getInt(1) != 1) {
                            throw new SQLException("Database query 'SELECT 1' failed or returned unexpected result");
                        }
                    }
                    String dbVersion = Config.DB_VERSION > 0 ? String.valueOf(Config.DB_VERSION) : "unknown";
                    return "Database connection OK (DB version: " + dbVersion + ", verified with query)";
                }
            });

            try {
                // Fail fast if the DB is unavailable or blocked — 2 second timeout
                return future.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new Exception("Database connection timeout (over 2 seconds)", e);
            } catch (ExecutionException e) {
                throw new Exception("Database connection failed: " + e.getCause().getMessage(), e.getCause());
            } finally {
                executor.shutdownNow();
            }
        });
    }

    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus,
            HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();

        if (Config.DB_VERSION > 0) {
            data.put("dbVersion", Config.DB_VERSION);
        }

        if (result.durationMs > 0) {
            data.put("connectionTimeMs", result.durationMs);
        }

        if (result.error != null) {
            data.put("errorType", "database_connection");
        }

        return data;
    }

}