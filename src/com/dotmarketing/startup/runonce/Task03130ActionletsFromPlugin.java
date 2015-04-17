package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * Created by Oscar Arrieta on 4/14/15.
 */
public class Task03130ActionletsFromPlugin extends AbstractJDBCStartupTask {

    private static final String UPDATE_QUERY =
            "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.EmailActionlet' " +
                    "where clazz = 'com.dotcms.plugin.email.actionlet.EmailActionlet';\n"
            + "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.SetValueActionlet' " +
                    "where clazz = 'com.dotcms.actionlet.setvalue.SetValueActionlet';\n"
            + "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet' " +
                    "where clazz = 'com.dotcms.plugin.pushnow.actionlet.PushnowActionlet'";

    private static final String UPDATE_QUERY_MSSQL =
            "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.EmailActionlet' " +
                    "where clazz like 'com.dotcms.plugin.email.actionlet.EmailActionlet';\n"
                    + "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.SetValueActionlet' " +
                    "where clazz like 'com.dotcms.actionlet.setvalue.SetValueActionlet';\n"
                    + "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet' " +
                    "where clazz like 'com.dotcms.plugin.pushnow.actionlet.PushnowActionlet'";

    private static final String UPDATE_QUERY_ORACLE =
            "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.EmailActionlet' " +
                    "where to_char(clazz) = 'com.dotcms.plugin.email.actionlet.EmailActionlet';\n"
                    + "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.SetValueActionlet' " +
                    "where to_char(clazz) = 'com.dotcms.actionlet.setvalue.SetValueActionlet';\n"
                    + "update workflow_action_class set clazz = 'com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet' " +
                    "where to_char(clazz) = 'com.dotcms.plugin.pushnow.actionlet.PushnowActionlet'";

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
        return UPDATE_QUERY_ORACLE;
    }

    @Override
    public String getMSSQLScript () {
        return UPDATE_QUERY_MSSQL;
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
