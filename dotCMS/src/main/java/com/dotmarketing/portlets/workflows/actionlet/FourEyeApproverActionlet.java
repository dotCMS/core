package com.dotmarketing.portlets.workflows.actionlet;

import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getApproversFromHistory;
import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;
import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getUsersFromIds;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.MultiUserReferenceParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.util.WorkflowEmailUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sometimes, customers would like content to be published if a specific number of people approve
 * their content. They are not particular as to which users, they just need a specific number of
 * users to approve it before the content goes live. This actionlet enforces what is called the
 * '4-Eyes' principle.
 * <p>
 * This workflow actionlet allows a user to specify the user IDs, email addresses, or role keys
 * (i.e., all the users assigned to those roles) which will be in charge of approving the new
 * content. If the users that approve the content are greater or equal to the specified number of
 * minimum approvers, then the content will move on to the next actionlet in the workflow.
 * Otherwise, an email will be sent to all users that haven't approved the content yet.
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Jan 3, 2018
 */
public class FourEyeApproverActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1177885642438262884L;

    private static final String ACTIONLET_NAME = "'4 Eye' Approval";
    private static final String HOW_TO =
            "This actionlet implements the '4 Eyes' principle for verifying new content. It takes a comma-separated list of user IDs, email addresses, or role keys "
                    + "(i.e., the users assigned to those roles) which will be in charge of reviewing the content, and a minimum number of approvers. "
                    + "If the number of approvers is greater or equal than the specified value, the next sub-action will be executed.";
    private static final String USER_ID_DELIMITER = ",";
    private static final String PARAM_CONTENT_APPROVERS = "approvers";
    private static final String PARAM_MINIMUM_CONTENT_APPROVERS = "minimumApprovers";
    private static final String PARAM_EMAIL_SUBJECT = "emailSubject";
    private static final String PARAM_EMAIL_BODY = "emailBody";
    private static final String PARAM_IS_HTML = "isHtml";

    private static final int DEFAULT_MINIMUM_CONTENT_APPROVERS = 2;

    private boolean shouldStop = false;

    private static ArrayList<WorkflowActionletParameter> ACTIONLET_PARAMETERS = null;

    @Override
    public synchronized List<WorkflowActionletParameter> getParameters() {
        if (null == ACTIONLET_PARAMETERS) {
            ACTIONLET_PARAMETERS = new ArrayList<>();
            ACTIONLET_PARAMETERS
                    .add(new MultiUserReferenceParameter(PARAM_CONTENT_APPROVERS,
                            "User IDs, Emails, or Role Keys", null,
                            true));
            ACTIONLET_PARAMETERS
                    .add(new WorkflowActionletParameter(PARAM_MINIMUM_CONTENT_APPROVERS,
                            "Number of Approvers",
                            String.valueOf(DEFAULT_MINIMUM_CONTENT_APPROVERS), true));
            ACTIONLET_PARAMETERS
                    .add(new WorkflowActionletParameter(PARAM_EMAIL_SUBJECT,
                            "Email Subject",
                            "'4 Eye' Approval Required", true));
            ACTIONLET_PARAMETERS
                    .add(new WorkflowActionletParameter(PARAM_EMAIL_BODY, "Email Message",
                            null,
                            false));
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
    public boolean equals(Object obj) {
        if (null != obj && obj instanceof WorkFlowActionlet) {
            return getClass().equals(obj.getClass());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (shouldStop ? 1 : 0);
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
            final Map<String, WorkflowActionClassParameter> params) {
        final String userIds = getParameterValue(params.get(PARAM_CONTENT_APPROVERS));
        final int minimumContentApprovers = ConversionUtils
                .toInt(getParameterValue(params.get(PARAM_MINIMUM_CONTENT_APPROVERS)),
                        DEFAULT_MINIMUM_CONTENT_APPROVERS);
        final String emailSubject = getParameterValue(params.get(PARAM_EMAIL_SUBJECT));
        final String emailBody = getParameterValue(params.get(PARAM_EMAIL_BODY));
        final boolean isHtml = getParameterValue(params.get(PARAM_IS_HTML), false);
        final Set<User> requiredContentApprovers = getUsersFromIds(userIds, USER_ID_DELIMITER);
        // Add this approval to the history
        final WorkflowHistory history = new WorkflowHistory();
        history.setActionId(processor.getAction().getId());
        history.setMadeBy(processor.getUser().getUserId());
        List<WorkflowHistory> historyList = processor.getHistory();
        if (null == historyList) {
            historyList = new ArrayList<>();
        }
        historyList.add(history);
        final Set<User> hasApproved = getApproversFromHistory(historyList, requiredContentApprovers,
                processor.getAction().getId(), minimumContentApprovers);
        if (hasApproved.size() < minimumContentApprovers) {
            this.shouldStop = true;
            // Keep the workflow process on the same step
            processor.setNextStep(processor.getStep());
            // Send email to users who have NOT approved only
            final List<String> emails = new ArrayList<>();
            for (final User user : requiredContentApprovers) {
                if (!hasApproved.contains(user)) {
                    emails.add(user.getEmailAddress());
                }
            }
            // Assign the workflow step for next assignee
            for (final User user : requiredContentApprovers) {
                if (!hasApproved.contains(user)) {
                    try {
                        processor.setNextAssign(APILocator.getRoleAPI().getUserRole(user));
                        break;
                    } catch (DotDataException e) {
                        Logger.error(this,
                                "An error occurred when reassigning workflow step to user '" + user
                                        .getUserId() + "': " + e.getMessage(), e);
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
