package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to change status_pojo column type from TEXT to LONGTEXT in publishing_queue_audit table
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 12-04-2015
 */
public class Task03505PublishingQueueAuditTable extends AbstractJDBCStartupTask {

    private final String SQL_QUERY = "ALTER TABLE `publishing_queue_audit` CHANGE COLUMN `status_pojo` `status_pojo` LONGTEXT NULL DEFAULT NULL;";

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript() {
        return null;
    }

    /**
     * The SQL for MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript() {
        return SQL_QUERY;
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript() {
        return null;
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript() {
        return null;
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
