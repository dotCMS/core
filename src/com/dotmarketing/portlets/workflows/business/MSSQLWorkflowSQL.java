package com.dotmarketing.portlets.workflows.business;

 class MSSQLWorkflowSQL extends WorkflowSQL{

     // tweaks for MSSQL because of "key" field in workflow_action_class_pars
     protected static String INSERT_ACTION_CLASS_PARAM= "insert into workflow_action_class_pars (id,workflow_action_class_id,[key],value) values (?,?, ?, ?)";
     protected static String UPDATE_ACTION_CLASS_PARAM= "update workflow_action_class_pars set workflow_action_class_id= ?, [key]=?, value=? where id =?";
     
}
