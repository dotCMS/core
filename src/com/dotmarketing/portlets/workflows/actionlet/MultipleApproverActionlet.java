package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.util.WorkflowEmailUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;

public class MultipleApproverActionlet extends WorkFlowActionlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {

		return "Require Multiple Approvers";
	}

	public String getHowTo() {

		return "This actionlet takes a comma separated list of userIds or "
				+ "user email addresses of users that need to approve this workflow task before it can progress. "
				+ "If eveyone in the list has not approved, this actionlet will send a notification email out to " +
						"users who have not approved and STOP all further subaction processing.";
	}

	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
			throws WorkflowActionFailureException {

		String userIds = (params.get("approvers") == null) ? "" : params.get("approvers").getValue();

		String emailSubject = null;
		String emailBody = null;
		boolean isHtml = false;

		if (params.get("emailSubject") != null) {
			emailSubject = params.get("emailSubject").getValue();
		}
		if (params.get("emailBody") != null) {
			emailBody = params.get("emailBody").getValue();
		}

		if (params.get("isHtml") != null) {
			try {
				isHtml = new Boolean(params.get("isHtml").getValue());
			} catch (Exception e) {

			}
		}

		Set<User> requiredApprovers = new HashSet<User>();
		Set<User> hasApproved = new HashSet<User>();
		StringTokenizer st = new StringTokenizer(userIds, ", ");
		while (st.hasMoreTokens()) {
			String x = st.nextToken();

			if (Validator.isEmailAddress(x)) {
				try {
					User u = APILocator.getUserAPI().loadByUserByEmail(x, APILocator.getUserAPI().getSystemUser(), false);

					requiredApprovers.add(u);
				} catch (Exception e) {
					Logger.error(this.getClass(), "Unable to find user with email:" + x);
				}
			} else {
				try {

					User u = APILocator.getUserAPI().loadUserById(x, APILocator.getUserAPI().getSystemUser(), false);
					requiredApprovers.add(u);
				} catch (Exception e) {
					Logger.error(this.getClass(), "Unable to find user with userID:" + x);
				}

			}
		}
		List<WorkflowHistory> histories = processor.getHistory();
		
		// add this approval to the history
		WorkflowHistory h = new WorkflowHistory();
		h.setActionId(processor.getAction().getId());
		h.setMadeBy(processor.getUser().getUserId());
		if(histories == null){
			histories = new ArrayList<WorkflowHistory>();
			histories.add(h);
		}else histories.add(h);
		
		for (User u : requiredApprovers) {

			for (WorkflowHistory history : histories) {
				if (history.getActionId().equals(processor.getAction().getId())) {
					if (u.getUserId().equals(history.getMadeBy())) {
						hasApproved.add(u);
					}

				}

			}

		}
		
		if (hasApproved.size() < requiredApprovers.size()) {
			
			shouldStop = true;
			// keep the workflow process on the same step
			processor.setNextStep( processor.getStep());
			
			
			// only send emails to users who have not approved
			List<String> emails = new ArrayList<String>();
			for (User u : requiredApprovers) {
				if(!hasApproved.contains(u)){
					emails.add(u.getEmailAddress());					
				}
			}
			
			// to assign it for next assignee
			for (User u : requiredApprovers) {
				if(!hasApproved.contains(u)){					
					try {
	                   processor.setNextAssign(APILocator.getRoleAPI().getUserRole(u));
	                   break;
	                } catch (DotDataException e) {
	                   Logger.error(MultipleApproverActionlet.class,e.getMessage(),e);
	                }
				}
			}
			
			
			
			
			String[] emailsToSend = (String[]) emails.toArray(new String[emails.size()]);

			
			processor.setWorkflowMessage(emailSubject);
			
			WorkflowEmailUtil.sendWorkflowEmail(processor, emailsToSend, emailSubject, emailBody, isHtml);

		}

	}

	@Override
	public boolean stopProcessing(){
		return shouldStop;
	}
	private boolean shouldStop = false;
	
	private static List<WorkflowActionletParameter> paramList = null; 
	@Override
	public List<WorkflowActionletParameter> getParameters() {
		if (paramList == null) {
			synchronized (this.getClass()) {
				if (paramList == null) {
					paramList = new ArrayList<WorkflowActionletParameter>();
					paramList.add(new WorkflowActionletParameter("approvers", "User IDs or Emails", null, true));
					paramList.add(new WorkflowActionletParameter("emailSubject", "Email Subject", "Multiple Approval Required", false));
					paramList.add(new WorkflowActionletParameter("emailBody", "Email Message", null, false));

				}
			}
		}
		return paramList;
	}

}
