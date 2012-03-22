package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;

public class TwitterActionlet extends WorkFlowActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Twitter Status Update";
	}

	public String getHowTo() {

		return "This actionlet posts the value of a field or the workflow comments to twitter.  If the \"Field to Post\" is set to the velocity variable of field on the contentlet, the system will post the value in that field to twitter, otherwise, it will post the comments entered into the workflow comment box to twitter.  To get an access token for twittering, see: <a href='https://dev.twitter.com/apps' target='_blank'>https://dev.twitter.com/apps</a> and create a new dotCMS application";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		
		String tweatThis = null;
		try {
			tweatThis = processor.getWorkflowMessage();
			if(!UtilMethods.isSet(tweatThis) && UtilMethods.isSet(params.get("fieldVar").getValue())){
				tweatThis=processor.getContentlet().getStringProperty(params.get("fieldVar").getValue());
			}

			
			if(UtilMethods.isSet(tweatThis)){
				
				if(tweatThis.length() > 140){
					String error = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(), "Tweet-too-long");
					if(error.equals("Tweet-too-long")){
						error = error.replaceAll("-", " ");
					}
					
					throw new DotValidationException(error);
					
				}
				
				
				String consumerKey =null, consumerSecret=null,password=null,userName=null,accessToken=null,accessTokenSecret=null;
				
				consumerKey=params.get("consumerKey").getValue();
				consumerSecret=params.get("consumerSecret").getValue();
				accessToken=params.get("accessToken").getValue();
				accessTokenSecret=params.get("accessTokenSecret").getValue();

				
				
				String path = APILocator.getContentletAPI().getUrlMapForContentlet(processor.getContentlet(), APILocator.getUserAPI().getSystemUser(), false);

				
				
				
				ConfigurationBuilder cb = new ConfigurationBuilder();
				cb.setDebugEnabled(true)
				  .setOAuthConsumerKey(consumerKey)
				  .setOAuthConsumerSecret(consumerSecret)
				  .setOAuthAccessToken(accessToken)
				  .setOAuthAccessTokenSecret(accessTokenSecret);

				TwitterFactory tf = new TwitterFactory(cb.build());
				Twitter twitter = tf.getInstance();


			
				Status stat = twitter.updateStatus(tweatThis);
				
				

				WorkflowComment comment = new WorkflowComment();
				comment.setPostedBy(processor.getUser().getUserId());
				comment.setComment("Tweeted: " + tweatThis + " twitterId:" + stat.getId());
				comment.setWorkflowtaskId(processor.getTask().getId());
				try {
					APILocator.getWorkflowAPI().saveComment(comment);
				} catch (DotDataException e) {
					Logger.error(CommentOnWorkflowActionlet.class,e.getMessage(),e);
				}
				
				
				

			}
			
		} catch (Exception e) {
			Logger.error(TwitterActionlet.class,e.getMessage());
			throw new  WorkflowActionFailureException(e.getMessage());
		
		}
		
		

	}

	public WorkflowStep getNextStep() {

		return null;
	}

	private static List<WorkflowActionletParameter> paramList = null; 
	@Override
	public List<WorkflowActionletParameter> getParameters() {
		if (paramList == null) {
			synchronized (this.getClass()) {
				if (paramList == null) {
					paramList = new ArrayList<WorkflowActionletParameter>();
					//paramList.add(new WorkflowActionletParameter("FieldNameVar", "Field Name Var", null, true));
					paramList.add(new WorkflowActionletParameter("consumerKey", "Consumer Key",  null, true));
					paramList.add(new WorkflowActionletParameter("consumerSecret", "Consumer Secret",  null, true));
					paramList.add(new WorkflowActionletParameter("accessToken", "Access Token",  null, true));
					paramList.add(new WorkflowActionletParameter("accessTokenSecret", "Access Token Secret",  null, true));
					paramList.add(new WorkflowActionletParameter("fieldVar", "Field to Post",  null, false));
					//paramList.add(new WorkflowActionletParameter("userName", "User Name",  null, false));
				}
			}
		}
		return paramList;
	}
}
