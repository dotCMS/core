package com.dotmarketing.startup.runonce;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * This task removes the database information associated to the
 * {@code com.dotmarketing.quartz.job.DeleteInactiveClusterServersJob} class that was deleted as of
 * dotCMS 4.2. This will make sure that upgrading environments will not be trying to start a Quartz
 * Job that doesn't exist anymore.
 * 
 * @author Jose Castro
 * @version 4.2
 * @since Sep 28, 2017
 *
 */
public class Task04220RemoveDeleteInactiveClusterServersJob extends AbstractJDBCStartupTask {

    // Execution order of the queries is important
    private static final List<String> QUERIES = CollectionsUtils.list(
                    "DELETE FROM qrtz_excl_fired_triggers WHERE trigger_name = 'trigger23'",
                    "DELETE FROM qrtz_excl_cron_triggers WHERE trigger_name = 'trigger23'",
                    "DELETE FROM qrtz_excl_triggers WHERE trigger_name = 'trigger23' AND job_name = 'RemoveInactiveClusterServerJob'",
                    "DELETE FROM qrtz_excl_job_details WHERE job_name = 'RemoveInactiveClusterServerJob' AND job_class_name = 'com.dotmarketing.quartz.job.DeleteInactiveClusterServersJob'");

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public String getPostgresScript() {
        return StringUtils.join(QUERIES, ";");
    }

    @Override
    public String getMySQLScript() {
        return StringUtils.join(QUERIES, ";");
    }

    @Override
    public String getOracleScript() {
        return StringUtils.join(QUERIES, ";");
    }

    @Override
    public String getMSSQLScript() {
        return StringUtils.join(QUERIES, ";");
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
