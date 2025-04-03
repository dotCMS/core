package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.dotmarketing.portlets.workflows.WorkflowParameter;
import org.apache.velocity.context.Context;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowEmailUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;

import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;

public class NotifyUsersActionlet extends WorkFlowActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	public String getName() {
		return "Notify Users";
	}

	public String getHowTo() {

		return "This actionlet takes a comma separated list of userId, email addresses and/or role keys and this will send them a notification email.";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {

		String emailSubject =null;
		String emailBody =null;
		boolean isHtml = false;
		String customHeaders =null;

		if(params.get("emailSubject") != null ){
			emailSubject = params.get("emailSubject").getValue();
		}
		if(params.get("emailBody") != null ){
			emailBody = params.get("emailBody").getValue();
		}
	
		if(params.get("isHtml") != null ){
			try{
				isHtml = Boolean.valueOf(params.get("isHtml").getValue());
			}
			catch(Exception e){
				
			}
		}
		customHeaders = getParameterValue(params.get(WorkflowParameter.CUSTOM_HEADERS.getKey()));

		String emails = (params.get("emails")== null) ? "" : params.get("emails").getValue();
		List<String> recipients = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(emails, ", ");
		while(st.hasMoreTokens()) {

			String x = st.nextToken();
			
			if(x.contains("$")){
				
				Context ctx = VelocityUtil.getBasicContext();
				ctx.put("workflow", processor);

				try {
					x = VelocityUtil.eval(x, ctx);
				} catch (Exception e) {
					Logger.error(NotifyUsersActionlet.class,e.getMessage(),e);
				}

				continue;
			}

			if(Validator.isEmailAddress(x)) {
				recipients.add(x);
				continue;
			}
			
			try {

				List<User> users = APILocator.getRoleAPI().findUsersForRole(APILocator.getRoleAPI().loadRoleByKey(x), false);
				for(User u : users){
					recipients.add(u.getEmailAddress());
				}
				continue;
			} catch(Exception e) {
				Logger.debug(this.getClass(),"Unable to find role:" + x);
			}
			
			try {
				User u = APILocator.getUserAPI().loadUserById(x, APILocator.getUserAPI().getDefaultUser(), true);
				if(u != null && UtilMethods.isSet(u.getUserId())){
					recipients.add(u.getEmailAddress());
				}
			} catch(Exception e) {
				Logger.debug(this.getClass(),"Unable to find user:" + x);
			}
		}
		
		String[] emailsToSend = (String[]) recipients.toArray(new String[recipients.size()]);
		
		WorkflowEmailUtil.sendWorkflowEmail(processor, emailsToSend, emailSubject, emailBody, isHtml, customHeaders);
	}

	public WorkflowStep getNextStep() {

		return null;
	}

	private static List<WorkflowActionletParameter> paramList = null; 
	
	@Override
	public  List<WorkflowActionletParameter> getParameters() {
		if(paramList ==null){
			synchronized (this.getClass()) {
				if(paramList ==null){
					paramList = new ArrayList<>();
					paramList.add(new WorkflowActionletParameter("emails", "Users, Emails and Roles", null, true));
					paramList.add(new WorkflowActionletParameter("emailSubject", "Email Subject", null, false));
					paramList.add(new WorkflowActionletParameter("emailBody", "Email Message", null, false));
					
					paramList.add(new WorkflowActionletParameter("isHtml", "Is Html?", null, false));
					paramList.add(WorkflowParameter.CUSTOM_HEADERS.toWorkflowActionletParameter());
				}
			}
		}
		return paramList;
	}
}
