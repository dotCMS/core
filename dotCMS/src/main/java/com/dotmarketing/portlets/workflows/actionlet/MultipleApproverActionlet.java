package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.WorkflowParameter;
import com.dotmarketing.portlets.workflows.model.MultiEmailParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryState;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryType;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.util.WorkflowEmailUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.liferay.util.Validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;

/**
 * Based on a list of email, userid or roles won't continue the pipeline until all necessary users approve the workflow
 */
public class MultipleApproverActionlet extends WorkFlowActionlet {

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

		final String userIds = (params.get("approvers") == null) ? "" : params.get("approvers").getValue();
		String emailSubject  = null;
		String emailBody     = null;
		boolean isHtml       = false;
		String customHeaders =null;

		if (params.get("emailSubject") != null) {
			emailSubject = params.get("emailSubject").getValue();
		}
		if (params.get("emailBody") != null) {
			emailBody    = params.get("emailBody").getValue();
		}

		if (params.get("isHtml") != null) {
			try {
				isHtml = Boolean.valueOf(params.get("isHtml").getValue());
			} catch (Exception e) {

			}
		}
		customHeaders = getParameterValue(params.get(WorkflowParameter.CUSTOM_HEADERS.getKey()));

		final Set<User> requiredApprovers = new HashSet<>();
		final Set<User> hasApproved       = new HashSet<>();
		final StringTokenizer userIdTokenizer = new StringTokenizer(userIds, StringPool.COMMA);
		while (userIdTokenizer.hasMoreTokens()) {

			final String userIdToken = userIdTokenizer.nextToken();

			if (Validator.isEmailAddress(userIdToken)) {
				try {

					final User user = APILocator.getUserAPI().loadByUserByEmail(userIdToken,
							APILocator.getUserAPI().getSystemUser(), false);

					requiredApprovers.add(user);
				} catch (Exception e) {

					Logger.warnAndDebug(this.getClass(), "Unable to find user with email:" + userIdToken
							+ ", message: " + e.getMessage(), e);
				}
			} else {
				try {

					final User user = APILocator.getUserAPI().loadUserById(userIdToken,
							APILocator.getUserAPI().getSystemUser(), false);
					requiredApprovers.add(user);
				} catch (Exception e) {
					Logger.error(this.getClass(), "Unable to find user with userID:" + userIdToken);
				}

			}
		}

		List<WorkflowHistory> histories = processor.getHistory();
		
		// add this approval to the history
		final WorkflowHistory workflowHistory = new WorkflowHistory();
		workflowHistory.setActionId(processor.getAction().getId());
		workflowHistory.setMadeBy(processor.getUser().getUserId());
		histories = histories == null?new ArrayList<>():histories;
		histories.add(workflowHistory);
		
		for (final User requiredApprover : requiredApprovers) {
			for (final WorkflowHistory history : histories) {

				final Map<String, Object> changeMap = history.getChangeMap();
				if (history.getActionId().equals(processor.getAction().getId()) && // if it is the action id and it is not reset.
						!WorkflowHistoryState.RESET.name().equals(changeMap.get("state"))) {

					if (requiredApprover.getUserId().equals(history.getMadeBy())) {

						hasApproved.add(requiredApprover);
					}
				}
			}
		}
		
		if (hasApproved.size() < requiredApprovers.size()) {
			
			shouldStop = true;
			// keep the workflow process on the same step
			processor.setNextStep( processor.getStep());

			// only send emails to users who have not approved
			final List<String> emails = new ArrayList<>();
			for (final User user : requiredApprovers) {
				if(!hasApproved.contains(user)){
					emails.add(user.getEmailAddress());
				}
			}
			
			// to assign it for next assignee
			for (final User requiredApprover : requiredApprovers) {
				if(!hasApproved.contains(requiredApprover)){
					try {
	                   processor.setNextAssign(APILocator.getRoleAPI().getUserRole(requiredApprover));
	                   break;
	                } catch (DotDataException e) {
	                   Logger.error(MultipleApproverActionlet.class,e.getMessage(),e);
	                }
				}
			}

			final String[] emailsToSend = emails.toArray(new String[emails.size()]);
			processor.setWorkflowMessage(emailSubject);
			WorkflowEmailUtil.sendWorkflowEmail(processor, emailsToSend, emailSubject, emailBody, isHtml, customHeaders);
		}

		processor.getContextMap().put("type", WorkflowHistoryType.APPROVAL);
	}

	@Override
	public boolean stopProcessing(){
		return shouldStop;
	}
	private boolean shouldStop = false;
	
	private static ArrayList<WorkflowActionletParameter> paramList = null; 
	@Override
	public List<WorkflowActionletParameter> getParameters() {
		if (paramList == null) {
			synchronized (this.getClass()) {
				if (paramList == null) {
					paramList = new ArrayList<>();
					paramList.add(new MultiEmailParameter("approvers", "User IDs or Emails", null, true));
					paramList.add(new WorkflowActionletParameter("emailSubject", "Email Subject", "Multiple Approval Required", false));
					paramList.add(new WorkflowActionletParameter("emailBody", "Email Message", null, false));
					paramList.add(WorkflowParameter.CUSTOM_HEADERS.toWorkflowActionletParameter());
				}
			}
		}
		return paramList;
	}

}
