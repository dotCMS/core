package com.dotcms.publisher.business;

import java.util.List;
import java.util.Map;

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

	private PublisherAPI solrAPI = PublisherAPI.getInstance();
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
		return "Remove Content from the PublishQueue Index";
	}

	@Override
	public String getHowTo() {
		return "This actionlet will remove the content from the PublishQueue Index";
	}

	/**
	 * Include contentlet in the solr_queue to remove it from the solr index 
	 */
	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
	throws WorkflowActionFailureException {
		try {
			Contentlet con = processor.getContentlet();
			solrAPI.removeContentFromPublishQueue(con);				
		} catch (DotPublisherException e) {
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		}
	}

}
