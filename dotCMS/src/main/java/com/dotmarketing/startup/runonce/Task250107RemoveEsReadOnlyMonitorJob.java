package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.util.ArrayList;
/**
 * Deletes the EsReadOnlyMonitorJob from the database and all the related records.
 * */
public class Task250107RemoveEsReadOnlyMonitorJob implements StartupTask {


    private final String JOB_NAME = "EsReadOnlyMonitorJob";
    private final String TRIGGER_NAME = "trigger29";
    private final String CRON_TABLE_NAME = "qrtz_excl_cron_triggers";
    private final String TRIGGER_TABLE_NAME = "qrtz_excl_triggers";
    private final String JOB_TABLE_NAME = "qrtz_excl_job_details";

    /**
     * Determines if the task should be forced to run.
     *
     * @return true if EsReadOnlyMonitorJob is found, false otherwise.
     */
    @Override
    public final boolean forceRun() {
        String sql=String.format("SELECT * FROM %s WHERE job_name=?", JOB_TABLE_NAME);
        DotConnect dc=new DotConnect();
        dc.setSQL(sql);

        dc.addParam(JOB_NAME);
        try {
            if (!dc.loadResults().isEmpty()){
                return true;
            }
        } catch (DotDataException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public final void executeUpgrade() throws DotDataException, DotRuntimeException {
        removeCron();
        removeTrigger();
        removeJob();
    }

    private void removeJob() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(String.format("DELETE FROM %s WHERE job_name = ?", JOB_TABLE_NAME));
        dc.addParam(JOB_NAME);
        dc.loadResult();
    }

    private void removeTrigger() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(String.format("DELETE FROM %s WHERE job_name = ?", TRIGGER_TABLE_NAME));
        dc.addParam(JOB_NAME);
        dc.loadResult();
    }

    private void removeCron() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(String.format("DELETE FROM %s WHERE trigger_name = ?", CRON_TABLE_NAME));
        dc.addParam(TRIGGER_NAME);
        dc.loadResult();
    }
}
