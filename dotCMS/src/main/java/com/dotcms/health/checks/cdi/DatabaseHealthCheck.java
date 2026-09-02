package com.dotcms.health.checks.cdi;

import com.dotcms.health.api.HealthEventManager;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.service.DatabaseHealthEventManager;
import com.dotcms.health.util.HealthCheckBase;
import com.dotcms.health.util.HealthCheckUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Config;
import com.dotmarketing.db.DbConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
            final int timeoutMs = Config.getIntProperty(
                    "health.check.database.timeout.seconds", 2) * 1000;
            return HealthCheckUtils.executeWithTimeout(() -> {
                // getDataSource().getConnection() bypasses ThreadLocal — try-with-resources is safe
                // because close() returns the connection to HikariCP's pool (no ownership conflict).
                // No deeper code relies on this connection; we just borrow, validate, and release.
                try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
                    if (conn == null) {
                        throw new SQLException("Database connection is null");
                    }
                    // isValid() confirms the database can process requests, not just that a
                    // TCP connection exists. Covers edge cases where HikariCP skips borrow
                    // validation (idle < 500ms) or the DB accepts connections but can't
                    // serve queries. pgjdbc implements this with the empty query protocol —
                    // one round-trip, no statement allocation.
                    final int timeoutSeconds = Config.getIntProperty(
                            "health.check.database.timeout.seconds", 2);
                    if (!conn.isValid(timeoutSeconds)) {
                        throw new SQLException("Database connection validation failed (isValid returned false)");
                    }
                    final String dbVersion = Config.DB_VERSION > 0 ? String.valueOf(Config.DB_VERSION) : "unknown";
                    return "Database connection OK (DB version: " + dbVersion + ")";
                }
            }, timeoutMs, "Database health check");
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