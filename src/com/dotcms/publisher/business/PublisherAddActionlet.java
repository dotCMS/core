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
 * This Actionlet Include/Update content in a Solr Index
 * @author Oswaldo
 *
 */
public class PublisherAddActionlet extends WorkFlowActionlet{

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
		return "Add/Update to Solr Index";
	}

	@Override
	public String getHowTo() {
		return "This actionlet will include/update the content in the SOLR Index";
	}

	/**
	 * Include contentlet in the solr_queue to update the solr index
	 */
	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
	throws WorkflowActionFailureException {
		try {
			Contentlet con = processor.getContentlet();
			solrAPI.addContentToSolr(con);				
		} catch (DotPublisherException e) {
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		}
	}

}
