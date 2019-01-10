package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 *
 */
public class Task03555AddFlagToDeleteUsers extends AbstractJDBCStartupTask {

    private final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

    @Override
    public boolean forceRun () {
        return !this.databaseMetaData.existsColumns(DbConnectionFactory.getConnection(), "user_", "delete_in_progress", "delete_date");
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript () {
        return "ALTER TABLE user_ ADD COLUMN delete_in_progress BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE user_ ADD COLUMN delete_date TIMESTAMP;";
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript () {
        return "ALTER TABLE user_ ADD delete_in_progress BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE user_ ADD delete_date DATETIME;";
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript () {
        return "ALTER TABLE user_ ADD delete_in_progress number(1,0) default 0;\n" +
                "ALTER TABLE user_ ADD delete_date DATE;";
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript () {
        return "ALTER TABLE user_ ADD delete_in_progress TINYINT NOT NULL DEFAULT '0';\n" +
                "ALTER TABLE user_ ADD delete_date DATETIME NULL;";
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script () {
        return "ALTER TABLE user_ ADD delete_in_progress BOOLEAN DEFAULT FALSE;\n" +
                "ALTER TABLE user_ ADD delete_date TIMESTAMP;";
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }

}
