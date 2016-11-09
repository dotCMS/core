package com.dotmarketing.portlets.workflows.business;

class MySQLWorkflowSQL extends WorkflowSQL{


	public MySQLWorkflowSQL() {
		INSERT_ACTION_CLASS_PARAM= "insert into workflow_action_class_pars (id,workflow_action_class_id,`key`,value) values (?,?, ?, ?)";
		UPDATE_ACTION_CLASS_PARAM= "update workflow_action_class_pars set workflow_action_class_id= ?, `key`=?, value=? where id =?";
		
		SELECT_EXPIRED_TASKS=
		"select workflow_task.id from workflow_task join workflow_step "+ 
		"on (workflow_task.status=workflow_step.id) "+
		"where workflow_step.resolved='0' and workflow_step.escalation_enable='1' and "+
		"now()>workflow_task.mod_date+interval workflow_step.escalation_time second";
	}

}
