package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import java.sql.SQLException;

public class Task250107RemoveEsReadOnlyMonitorJobTest {


    private final String JOB_NAME = "EsReadOnlyMonitorJob";
    private final String TRIGGER_NAME = "trigger29";
    private final String CRON_TABLE_NAME = "qrtz_excl_cron_triggers";
    private final String TRIGGER_TABLE_NAME = "qrtz_excl_triggers";
    private final String JOB_TABLE_NAME = "qrtz_excl_job_details";


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }


    @Test
    public void test_job() throws DotDataException, SQLException {
        final DotConnect dc = new DotConnect();

        Task250107RemoveEsReadOnlyMonitorJob upgradeTask = new Task250107RemoveEsReadOnlyMonitorJob();

        if (!upgradeTask.forceRun()) {
            dc.setSQL(String.format("INSERT INTO %s (job_name, job_group, job_class_name, is_durable, is_volatile, is_stateful, requests_recovery) VALUES (?, 'dotcms_jobs', 'com.dotmarketing.quartz.job.EsReadOnlyMonitorJob', 'f', 'f', 'f', 'f')", JOB_TABLE_NAME));
            dc.addParam(JOB_NAME);
            dc.loadResults();

            dc.setSQL(String.format("INSERT INTO %s (job_name, trigger_name, trigger_group, job_group, is_volatile, trigger_state, trigger_type, start_time) VALUES (?,?, 'group98', 'dotcms_jobs', 'f', 'WAITING', 'CRON', 17362" +
                    "84300000)", TRIGGER_TABLE_NAME));
            dc.addParam(JOB_NAME);
            dc.addParam(TRIGGER_NAME);
            dc.loadResults();

            dc.setSQL(String.format("INSERT INTO %s (trigger_name, trigger_group, cron_expression) VALUES (?, 'group98', '0 */5 * ? * *')", CRON_TABLE_NAME));
            dc.addParam(TRIGGER_NAME);
            dc.loadResults();

            Assert.assertFalse(dc.setSQL("SELECT * FROM " + JOB_TABLE_NAME + " WHERE job_name = '" + JOB_NAME + "'").loadResults().isEmpty());
        }


        Assert.assertTrue(upgradeTask.forceRun());

        upgradeTask.executeUpgrade();

        Assert.assertTrue(dc.setSQL("SELECT * FROM " + JOB_TABLE_NAME + " WHERE job_name = '" + JOB_NAME + "'").loadResults().isEmpty());

    }


}
