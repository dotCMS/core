package com.dotmarketing.startup.runonce;

import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 16/01/15
 */
public class Task03105HTMLPageGenericPermissions extends AbstractJDBCStartupTask {

    private static final String UPDATE_QUERY =
            "update permission_reference set permission_type = "
                    + "'" + IHTMLPage.class.getCanonicalName() + "'"
                    + " where permission_type = 'com.dotmarketing.portlets.htmlpages.model.HTMLPage';\n" +
            "update permission set permission_type = "
                    + "'" + IHTMLPage.class.getCanonicalName() + "'"
                    + " where permission_type = 'com.dotmarketing.portlets.htmlpages.model.HTMLPage';";

    @Override
    public boolean forceRun () {
        return true;
    }

    @Override
    public String getPostgresScript () {
        return UPDATE_QUERY;
    }

    @Override
    public String getMySQLScript () {
        return UPDATE_QUERY;
    }

    @Override
    public String getOracleScript () {
        return UPDATE_QUERY;
    }

    @Override
    public String getMSSQLScript () {
        return UPDATE_QUERY;
    }

    @Override
    public String getH2Script () {
        return UPDATE_QUERY;
    }

    @Override
    protected List<String> getTablesToDropConstraints () {
        return null;
    }


}