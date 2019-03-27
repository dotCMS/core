package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * This upgrade Task will clean the permission_type on permission table. TemplateLayout canonical name was wrong, we were
 * using com.dotmarketing.portlets.templates.model.TemplateLayout instead com.dotmarketing.portlets.templates.design.bean.TemplateLayout
 *
 * Created by Oscar Arrieta on 8/4/16.
 */
public class Task03560TemplateLayoutCanonicalName extends AbstractJDBCStartupTask {

    private final String SQL_QUERY = "UPDATE permission SET permission_type='com.dotmarketing.portlets.templates.design.bean.TemplateLayout' WHERE permission_type='com.dotmarketing.portlets.templates.model.TemplateLayout'";

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
        return SQL_QUERY;
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
        return SQL_QUERY;
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript() {
        return SQL_QUERY;
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script() {
        return SQL_QUERY;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
