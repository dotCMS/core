package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 *
 */
public class Task03555AddFlagToDeleteUsers extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun () {
        return true;
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript () {
        return "ALTER TABLE user_ ADD COLUMN deleteInProgress BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE user_ ADD COLUMN deleteDate TIMESTAMP;";
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript () {
        return "ALTER TABLE user_ ADD deleteInProgress BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE user_ ADD deleteDate DATETIME;";
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript () {
        return "ALTER TABLE user_ ADD deleteInProgress number(1,0) default 0;\n" +
                "ALTER TABLE user_ ADD deleteDate DATE;";
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript () {
        return "ALTER TABLE user_ ADD deleteInProgress TINYINT DEFAULT 0;\n" +
                "ALTER TABLE user_ ADD deleteDate DATETIME NULL;";
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script () {
        return "ALTER TABLE user_ ADD deleteInProgress BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE user_ ADD deleteDate TIMESTAMP;";
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }

}
