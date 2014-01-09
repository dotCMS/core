package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

public class ArchiveContentActionlet extends ContentActionlet {
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
			super.executeAction(processor, params);
			for(Contentlet c : contentletsToProcess)
				APILocator.getContentletAPI().archive(c, processor.getUser(), false);

		} catch (Exception e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage());
		}
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
