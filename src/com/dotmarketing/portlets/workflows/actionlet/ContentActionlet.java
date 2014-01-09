package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

public abstract class ContentActionlet extends WorkFlowActionlet {
	
	private static final long serialVersionUID = 1L;
	
	protected List<Contentlet> contentletsToProcess = null;
	
	public void executeAction(WorkflowProcessor processor, Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		StringBuffer sb = new StringBuffer();
		sb.append("+identifier: ");
		sb.append(processor.getContentlet().getIdentifier());		
		try {
			contentletsToProcess = APILocator.getContentletAPI().search(sb.toString(), 0, -1, null, processor.getUser(), false);
		} catch (DotDataException e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage());
		} catch (DotSecurityException e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage());
		}
	}
}
