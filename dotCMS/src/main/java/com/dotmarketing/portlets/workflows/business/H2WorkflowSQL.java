package com.dotmarketing.portlets.workflows.business;

public class H2WorkflowSQL extends WorkflowSQL {
    public H2WorkflowSQL() {
        SELECT_EXPIRED_TASKS =
            "select workflow_task.id from workflow_task join workflow_step on (workflow_task.status=workflow_step.id) "+
            "where workflow_step.resolved=false and workflow_step.escalation_enable=true "
           +" and now() > dateadd('second',workflow_step.escalation_time,workflow_task.mod_date)";

    }
}
