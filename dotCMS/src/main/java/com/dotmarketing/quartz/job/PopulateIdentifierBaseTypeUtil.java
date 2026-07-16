package com.dotmarketing.quartz.job;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.sql.Connection;

/**
 * Populates the {@code base_type} column on the {@code identifier} table in small, independently
 * committed batches so that large customer databases (millions of rows) are not impacted by
 * long-running locks.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Run a single-statement batched UPDATE that touches at most {@code BATCH_SIZE} rows:
 *       <pre>
 *       UPDATE identifier
 *         SET base_type = s.structuretype
 *         FROM structure s
 *        WHERE identifier.asset_subtype = s.velocity_var_name
 *          AND identifier.base_type IS NULL
 *          AND identifier.id IN (
 *              SELECT id FROM identifier
 *               WHERE base_type IS NULL
 *                 AND asset_subtype IS NOT NULL
 *               LIMIT :batchSize
 *          )
 *       </pre>
 *   </li>
 *   <li>Commit immediately (each call to {@link #processBatch()} is {@code @WrapInTransaction}).</li>
 *   <li>Sleep for {@code SLEEP_BETWEEN_BATCHES_MS} milliseconds to relieve DB pressure.</li>
 *   <li>Repeat until no rows are updated.</li>
 * </ol>
 *
 * <p>Because rows that have been assigned a {@code base_type} no longer satisfy
 * {@code base_type IS NULL}, each iteration naturally advances to the next unprocessed set
 * without needing OFFSET or cursor state. The operation is fully idempotent.
 */
public class PopulateIdentifierBaseTypeUtil {

    private static final int BATCH_SIZE = Config.getIntProperty(
            "task.populateIdentifierBaseType.batchsize", 500);

    private static final long SLEEP_BETWEEN_BATCHES_MS = Config.getLongProperty(
            "task.populateIdentifierBaseType.sleep.ms", 250L);

    /**
     * Batched UPDATE: resolves base_type from the structure table for at most BATCH_SIZE rows
     * whose base_type is still NULL.
     */
    private static final String UPDATE_BATCH =
            "UPDATE identifier " +
            "   SET base_type = s.structuretype " +
            "  FROM structure s " +
            " WHERE identifier.asset_subtype = s.velocity_var_name " +
            "   AND identifier.base_type IS NULL " +
            "   AND identifier.id IN ( " +
            "       SELECT id FROM identifier " +
            "        WHERE base_type IS NULL " +
            "          AND asset_subtype IS NOT NULL " +
            "        LIMIT " + BATCH_SIZE +
            "   )";

    /**
     * Runs the full population loop, committing one small batch at a time until every
     * eligible {@code identifier} row has a non-null {@code base_type}.
     *
     * @return Total number of rows updated.
     */
    public int populate() {
        Logger.info(this, "PopulateIdentifierBaseType: starting — batch size=" + BATCH_SIZE
                + ", sleep between batches=" + SLEEP_BETWEEN_BATCHES_MS + "ms");

        int total = 0;
        int pass = 0;

        while (true) {
            final int updated = processBatch();
            if (updated == 0) {
                break;
            }

            total += updated;
            pass++;

            if (pass % 100 == 0) {
                Logger.info(this, "PopulateIdentifierBaseType: " + total + " rows updated so far...");
            }

            if (SLEEP_BETWEEN_BATCHES_MS > 0) {
                try {
                    Thread.sleep(SLEEP_BETWEEN_BATCHES_MS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Logger.warn(this, "PopulateIdentifierBaseType: interrupted — stopping early after "
                            + total + " rows");
                    break;
                }
            }
        }

        Logger.info(this, "PopulateIdentifierBaseType: done — total rows updated: " + total);
        return total;
    }

    /**
     * Executes a single batch UPDATE in its own transaction. Keeping the transaction small
     * (at most {@code BATCH_SIZE} rows) avoids long-held row locks on busy tables.
     *
     * @return Number of rows updated in this batch; 0 means all rows are already populated.
     */
    private int processBatch() {
        try(Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            int result = new DotConnect().executeUpdate(conn, UPDATE_BATCH);
            conn.commit();
            return result;
        } catch (final Exception e) {
            throw new DotRuntimeException(
                    "PopulateIdentifierBaseType: error executing batch update", e);
        }
    }

}
