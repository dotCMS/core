package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

/**
 * This Actionlet Include/Update content in a PublishQueue Index
 *
 */
public class PublisherAddActionlet extends WorkFlowActionlet{

	private PublisherAPI publisherAPI = PublisherAPI.getInstance();
	private LanguageAPI languagesAPI = APILocator.getLanguageAPI();
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
		return "Add Content to publish queue";
	}

	@Override
	public String getHowTo() {
		return "This actionlet will add the content to the publish queue";
	}

	/**
	 * add the contentlet to the publish queue
	 */
	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
	throws WorkflowActionFailureException {
		try {
			//Gets available languages
			//List<Language> languages = languagesAPI.getLanguages();
			
			Contentlet ref = processor.getContentlet();
			List<Contentlet> contentsLive = new ArrayList<Contentlet>();
			List<Contentlet> contentsWorking = new ArrayList<Contentlet>();
			
			String bundleId = UUID.randomUUID().toString();
			
			//For each language, query for the content
			//for(Language language : languages) {
			try {	
				contentsLive.add(conAPI.findContentletByIdentifier(
								ref.getIdentifier(), 
								true, 
								ref.getLanguageId(), 
								processor.getUser(), false));
			} catch(DotContentletStateException e) {}
			
			try {
				contentsWorking.add(conAPI.findContentletByIdentifier(
						ref.getIdentifier(), 
						false, 
						ref.getLanguageId(), 
						processor.getUser(), false));
			} catch(DotContentletStateException e) {}
			//}
			
			publisherAPI.addContentsToPublishQueue(contentsLive, bundleId, true);
			publisherAPI.addContentsToPublishQueue(contentsWorking, bundleId, false);
			
		} catch (DotPublisherException e) {
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		} catch (DotDataException e) {
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		} catch (DotSecurityException e) {
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		}
	}

}
