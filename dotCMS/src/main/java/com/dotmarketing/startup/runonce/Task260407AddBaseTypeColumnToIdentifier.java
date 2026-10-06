package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.job.PopulateIdentifierBaseTypeJob;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Adds a {@code base_type} column to the {@code identifier} table and creates an index on it.
 * The column is populated asynchronously by {@link PopulateIdentifierBaseTypeJob} to avoid
 * long-running transactions on large customer databases.
 *
 * @since Apr 7th, 2026
 */
public class Task260407AddBaseTypeColumnToIdentifier implements StartupTask {

    private static final String COLUMN_NAME = "base_type";
    private static final String INDEX_NAME = "idx_identifier_base_type";

    @Override
    public boolean forceRun() {
        try {
            // Run if the index has not been created yet
            final List<Map<String, Object>> result = new DotConnect()
                    .setSQL("SELECT 1 FROM pg_indexes WHERE tablename = 'identifier' AND indexname = ?")
                    .addParam(INDEX_NAME)
                    .loadObjectResults();
            if (result.isEmpty()) {
                return true;
            }
            // Also run if the backfill is incomplete — handles server restarts mid-migration.
            // DDL uses IF NOT EXISTS so re-running executeUpgrade() is always safe.
            return PopulateIdentifierBaseTypeJob.hasPendingRows();
        } catch (final DotDataException e) {
            // Fail open — a transient DB error during startup must not permanently skip the
            // migration. executeUpgrade() uses IF NOT EXISTS so re-running is always safe.
            Logger.error(this, "Error in forceRun() for " + INDEX_NAME + ", defaulting to run: " + e.getMessage(), e);
            return true;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        addBaseTypeColumn();
        createIndex();
        PopulateIdentifierBaseTypeJob.fireJob();
    }

    /**
     * Adds the base_type integer column to the identifier table.
     */
    private void addBaseTypeColumn() throws DotDataException {
        try {
            Logger.info(this, "Adding column " + COLUMN_NAME + " to identifier table");
            new DotConnect().executeStatement(
                    "ALTER TABLE identifier ADD COLUMN IF NOT EXISTS "
                            + COLUMN_NAME + " INT4");
        } catch (final SQLException e) {
            throw new DotDataException(
                    "Failed to add column " + COLUMN_NAME + ": " + e.getMessage(), e);
        }
    }

    /**
     * Creates an index on identifier.base_type for efficient filtering.
     */
    private void createIndex() throws DotDataException {
        try {
            Logger.info(this, "Creating index " + INDEX_NAME + " on identifier table");
            new DotConnect().executeStatement(
                    "CREATE INDEX IF NOT EXISTS " + INDEX_NAME
                            + " ON identifier (" + COLUMN_NAME + ")");
        } catch (final SQLException e) {
            throw new DotDataException(
                    "Failed to create index " + INDEX_NAME + ": " + e.getMessage(), e);
        }
    }

}
