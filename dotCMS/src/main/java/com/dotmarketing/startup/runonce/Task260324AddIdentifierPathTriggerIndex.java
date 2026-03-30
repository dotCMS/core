package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * Creates a functional index on {@code identifier(host_inode, asset_type,
 * lower(parent_path||asset_name||'/'))} to support the
 * {@code identifier_parent_path_trigger} trigger efficiently.
 * The trigger calls the {@code identifier_parent_path_check()} function which runs:
 * <p>
 * <pre>
 *   SELECT id FROM identifier
 *     WHERE asset_type = 'folder'
 *       AND host_inode = NEW.host_inode
 *       AND lower(parent_path||asset_name||'/') = lower(NEW.parent_path)
 *       AND id &lt;&gt; NEW.id;
 * </pre>
 * Without this index the planner falls back to scanning all folder rows for the
 * given host via {@code idx_identifier_perm}, applying the expression predicate
 * as a row-level filter (O(N) per trigger invocation). During a bulk
 * {@code UPDATE} that moves thousands of identifiers to a renamed folder, the
 * trigger fires once per row, making the total cost O(N×F) where F is the
 * number of folders on the host. With this index each trigger invocation
 * becomes O(log F).
 */
public class Task260324AddIdentifierPathTriggerIndex implements StartupTask {

    private static final String INDEX_NAME = "idx_identifier_parent_path_trigger";

    @Override
    public boolean forceRun() {
        // CREATE INDEX IF NOT EXISTS is idempotent; forceRun=false lets the startup framework
        // skip this task after it has been recorded in db_version.
        return false;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        try {
            if (DbConnectionFactory.isPostgres()) {
                Logger.info(this, "Creating index " + INDEX_NAME + " on identifier table");
                new DotConnect().executeStatement(
                        "CREATE INDEX IF NOT EXISTS " + INDEX_NAME + " ON identifier"
                                + " (host_inode, asset_type, lower(parent_path||asset_name||'/'))");
            } else {
                Logger.info(this, "Skipping index " + INDEX_NAME + " (not a Postgres database)");
            }
        } catch (final SQLException e) {
            throw new DotDataException("Failed to create index " + INDEX_NAME + ": "
                    + e.getMessage(), e);
        }
    }

}
