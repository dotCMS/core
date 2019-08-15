package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;

@Actionlet(archive = true)
public class ArchiveContentActionlet extends WorkFlowActionlet {
    private static final long serialVersionUID = 6953016451278627341L;

    @Override
    public String getName() {
		return "Archive content";
	}

    @Override
	public String getHowTo() {

		return "This actionlet will archive the content.";
	}

	@Override
	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {

			APILocator.getContentletAPI().archive(processor.getContentlet(), processor.getUser(), false);

		} catch (Exception e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage(),e);
		}
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
