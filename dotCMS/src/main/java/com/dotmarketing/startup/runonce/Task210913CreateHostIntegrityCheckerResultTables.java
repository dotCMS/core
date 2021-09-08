package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.Collections;
import java.util.List;

public class Task210913CreateHostIntegrityCheckerResultTables extends AbstractJDBCStartupTask {

    private final String script = "CREATE TABLE hosts_ir(" +
            " local_identifier VARCHAR(36)," +
            " remote_identifier VARCHAR(36)," +
            " endpoint_id VARCHAR(40)," +
            " local_working_inode VARCHAR(36)," +
            " local_live_inode VARCHAR(36)," +
            " remote_working_inode VARCHAR(36)," +
            " remote_live_inode VARCHAR(36)," +
            " language_id int8," +
            " host VARCHAR(255)," +
            " PRIMARY KEY (local_working_inode, language_id, endpoint_id)" +
            ");\n";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return script;
    }

    @Override
    public String getMySQLScript() {
        return script.replaceAll("INT8", "BIGINT");
    }

    @Override
    public String getOracleScript() {
        return script
                .replaceAll("INT8", "NUMBER(19,0)")
                .replaceAll("VARCHAR", "VARCHAR2");
    }

    @Override
    public String getMSSQLScript() {
        return script.replaceAll("INT8", "NUMERIC(19,0)");
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }
}
