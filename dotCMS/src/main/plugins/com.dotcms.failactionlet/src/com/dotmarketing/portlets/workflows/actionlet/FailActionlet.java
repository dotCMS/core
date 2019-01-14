
package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

import java.util.List;
import java.util.Map;

public class FailActionlet extends WorkFlowActionlet implements DotInitializer {

	@Override
	public void init() {

		final WorkflowAPIOsgiService workflowService =
				(WorkflowAPIOsgiService)APILocator.getWorkflowAPI();
		workflowService.addActionlet(FailActionlet.class);
	}

	@Override
	public String getName() {
		return "Fail Workflow";
	}

	@Override
	public String getHowTo() {

		return "This actionlet will fail the pipeline";
	}

	@Override
	public void executeAction(final WorkflowProcessor processor,
							  final Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {

		processor.setTask(null);
		processor.setContentlet(null);
		processor.abortProcessor();
		throw new RuntimeException("Action fail");
	}

	@Override
	public boolean stopProcessing() {

		return true;
	}

	
	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
