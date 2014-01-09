package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

public class PublishContentActionlet extends ContentActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Publish content";
	}

	public String getHowTo() {

		return "This actionlet will publish the content.";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {			
			super.executeAction(processor, params);
			
			if(processor.getContentlet().isArchived()){
				for(Contentlet c : contentletsToProcess)
					APILocator.getContentletAPI().unarchive(c, processor.getUser(), false);
			}
			
			for(Contentlet c : contentletsToProcess)
				APILocator.getContentletAPI().publish(c, processor.getUser(), false);			

		} catch (Exception e) {
			Logger.error(PublishContentActionlet.class,e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage());
		
		}

	}

	public WorkflowStep getNextStep() {

		return null;
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
