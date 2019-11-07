package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
	public void executeAction(final WorkflowProcessor processor,
			final Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {

		try {

			Logger.debug(this, ()-> "The contentlet: " + processor.getContentlet().getIdentifier() +
								", will be archive");
			processor.getContentlet().setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
			APILocator.getContentletAPI().archive(processor.getContentlet(), processor.getUser(), false);
		} catch (Exception e) {

			Logger.error(this.getClass(), e.getMessage(), e);
			throw new  WorkflowActionFailureException(e.getMessage(), e);
		}
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
