package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class Task211020CreateHostIntegrityCheckerResultTables extends AbstractJDBCStartupTask {

    private static final String CREATE_HOSTS_IR_TABLE_SCRIPT = "CREATE TABLE hosts_ir(" +
            " local_identifier VARCHAR(36)," +
            " remote_identifier VARCHAR(36)," +
            " endpoint_id VARCHAR(40)," +
            " local_working_inode VARCHAR(36)," +
            " local_live_inode VARCHAR(36)," +
            " remote_working_inode VARCHAR(36)," +
            " remote_live_inode VARCHAR(36)," +
            " language_id INT8," +
            " host VARCHAR(255)," +
            " PRIMARY KEY (local_working_inode, language_id, endpoint_id)" +
            ");\n";

    @Override
    public boolean forceRun() {
        try {
            return !(new DotDatabaseMetaData().tableExists(DbConnectionFactory.getConnection(), "hosts_ir"));
        } catch (SQLException e) {
            return Boolean.FALSE;
        }
    }

    @Override
    public String getPostgresScript() {
        return CREATE_HOSTS_IR_TABLE_SCRIPT;
    }

    @Override
    public String getMySQLScript() {
        return CREATE_HOSTS_IR_TABLE_SCRIPT.replaceAll("INT8", "BIGINT");
    }

    @Override
    public String getOracleScript() {
        return CREATE_HOSTS_IR_TABLE_SCRIPT
                .replaceAll("INT8", "NUMBER(19,0)")
                .replaceAll("VARCHAR", "VARCHAR2");
    }

    @Override
    public String getMSSQLScript() {
        return CREATE_HOSTS_IR_TABLE_SCRIPT.replaceAll("INT8", "NUMERIC(19,0)");
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }
}
