package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * Changes {@code sitesearch_audit.path}, {@code host_list} and {@code lang_list} from
 * {@code varchar(500)} to {@code text}.
 *
 * <p>The audit row is the checkpoint an incremental Site Search crawl starts from. A job whose
 * include/exclude path list (or site/language list) is longer than 500 characters could not write
 * that row, so every following run fell back to a full index rebuild (issue #36706). Existing
 * {@code NOT NULL} constraints are kept: changing a column's type in Postgres does not drop them,
 * and going from {@code varchar} to {@code text} does not rewrite the table.</p>
 *
 * <p>Runs only while at least one of the three columns is not {@code text} yet, so the task does
 * nothing on a database that already applied the manual workaround from issue #19706.</p>
 *
 * @since Nov 12th, 2026
 */
public class Task261112WidenSiteSearchAuditTextColumns implements StartupTask {

    private static final String COUNT_NARROW_COLUMNS_SQL =
            "SELECT count(*) AS narrow FROM information_schema.columns "
                    + "WHERE table_schema = current_schema() "
                    + "AND table_name = 'sitesearch_audit' "
                    + "AND column_name IN ('path', 'host_list', 'lang_list') "
                    + "AND data_type <> 'text'";

    private static final String WIDEN_COLUMNS_SQL =
            "ALTER TABLE sitesearch_audit "
                    + "ALTER COLUMN path TYPE text, "
                    + "ALTER COLUMN host_list TYPE text, "
                    + "ALTER COLUMN lang_list TYPE text";

    /**
     * Tells the startup runner whether this task still has work to do.
     *
     * @return {@code true} when any of {@code path}, {@code host_list} or {@code lang_list} in
     *         {@code sitesearch_audit} is not a {@code text} column yet, or when the column types
     *         cannot be read
     */
    @Override
    public boolean forceRun() {
        try {
            return new DotConnect().setSQL(COUNT_NARROW_COLUMNS_SQL).getInt("narrow") > 0;
        } catch (final Exception e) {
            Logger.warn(this, "Could not read the sitesearch_audit column types, running the "
                    + "upgrade anyway: " + e.getMessage());
            return true;
        }
    }

    /**
     * Changes the three list columns of {@code sitesearch_audit} to {@code text} in one statement.
     *
     * @throws DotDataException if the {@code ALTER TABLE} fails
     */
    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect().setSQL(WIDEN_COLUMNS_SQL).loadResult();
    }
}
