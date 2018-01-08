package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.MultiEmailParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.util.WorkflowEmailUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Sometimes, customers would like content to be published if at least 2 people (4 eyes) approve
 * their content. They are not particular as to which users, they just need 2 people to approve it
 * before the content goes live. This actionlet enforces this principle.
 * <br>
 * This actionlet allows a user to optionally specify particular users, and also select users with
 * the "4 Eye" publishing permissions. Then,
 * all it would take is 2 of those users to approve. This way, the content would be published.
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Jan 3, 2018
 */
public class FourEyeApproverActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1177885642438262884L;

    private static final String ACTIONLET_NAME = "'4 Eye' Approval";
    private static final String HOW_TO =
            "This actionlet implements the '4 Eyes' principle when verifying content that will be published, and also a comma-separated list of user IDs or user email addresses. "
                    + "This means that a least two people which have the '4 Eye' publishing permission (or the explicitly specified users) need to approve a given content before it can be published.";
    private static final String USER_ID_DELIMITER = ",";
    private static final String FOUR_EYES_ROLE_KEY = "four-eyes-approval";

    private boolean shouldStop = false;

    private static final Object LOCK = new Object();
    private static ArrayList<WorkflowActionletParameter> ACTIONLET_PARAMETERS = null;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        if (null == ACTIONLET_PARAMETERS) {
            synchronized (LOCK) {
                if (null == ACTIONLET_PARAMETERS) {
                    ACTIONLET_PARAMETERS = new ArrayList<>();
                    ACTIONLET_PARAMETERS
                            .add(new MultiEmailParameter("approvers",
                                    "User IDs, Emails, or Role Keys", null,
                                    true));
                    ACTIONLET_PARAMETERS
                            .add(new WorkflowActionletParameter("minimumApprovers",
                                    "Number of Approvers",
                                    "2", true));
                    ACTIONLET_PARAMETERS
                            .add(new WorkflowActionletParameter("emailSubject", "Email Subject",
                                    "'4 Eye' Approval Required", false));
                    ACTIONLET_PARAMETERS
                            .add(new WorkflowActionletParameter("emailBody", "Email Message", null,
                                    false));
                }
            }
        }
        return ACTIONLET_PARAMETERS;
    }

    @Override
    public String getName() {
        return ACTIONLET_NAME;
    }

    @Override
    public String getHowTo() {
        return HOW_TO;
    }

    @Override
    public boolean stopProcessing() {
        return this.shouldStop;
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
            final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {
        final String userIds =
                (null == params.get("approvers")) ? "" : params.get("approvers").getValue();
        final int minimumApprovers = Integer.parseInt(params.get("minimumApprovers").getValue());
        final String emailSubject =
                (null == params.get("emailSubject")) ? "" : params.get("emailSubject").getValue();
        final String emailBody =
                (null == params.get("emailBody")) ? "" : params.get("emailBody").getValue();
        boolean isHtml = false;
        if (null != params.get("isHtml")) {
            try {
                isHtml = new Boolean(params.get("isHtml").getValue());
            } catch (Exception e) {
                // Boolean casting failed. Just use default value
            }
        }

        final Set<User> requiredUserApprovers = new HashSet<>();
        final Set<User> hasApproved = new HashSet<>();
        final StringTokenizer tokenizer = new StringTokenizer(userIds, USER_ID_DELIMITER);
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken().trim();
            if (Validator.isEmailAddress(token)) {
                try {
                    User user = APILocator.getUserAPI()
                            .loadByUserByEmail(token, APILocator.getUserAPI().getSystemUser(),
                                    false);
                    requiredUserApprovers.add(user);
                } catch (Exception e) {
                    Logger.warn(this.getClass(), "Unable to find user with email: " + token);
                }
            } else {
                try {
                    User user = APILocator.getUserAPI()
                            .loadUserById(token, APILocator.getUserAPI().getSystemUser(), false);
                    requiredUserApprovers.add(user);
                    continue;
                } catch (Exception e) {
                    Logger.warn(this.getClass(), "Unable to find user with userID: " + token);
                }
                try {
                    final Role role = APILocator.getRoleAPI().loadRoleByKey(token);
                    try {
                        final List<User> approvingUsersInRole = APILocator.getRoleAPI().findUsersForRole(role);
                        requiredUserApprovers.addAll(approvingUsersInRole);
                    } catch (DotSecurityException e) {
                        Logger.warn(this.getClass(), "Unable to find role with key: " + token);
                    }
                } catch (DotDataException e) {
                    // The specified role could not be found
                    Logger.warn(this.getClass(), "Unable to find role with key: " + token);
                }
            }
        }

        List<WorkflowHistory> historyList = processor.getHistory();
        // add this approval to the history
        final WorkflowHistory history = new WorkflowHistory();
        history.setActionId(processor.getAction().getId());
        history.setMadeBy(processor.getUser().getUserId());
        if (null == historyList) {
            historyList = new ArrayList<>();
        }
        historyList.add(history);
        for (final User user : requiredUserApprovers) {
            for (final WorkflowHistory historyItem : historyList) {
                if (historyItem.getActionId().equals(processor.getAction().getId())) {
                    if (user.getUserId().equals(historyItem.getMadeBy())) {
                        hasApproved.add(user);
                    }
                }
            }
        }

        if (hasApproved.size() < minimumApprovers) {
            this.shouldStop = true;
            // keep the workflow process on the same step
            processor.setNextStep(processor.getStep());

            // only send emails to users who have not approved
            final List<String> emails = new ArrayList<>();
            for (final User user : requiredUserApprovers) {
                if (!hasApproved.contains(user)) {
                    emails.add(user.getEmailAddress());
                }
            }

            // to assign it for next assignee
            for (final User user : requiredUserApprovers) {
                if (!hasApproved.contains(user)) {
                    try {
                        processor.setNextAssign(APILocator.getRoleAPI().getUserRole(user));
                        break;
                    } catch (DotDataException e) {
                        Logger.error(this, e.getMessage(), e);
                    }
                }
            }

            final String[] emailsToSend = emails.toArray(new String[emails.size()]);
            processor.setWorkflowMessage(emailSubject);
            WorkflowEmailUtil
                    .sendWorkflowEmail(processor, emailsToSend, emailSubject, emailBody, isHtml);
        }
    }

}
