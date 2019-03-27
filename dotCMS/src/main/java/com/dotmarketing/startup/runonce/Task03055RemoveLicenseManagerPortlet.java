package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 9/10/14
 */
public class Task03055RemoveLicenseManagerPortlet extends AbstractJDBCStartupTask {

    private final String DELETE_QUERY = "delete from cms_layouts_portlets where portlet_id = 'EXT_LICENSE_MANAGER';";

    @Override
    public String getPostgresScript () {
        return DELETE_QUERY;
    }

    @Override
    public String getMySQLScript () {
        return DELETE_QUERY;
    }

    @Override
    public String getOracleScript () {
        return DELETE_QUERY;
    }

    @Override
    public String getMSSQLScript () {
        return DELETE_QUERY;
    }

    @Override
    public String getH2Script () {
        return DELETE_QUERY;
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }

    @Override
    public boolean forceRun () {
        return true;
    }

}