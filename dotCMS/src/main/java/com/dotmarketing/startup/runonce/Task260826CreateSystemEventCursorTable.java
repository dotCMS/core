package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * Creates {@code system_event_cursor}, the per-node delivery cursor that replaces the in-memory
 * wall-clock high-water mark previously held by {@code SystemEventsJob} (issue #36827).
 *
 * <p>The cursor lives in its own table rather than as a column on {@code system_event} so the
 * migration stays purely additive: an older release rolled back onto this schema never queries the
 * table, and nothing about the event rows themselves changes.
 */
public class Task260826CreateSystemEventCursorTable extends AbstractJDBCStartupTask {

    private static final String TABLE_NAME = "system_event_cursor";

    /**
     * Checks whether the cursor table already exists in the database.
     *
     * @return true when the task must run
     */
    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().tableExists(DbConnectionFactory.getConnection(), TABLE_NAME);
        } catch (final SQLException e) {
            Logger.error(this, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns the PostgreSQL script that creates the cursor table.
     *
     * @return table DDL
     */
    @Override
    public String getPostgresScript() {
        return getScript();
    }

    /**
     * Returns the PostgreSQL DDL for the cursor table. One row per server: {@code server_id} is the
     * primary key, so a node can never hold two conflicting cursors.
     *
     * @return table DDL
     */
    private String getScript() {
        return "CREATE TABLE IF NOT EXISTS system_event_cursor (" // nosemgrep: gitlab.find_sec_bugs.CUSTOM_INJECTION-2 -- fully hardcoded DDL, no user input
                + " server_id varchar(36) not null,"
                + " last_event_date bigint not null,"
                + " mod_date timestamptz not null,"
                + " constraint pk_system_event_cursor primary key (server_id)"
                + ")";
    }
}
