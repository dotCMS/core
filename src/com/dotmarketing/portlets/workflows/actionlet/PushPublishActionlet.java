package com.dotmarketing.portlets.workflows.actionlet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.business.PublisherAddActionlet;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

public class PushPublishActionlet extends WorkFlowActionlet {

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
			String _contentPushPublishDate = ref.getStringProperty("wfPublishDate");
			String _contentPushPublishTime = ref.getStringProperty("wfPublishTime");
			String _contentPushExpireDate = ref.getStringProperty("wfExpireDate");
			String _contentPushExpireTime = ref.getStringProperty("wfExpireTime");
			boolean _contentPushNeverExpire = "on".equals(ref.getStringProperty("wfNeverExpire"))?true:false;

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-H-m");
			Date publishDate = dateFormat.parse(_contentPushPublishDate+"-"+_contentPushPublishTime);
			
			List<String> identifiers = new ArrayList<String>();			
			String bundleId = UUID.randomUUID().toString();			
			identifiers.add(ref.getIdentifier());
			
			publisherAPI.addContentsToPublish(identifiers, bundleId, publishDate);
			if(!_contentPushNeverExpire && (!"".equals(_contentPushExpireDate.trim()) && !"".equals(_contentPushExpireTime.trim()))){
				bundleId = UUID.randomUUID().toString();
				Date expireDate = dateFormat.parse(_contentPushExpireDate+"-"+_contentPushExpireTime);
				publisherAPI.addContentsToUnpublish(identifiers, bundleId, expireDate);
			}
		} catch (DotPublisherException e) {
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		} catch (ParseException e){
			Logger.debug(PublisherAddActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());			
		}
	}

}
