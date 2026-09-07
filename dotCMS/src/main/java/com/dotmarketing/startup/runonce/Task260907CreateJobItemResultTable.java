package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * Creates {@code job_item_result}, the durable per-item record a batch job writes as it runs
 * (#37166, spec FR-036 … FR-038).
 * <p>
 * <b>Why the table has to exist at all.</b> The job framework persists three things about a run:
 * its parameters, written once at creation and immutable thereafter; its progress, a single float;
 * and its result, harvested once at the terminal state. None of them is a mid-run record of which
 * items are done. Meanwhile the abandoned-job sweep re-queues a stalled run <b>without consulting
 * the retry policy</b>, so an interrupted run is retried whether or not its processor is marked
 * no-retry. Without a checkpoint the retry restarts from the first item, and the outcome then lies:
 * the files the first attempt created come back as name collisions, so the author is told 30 files
 * failed when all 30 are in the folder.
 * <p>
 * <b>Named for jobs, not for uploads, on purpose.</b> Folder copy and bulk delete (#37062), folder
 * move (#37165) and #37063 all need the same thing, and a table per bulk action would leave four
 * near-identical schemas. Whether durable per-item state belongs in the job framework itself rather
 * than in a shared table each feature reaches into is being proposed as an ADR; generic naming
 * keeps that door open instead of nailing it shut with an upload-specific schema.
 * <p>
 * Additive only — a new table, no changes to existing ones — so an older build simply ignores it
 * and the change stays rollback-safe.
 */
public class Task260907CreateJobItemResultTable extends AbstractJDBCStartupTask {

    private static final String TABLE_NAME = "job_item_result";

    /**
     * Checks whether the table already exists in the database.
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
     * Returns the PostgreSQL script that creates the per-item result table.
     *
     * @return table DDL
     */
    @Override
    public String getPostgresScript() {
        return getScript();
    }

    /**
     * Returns the PostgreSQL DDL.
     * <p>
     * The primary key is {@code (job_id, seq)} and <b>not</b> {@code (job_id, item_key)}. A batch is
     * allowed to contain two files of the same name — each is attempted, and the second is subject
     * to the same collision rule as a pre-existing name — so keying on the name would reject the
     * second write outright and leave a resumed run unable to tell the two apart. {@code seq} is the
     * submission index: unique within the batch and stable across a resume, which is what makes the
     * write idempotent. {@code item_key} is indexed but deliberately not unique.
     *
     * @return table DDL
     */
    private String getScript() {
        return "CREATE TABLE IF NOT EXISTS job_item_result (" // nosemgrep: gitlab.find_sec_bugs.CUSTOM_INJECTION-2 -- fully hardcoded DDL, no user input
                + " job_id varchar(255) not null,"
                + " seq integer not null,"
                + " item_key varchar(510) not null,"
                + " status varchar(20) not null,"
                + " reason varchar(64),"
                + " message text,"
                + " ref_id varchar(36),"
                + " updated_at timestamptz not null,"
                + " primary key (job_id, seq)"
                + ");"
                + "CREATE INDEX IF NOT EXISTS idx_job_item_result_key "
                + "ON job_item_result (job_id, item_key)";
    }
}
