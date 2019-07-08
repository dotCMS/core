package com.dotmarketing.portlets.workflows.business;

import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.List;

/**
 * When a contentlet is being checkin, we double check if the contentlet is on a step. If not the {@link AutoAssignWorkflowDelegate} is called to set the content to a step.
 * User can customize the {@link AutoAssignWorkflowDelegate} in order to use their on implementation when a contentlet is not assigned.
 * @author jsanca
 */
public class CheckInUnAssignWorkflowStepCheckerListener implements EventSubscriber<ContentletCheckinEvent> {

    private volatile AutoAssignWorkflowDelegate customAutoAssignWorkflowDelegate;
    private final    AutoAssignWorkflowDelegate autoAssignWorkflowDelegate =
            CheckInUnAssignWorkflowStepCheckerListener::assign;

    private static class SingletonHolder {
        private static final CheckInUnAssignWorkflowStepCheckerListener INSTANCE = new CheckInUnAssignWorkflowStepCheckerListener();
    }

    /**
     * Get the instance.
     * @return CheckInUnAssignWorkflowStepCheckerListener
     */
    public static CheckInUnAssignWorkflowStepCheckerListener getInstance() {

        return CheckInUnAssignWorkflowStepCheckerListener.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Sets a custom {@link AutoAssignWorkflowDelegate} to handle the checkin on unassigned to workflow contentlet
     * @param customAutoAssignWorkflowDelegate
     */
    public void setCustomAutoAssignWorkflowDelegate (final AutoAssignWorkflowDelegate customAutoAssignWorkflowDelegate) {

        if (null != customAutoAssignWorkflowDelegate) {

            this.customAutoAssignWorkflowDelegate = customAutoAssignWorkflowDelegate;
        }
    }

    @Override
    public void notify(final ContentletCheckinEvent event) {

        try {

            if (!APILocator.getWorkflowAPI().findCurrentStep(event.getContentlet()).isPresent()) {

                if (Config.getBooleanProperty("AUTO_ASSIGN_WORKFLOW", true)) {

                    final AutoAssignWorkflowDelegate delegate = null != this.customAutoAssignWorkflowDelegate?
                            this.customAutoAssignWorkflowDelegate: this.autoAssignWorkflowDelegate;
                    Logger.debug(this, "Using the auto assign workflow with the delegate: " + delegate.getName());

                    delegate.assign(event.getContentlet(), event.getUser());
                }
            }
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }
    } // notify.

    private static void assign (final Contentlet contentlet, final User user) {

        List<WorkflowAction> workflowActions = null;

        try {

            workflowActions = APILocator.getWorkflowAPI()
                    .findAvailableDefaultActionsByContentType(contentlet.getContentType(), user);

            if (UtilMethods.isSet(workflowActions) && workflowActions.size() > 0) {

                APILocator.getWorkflowAPI().saveWorkflowTask(
                        createWorkflowTask(contentlet, user, workflowActions));
            } else {

                setToFirstSystemWorkflowStep (contentlet, user);
            }
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(CheckInUnAssignWorkflowStepCheckerListener.class, e.getMessage(), e);
        }
    }

    private static WorkflowTask createWorkflowTask(final Contentlet contentlet,
                                                   final User user,
                                                   final List<WorkflowAction> workflowActions) throws DotDataException {

        final WorkflowAction firstWorkflowAction    = workflowActions.get(0);
        final WorkflowAction selectedWorkflowAction = workflowActions.stream()
                .filter(workflowAction -> workflowAction.getSchemeId().equals(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID))
                .findFirst().orElse(firstWorkflowAction);
        final WorkflowStep   workflowStep           = APILocator.getWorkflowAPI().findFirstStepForAction(selectedWorkflowAction).get();
        return createWorkflowTask(contentlet, user, workflowStep);
    }

    private static WorkflowTask createWorkflowTask(final Contentlet contentlet, final User user,
                                                   final WorkflowStep workflowStep) throws DotDataException {

        final WorkflowTask task                     = new WorkflowTask();
        final String stepName                       = UtilHTML.escapeHTMLSpecialChars(workflowStep.getName());
        task.setTitle("Auto assign to the step: " + stepName);
        task.setDescription("The content titled \"" + UtilHTML.escapeHTMLSpecialChars(contentlet.getTitle().trim()) +
                "\" has been moved automatically to the step " + stepName);

        task.setAssignedTo(APILocator.getRoleAPI().getUserRole(user).getId());
        task.setModDate(new Date());
        task.setCreationDate(new Date());
        task.setCreatedBy(user.getUserId());
        task.setStatus(workflowStep.getId());
        task.setDueDate(null);
        task.setWebasset(contentlet.getIdentifier());
        task.setLanguageId(contentlet.getLanguageId());

        return task;
    }

    private static void setToFirstSystemWorkflowStep(final Contentlet contentlet, final User user) throws DotDataException {

        final WorkflowStep workflowStep = APILocator.getWorkflowAPI()
                .findFirstStep(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID).get();
        APILocator.getWorkflowAPI().saveWorkflowTask(createWorkflowTask(contentlet, user, workflowStep)); // todo: here ask for the NEW step if workflowStep does not exists: the optional
    }
} // E:O:F:CheckInUnAssignWorkflowStepCheckerListener.
