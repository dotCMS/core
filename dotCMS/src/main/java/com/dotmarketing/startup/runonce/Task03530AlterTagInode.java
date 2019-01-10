package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 2/5/16
 */
public class Task03530AlterTagInode extends AbstractJDBCStartupTask {

    private final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

    @Override
    public boolean forceRun () {
        return !databaseMetaData.existsColumns(DbConnectionFactory.getConnection(), "tag_inode", "field_var_name");
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript () {
        return "ALTER TABLE tag_inode ADD COLUMN field_var_name varchar(255);";
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript () {
        return "ALTER TABLE tag_inode ADD field_var_name varchar(255);";
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript () {
        return "ALTER TABLE tag_inode ADD field_var_name varchar2(255);";
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript () {
        return "ALTER TABLE tag_inode ADD field_var_name varchar(255);";
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script () {
        return "ALTER TABLE tag_inode ADD COLUMN field_var_name varchar(255);";
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }

}