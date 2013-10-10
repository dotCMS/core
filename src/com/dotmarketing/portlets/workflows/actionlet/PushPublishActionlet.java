package com.dotmarketing.portlets.workflows.actionlet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
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
		return "Remote Publish";
	}

	@Override
	public String getHowTo() {
		return "This actionlet will add the content to the remote publish queue";
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
			boolean _contentPushNeverExpire = "on".equals(ref.getStringProperty("wfNeverExpire")) || "true".equals(ref.getStringProperty("wfNeverExpire"))?true:false;
			String whoToSendTmp = ref.getStringProperty( "whereToSend" );
            String forcePushStr = ref.getStringProperty( "forcePush" );
            boolean forcePush = (forcePushStr!=null && forcePushStr.equals("true"));
            List<String> whereToSend = Arrays.asList(whoToSendTmp.split(","));
            List<Environment> envsToSendTo = new ArrayList<Environment>();

            // Lists of Environments to push to
            for (String envId : whereToSend) {
            	Environment e = APILocator.getEnvironmentAPI().findEnvironmentById(envId);

            	if(e!=null) {
            		envsToSendTo.add(e);
            	}
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-H-m");
			Date publishDate = dateFormat.parse(_contentPushPublishDate+"-"+_contentPushPublishTime);

			List<String> identifiers = new ArrayList<String>();
			identifiers.add(ref.getIdentifier());

			Bundle bundle = new Bundle(null, publishDate, null, processor.getUser().getUserId(), forcePush);
        	APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);

			publisherAPI.addContentsToPublish(identifiers, bundle.getId(), publishDate, processor.getUser());
			if(!_contentPushNeverExpire && (!"".equals(_contentPushExpireDate.trim()) && !"".equals(_contentPushExpireTime.trim()))){
				Date expireDate = dateFormat.parse(_contentPushExpireDate+"-"+_contentPushExpireTime);
				bundle = new Bundle(null, publishDate, expireDate, processor.getUser().getUserId(), forcePush);
            	APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);
				publisherAPI.addContentsToUnpublish(identifiers, bundle.getId(), expireDate, processor.getUser());
			}
		} catch (DotPublisherException e) {
			Logger.debug(PushPublishActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		} catch (ParseException e){
			Logger.debug(PushPublishActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		} catch (DotDataException e) {
			Logger.debug(PushPublishActionlet.class, e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		}

	}

}
