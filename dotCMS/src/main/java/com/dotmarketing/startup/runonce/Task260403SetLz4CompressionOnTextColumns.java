package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Sets LZ4 compression on every {@code text}, {@code bytea}, {@code jsonb}, and {@code json}
 * column in the {@code public} schema that uses TOAST storage.
 *
 * <h3>Why LZ4?</h3>
 * PostgreSQL defaults to pglz for TOAST compression. LZ4 decompresses roughly 3–5× faster
 * than pglz while achieving comparable (often slightly better) ratios on typical dotCMS data
 * (HTML, JSON, workflow payloads). Read-heavy workloads — content delivery, page rendering,
 * workflow evaluation — pay the decompression cost on every fetch of a TOASTed column, so
 * faster decompression directly reduces latency.
 *
 * <h3>Scope</h3>
 * Only columns with TOAST-eligible storage are targeted (extended / external / main).
 * Plain-storage columns (e.g. short {@code varchar}) are excluded automatically via
 * the {@code pg_attribute.attstorage} filter. Columns already using LZ4
 * ({@code attcompression = 'l'}) are skipped — the task is fully idempotent.
 *
 * <h3>Effect on existing data</h3>
 * {@code SET COMPRESSION lz4} changes the compression method for <em>future</em> writes only.
 * Existing TOASTed values are not rewritten immediately — they retain their original encoding
 * until the row is next updated. This makes the migration instant and lock-free.
 *
 * @since Apr 3rd, 2026
 */
public class Task260403SetLz4CompressionOnTextColumns implements StartupTask {

    /**
     * Finds all TOAST-eligible text/bytea/jsonb/json columns in the public schema that do not
     * yet use LZ4 compression.
     *
     * <p>Storage codes: 'x' = extended (TOAST, compressed), 'e' = external (TOAST,
     * uncompressed), 'm' = main (inline compressed). Compression code 'l' = lz4.
     */
    private static final String FIND_COLUMNS_SQL =
            "SELECT c.relname AS tbl, a.attname AS col " +
            "  FROM pg_attribute a " +
            "  JOIN pg_class c ON c.oid = a.attrelid " +
            "  JOIN pg_namespace n ON n.oid = c.relnamespace " +
            "  JOIN pg_type t ON t.oid = a.atttypid " +
            " WHERE n.nspname = 'public' " +
            "   AND c.relkind = 'r' " +
            "   AND a.attnum > 0 " +
            "   AND NOT a.attisdropped " +
            "   AND t.typname IN ('text', 'bytea', 'jsonb', 'json') " +
            "   AND a.attstorage IN ('x', 'e', 'm') " +
            "   AND a.attcompression != 'l' " +
            " ORDER BY c.relname, a.attname";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        final List<Map<String, Object>> columns = new DotConnect()
                .setSQL(FIND_COLUMNS_SQL)
                .loadObjectResults();

        if (columns.isEmpty()) {
            Logger.info(this, "All eligible columns already use LZ4 compression — nothing to do");
            return;
        }

        Logger.info(this, "Setting LZ4 compression on " + columns.size() + " column(s)");
        int updated = 0;
        int failed = 0;

        for (final Map<String, Object> row : columns) {
            final String table = (String) row.get("tbl");
            final String column = (String) row.get("col");
            try {
                new DotConnect().executeStatement(
                        "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET COMPRESSION lz4");
                updated++;
            } catch (final SQLException e) {
                Logger.warn(this, "Failed to set LZ4 on " + table + "." + column
                        + ": " + e.getMessage());
                failed++;
            }
        }

        Logger.info(this, "LZ4 compression migration complete — "
                + updated + " column(s) updated, " + failed + " skipped due to errors");
    }

}
