package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;

/**
 * Unpublish a content if this one has a live version
 */
@Actionlet(unpublish = true)
public class UnpublishContentActionlet extends WorkFlowActionlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Unpublish content";
	}

	public String getHowTo() {

		return "This actionlet will unpublish the content.";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {

			//Verify if there is something to unpublish
			final boolean hasLiveVersion = APILocator.getVersionableAPI()
					.hasLiveVersion(processor.getContentlet());

			if (hasLiveVersion) {

				processor.getContentlet().setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
				APILocator.getContentletAPI().unpublish(processor.getContentlet(), processor.getUser(), false);
				Logger.debug(this, ()->"Unpublished: " + processor.getContentlet().getIdentifier());
			} else {

				Logger.debug(this, ()->"Unpublishing, already unpublish does not need: " + processor.getContentlet().getIdentifier());
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
