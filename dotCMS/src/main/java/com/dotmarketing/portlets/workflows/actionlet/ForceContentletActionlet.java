
package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

import java.util.List;
import java.util.Map;

public class ForceContentletActionlet extends WorkFlowActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = -3399186955215452961L;


	@Override
	public String getName() {
		return "Don't Validate Contentlet";
	}
	@Override
	public String getHowTo() {

		return "This actionlet will set the DONT_VALIDATE_ME flag on the content, which will let ";
	}
	@Override
	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
	    processor.getContentlet().setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
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
