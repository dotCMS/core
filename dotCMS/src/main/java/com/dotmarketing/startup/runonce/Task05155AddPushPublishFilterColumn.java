package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

public class Task05155AddPushPublishFilterColumn extends AbstractJDBCStartupTask {

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
