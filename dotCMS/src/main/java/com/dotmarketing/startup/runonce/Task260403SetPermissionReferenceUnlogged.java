package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Converts the {@code permission_reference} table to UNLOGGED.
 *
 * <p>UNLOGGED tables bypass WAL (Write-Ahead Logging), which eliminates the
 * per-row WAL write cost on every INSERT/UPDATE/DELETE. This table is a pure
 * denormalized cache — rows are rebuilt automatically by the permission system
 * whenever they are invalidated. Losing this data on an unexpected crash is
 * completely safe because the permission system rebuilds it on demand.
 *
 * <p>Expected benefits:
 * <ul>
 *   <li>~2–3× faster INSERT/DELETE throughput on permission rebuilds</li>
 *   <li>Reduced WAL volume, lowering I/O pressure and replication lag</li>
 *   <li>Smaller checkpoints during mass permission recalculation</li>
 * </ul>
 *
 * <p>{@code ALTER TABLE ... SET UNLOGGED} rewrites the table (brief exclusive
 * lock) and truncates the unlogged table on replica nodes — expected behaviour
 * since permission_reference is never read-from replicas directly.
 *
 * @since Apr 3rd, 2026
 */
public class Task260403SetPermissionReferenceUnlogged implements StartupTask {

    private static final String TABLE_NAME = "permission_reference";

    @Override
    public boolean forceRun() {
        if (!DbConnectionFactory.isPostgres()) {
            return false;
        }
        try {
            // relpersistence = 'u' means UNLOGGED; 'p' means permanent (logged)
            final List<Map<String, Object>> result = new DotConnect()
                    .setSQL("SELECT 1 FROM pg_class WHERE relname = ? AND relpersistence = 'u'")
                    .addParam(TABLE_NAME)
                    .loadObjectResults();
            return result.isEmpty(); // run if NOT already unlogged
        } catch (final DotDataException e) {
            // Fail open — idempotent DDL, safe to re-run
            Logger.error(this, "Error in forceRun() for " + TABLE_NAME + ", defaulting to run: "
                    + e.getMessage(), e);
            return true;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        if (!DbConnectionFactory.isPostgres()) {
            Logger.info(this, "Skipping SET UNLOGGED for " + TABLE_NAME + " (not PostgreSQL)");
            return;
        }
        try {
            Logger.info(this, "Converting " + TABLE_NAME + " to UNLOGGED");
            new DotConnect().executeStatement(
                    "ALTER TABLE " + TABLE_NAME + " SET UNLOGGED");
            Logger.info(this, "Successfully converted " + TABLE_NAME + " to UNLOGGED");
        } catch (final SQLException e) {
            throw new DotDataException(
                    "Failed to set " + TABLE_NAME + " UNLOGGED: " + e.getMessage(), e);
        }
    }

}
