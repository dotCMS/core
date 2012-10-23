package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

public class PushPublishActionlet extends WorkFlowActionlet {

	@Override
	public List<WorkflowActionletParameter> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Push Publisher";
	}

	@Override
	public String getHowTo() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void executeAction(WorkflowProcessor processor,
			Map<String, WorkflowActionClassParameter> params)
			throws WorkflowActionFailureException {
		// TODO Auto-generated method stub

	}

}
