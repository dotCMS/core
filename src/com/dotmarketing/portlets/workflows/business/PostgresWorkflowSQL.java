package com.dotmarketing.portlets.workflows.business;

class PostgresWorkflowSQL extends WorkflowSQL{

    public PostgresWorkflowSQL() {
        SELECT_EXPIRED_TASKS =
            "select workflow_task.id from workflow_task join workflow_step on (workflow_task.status=workflow_step.id) "+
            "where workflow_step.resolved=false and workflow_task.escalation_enable=true and now()>workflow_task.mod_date+ "+
            "cast(cast(workflow_step.escalation_time as varchar(20))||' seconds' as interval) ";

    }
}
