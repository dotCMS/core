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

/**
 * {@link WorkFlowActionlet} that unlock a {@link Contentlet}
 */
public class CheckinContentActionlet extends WorkFlowActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Unlock content";
	}

	public String getHowTo() {

		return "This actionlet will unlock the content.";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {
			final Contentlet contentlet = processor.getContentlet();

			if (contentlet.isLocked()) {
				contentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
				APILocator.getContentletAPI().unlock(contentlet, processor.getUser(),
						processor.getContentletDependencies() != null
								&& processor.getContentletDependencies().isRespectAnonymousPermissions());
			}
		} catch (Exception e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage(),e);
		
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
