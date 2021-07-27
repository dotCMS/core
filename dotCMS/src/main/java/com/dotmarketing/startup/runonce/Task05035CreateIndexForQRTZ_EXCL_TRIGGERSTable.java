package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.Collections;
import java.util.List;

public class Task05035CreateIndexForQRTZ_EXCL_TRIGGERSTable extends AbstractJDBCStartupTask {
    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return "create index IDX_QRTZ_EXCL_TRIGGERS_QRTZ_EXCL_JOB_DETAILS on QRTZ_EXCL_TRIGGERS (JOB_NAME, JOB_GROUP);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }

    @Override
    public boolean forceRun() {
        return true;
    }
}
