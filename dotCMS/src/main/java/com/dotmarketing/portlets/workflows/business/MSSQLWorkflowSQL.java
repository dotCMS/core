package com.dotmarketing.portlets.workflows.business;

 class MSSQLWorkflowSQL extends WorkflowSQL{

     public MSSQLWorkflowSQL() {
         // tweaks for MSSQL because of "key" field in workflow_action_class_pars
         INSERT_ACTION_CLASS_PARAM=super.INSERT_ACTION_CLASS_PARAM.replace("key", "[key]");
         UPDATE_ACTION_CLASS_PARAM=super.UPDATE_ACTION_CLASS_PARAM.replace("key", "[key]");
         
         SELECT_EXPIRED_TASKS =
         "select workflow_task.id from workflow_task join workflow_step on (workflow_task.status=workflow_step.id) "+ 
         "where workflow_step.resolved=0 and workflow_step.escalation_enable='1'  and "+
         "GETDATE()>dateadd(second,workflow_step.escalation_time,workflow_task.mod_date)";
     }
}
