package com.dotmarketing.portlets.workflows.business;
 class OracleWorkflowSQL extends WorkflowSQL{

	public OracleWorkflowSQL() {
	    SELECT_EXPIRED_TASKS=
	    "select workflow_task.id from workflow_task join workflow_step on (workflow_task.status=workflow_step.id) "+ 
	    "where workflow_step.resolved=0 and workflow_step.escalation_enable=1 and "+
	    "sysdate>workflow_task.mod_date+NUMTODSINTERVAL(workflow_step.escalation_time,'second')";
	}
	
	

}
