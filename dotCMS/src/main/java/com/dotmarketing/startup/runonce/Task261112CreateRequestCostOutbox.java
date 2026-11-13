package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.exception.DotRuntimeException;
import java.sql.SQLException;

/** Adds isolated durable telemetry storage without changing existing accounting or content tables. */
public class Task261112CreateRequestCostOutbox extends AbstractJDBCStartupTask {

    /** Runs on existing installations that do not yet have the additive outbox table. */
    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "request_cost_outbox");
        } catch (final SQLException e) {
            throw new DotRuntimeException("Cannot inspect request-cost outbox schema", e);
        }
    }

    /** Creates the same bounded row shape and delivery index used by fresh installations. */
    @Override
    public String getPostgresScript() {
        return """
                CREATE TABLE IF NOT EXISTS request_cost_outbox (
                    id varchar(64) PRIMARY KEY,
                    destination varchar(64) NOT NULL,
                    payload text NOT NULL CHECK (octet_length(payload) <= 8192),
                    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
                    next_attempt timestamptz NOT NULL DEFAULT clock_timestamp(),
                    attempts integer NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 16)
                );
                CREATE INDEX IF NOT EXISTS idx_request_cost_outbox_due
                    ON request_cost_outbox (destination, next_attempt, created_at, id);
                """;
    }
}
