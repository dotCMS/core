package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00910AddEscalationFields extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return "alter table workflow_step add column escalation_enable boolean default false;\n"+
                "alter table workflow_step add column escalation_action varchar(36);\n"+
                "alter table workflow_step add column escalation_time int default 0;\n"+
                "alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);\n";
    }
    
    @Override
    public String getMySQLScript() {
        return "alter table workflow_step add escalation_enable boolean default false;\n"+
                "alter table workflow_step add escalation_action varchar(36);\n"+
                "alter table workflow_step add escalation_time int default 0;\n"+
                "alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);\n";
    }
    
    @Override
    public String getOracleScript() {
        return "alter table workflow_step add escalation_enable number(1,0) default 0;\n"+
                "alter table workflow_step add escalation_action varchar(36);\n"+
                "alter table workflow_step add escalation_time number(10,0) default 0;\n"+
                "alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);\n";
    }
    
    @Override
    public String getMSSQLScript() {
        return "alter table workflow_step add escalation_enable tinyint default 0;\n"+
                "alter table workflow_step add escalation_action varchar(36);\n"+
                "alter table workflow_step add escalation_time int default 0;\n"+
                "alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);\n";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
