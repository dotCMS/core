package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

/**
 * Class that deletes unused quartz cron jobs: DeliverCampaignJob, CheckQuestionJob, UpdateRatingJob, PopBouncedMailJob and PopBouncedMailThread
 *
 * @author Daniel Laguna
 * @version 4.1.0
 * @since April 13, 2017
 */
public class Task04100DeleteUnusedJobEntries extends AbstractJDBCStartupTask {

    /**
     * DELETE_QRTZ_EXCL_CRON SQL Statement
     */
    private final String DELETE_QRTZ_EXCL_CRON = "DELETE FROM qrtz_excl_cron_triggers WHERE trigger_name IN (SELECT trigger_name FROM qrtz_excl_triggers WHERE job_name = '%s');\n";

    /**
     * DELETE_QRTZ_EXCL_TRIGGERS SQL Statement
     */
    private final String DELETE_QRTZ_EXCL_TRIGGERS = "DELETE FROM qrtz_excl_triggers WHERE job_name = '%s';\n";

    /**
     * DELETE_QRTZ_EXCL_JOB SQL Statement
     */
    private final String DELETE_QRTZ_EXCL_JOB = "DELETE FROM qrtz_excl_job_details WHERE job_name = '%s';\n";

    /**
     * Jobs to be deleted
     */
    private String[] jobs = {"DeliverCampaignJob", "CheckQuestionJob", "UpdateRatingJob", "PopBouncedMailJob", "PopBouncedMailThread"};

    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return fetchDeleteQueries();
    }

    @Override
    public String getMySQLScript() {
        return fetchDeleteQueries();
    }

    @Override
    public String getOracleScript() {
        return fetchDeleteQueries();
    }

    @Override
    public String getMSSQLScript() {
        return fetchDeleteQueries();
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    /**
     * Creates the delete script for unused classes on jobs
     *
     * @return String containing the queries
     */
    private String fetchDeleteQueries() {
        StringBuilder sb = new StringBuilder();

        for (String key : jobs) {
            sb.append(String.format(DELETE_QRTZ_EXCL_CRON, key));
            sb.append(String.format(DELETE_QRTZ_EXCL_TRIGGERS, key));
            sb.append(String.format(DELETE_QRTZ_EXCL_JOB, key));
        }

        Logger.debug(this, "Executing delete statements: " + sb.toString());
        return sb.toString();
    }
}
