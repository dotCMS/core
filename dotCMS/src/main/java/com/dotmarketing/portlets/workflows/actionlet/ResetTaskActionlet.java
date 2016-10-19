
package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;

public class ResetTaskActionlet extends WorkFlowActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = -3399186955215452961L;


	@Override
	public String getName() {
		return "Reset Workflow";
	}
	@Override
	public String getHowTo() {

		return "This actionlet will complety delete all workflow task information, including history for the content item and reset the content items workflow state.  It will also STOP all further subaction processing";
	}
	@Override
	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		WorkflowTask task = processor.getTask();
		task.setStatus(null);
		try {
			APILocator.getWorkflowAPI().deleteWorkflowTask(task);

			processor.setTask(null);
		} catch (DotDataException e) {
			Logger.error(ResetTaskActionlet.class,e.getMessage(),e);
		}
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
