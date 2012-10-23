package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

/**
 * This Actionlet remove content with IMAGE,FILE or BINARY fields from PublishQueueIndex
 * @author Oswaldo
 *
 */
public class PublisherDeleteActionlet extends WorkFlowActionlet{

	private PublisherAPI publisherAPI = PublisherAPI.getInstance();
	ContentletAPI conAPI = APILocator.getContentletAPI();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public List<WorkflowActionletParameter> getParameters() {
		return null;
	}

	@Override
	public String getName() {
		return "Add Content to unpublish";
	}

	@Override
	public String getHowTo() {
		return "This actionlet will add the content to the unpublish";
	}

	/**
	 * add the contentlet to the publish queue
	 */
	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
	throws WorkflowActionFailureException {
		try {
			Contentlet ref = processor.getContentlet();
			List<String> identifiers = new ArrayList<String>();
			
			String bundleId = UUID.randomUUID().toString();
			
			identifiers.add(ref.getIdentifier());
			
			publisherAPI.addContentsToUnpublish(identifiers, bundleId, new Date()); //FIXME
			
		} catch (DotPublisherException e) {
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		}
	}
}
