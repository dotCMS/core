package com.dotmarketing.portlets.workflows.business;

class MySQLWorkflowSQL extends WorkflowSQL{


	public MySQLWorkflowSQL() {
		INSERT_ACTION_CLASS_PARAM= "insert into workflow_action_class_pars (id,workflow_action_class_id,`key`,value) values (?,?, ?, ?)";
		UPDATE_ACTION_CLASS_PARAM= "update workflow_action_class_pars set workflow_action_class_id= ?, `key`=?, value=? where id =?";
	}

}
