package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 1/27/16
 */
public class Task03525LowerTagsTagname extends AbstractJDBCStartupTask {

    private final String SQL_LOWER_TAG_NAME = "UPDATE tag SET tagname=LOWER(tagname);";

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
        return SQL_LOWER_TAG_NAME;
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript () {
        return SQL_LOWER_TAG_NAME;
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript () {
        return SQL_LOWER_TAG_NAME;
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript () {
        return SQL_LOWER_TAG_NAME;
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script () {
        return SQL_LOWER_TAG_NAME;
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }

}
