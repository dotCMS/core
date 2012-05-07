package com.dotmarketing.portlets.workflows.business;

 class MSSQLWorkflowSQL extends WorkflowSQL{

     public MSSQLWorkflowSQL() {
         // tweaks for MSSQL because of "key" field in workflow_action_class_pars
         INSERT_ACTION_CLASS_PARAM=super.INSERT_ACTION_CLASS_PARAM.replace("key", "[key]");
         UPDATE_ACTION_CLASS_PARAM=super.UPDATE_ACTION_CLASS_PARAM.replace("key", "[key]");
     }
}
