package com.dotmarketing.startup.runonce;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01075AddModDateToWorkflowScheme extends AbstractJDBCStartupTask {
    

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "alter table workflow_scheme add mod_date timestamp;\n"+
               "update workflow_scheme set mod_date = now();\n";
    }

    @Override
    public String getMySQLScript() {
        return "alter table workflow_scheme add mod_date datetime;"+
               "update workflow_scheme set mod_date = now(); \n";
    }

    @Override
    public String getOracleScript() {
        return "alter table workflow_scheme add mod_date timestamp;\n"+
               "update workflow_scheme set mod_date = sysdate;\n";
    }

    @Override
    public String getMSSQLScript() {
        return  "alter table workflow_scheme add mod_date datetime null;\n"+
                "update workflow_scheme set mod_date = getdate();\n";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        List<String> tables=new ArrayList<String>();
        //tables.add("publishing_bundle_environment");
        return tables;
    }

}
