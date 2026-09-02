package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

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
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
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
