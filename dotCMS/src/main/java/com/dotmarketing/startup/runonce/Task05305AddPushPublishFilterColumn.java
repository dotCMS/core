package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

public class Task05305AddPushPublishFilterColumn extends AbstractJDBCStartupTask {

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE publishing_bundle ADD filter_key VARCHAR(100)";
    }

    @Override
    public String getMySQLScript() {
        return "ALTER TABLE publishing_bundle ADD filter_key VARCHAR(100)";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE publishing_bundle ADD filter_key VARCHAR2(100)";
    }

    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE publishing_bundle ADD filter_key NVARCHAR(100)";
    }

    @Override
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    @Override
    public boolean forceRun() {
        return true;
    }
}
