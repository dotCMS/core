package com.dotmarketing.portlets.workflows.business;

import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * When a contentlet is being checkin, we double check if the contentlet is on a step. If not the {@link AutoAssignWorkflowDelegate} is called to set the content to a step.
 * User can customize the {@link AutoAssignWorkflowDelegate} in order to use their on implementation when a contentlet is not assigned.
 * @author jsanca
 */
public class UnassignedWorkflowContentletCheckinListener implements EventSubscriber<ContentletCheckinEvent> {

    private static final String AUTO_ASSIGN_WORKFLOW = Contentlet.AUTO_ASSIGN_WORKFLOW;
    private volatile AutoAssignWorkflowDelegate customAutoAssignWorkflowDelegate;
    private final    AutoAssignWorkflowDelegate autoAssignWorkflowDelegate =
            UnassignedWorkflowContentletCheckinListener::assign;

    private static class SingletonHolder {
        private static final UnassignedWorkflowContentletCheckinListener INSTANCE = new UnassignedWorkflowContentletCheckinListener();
    }

    /**
     * Get the instance.
     * @return UnassignedWorkflowContentletCheckinListener
     */
    public static UnassignedWorkflowContentletCheckinListener getInstance() {

        return UnassignedWorkflowContentletCheckinListener.SingletonHolder.INSTANCE;
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

            if (Config.getBooleanProperty(AUTO_ASSIGN_WORKFLOW, true) &&
                    this.validContentType(event.getContentlet()) &&
                    !APILocator.getWorkflowAPI().findCurrentStep(event.getContentlet()).isPresent()) {

                final AutoAssignWorkflowDelegate delegate = null != this.customAutoAssignWorkflowDelegate?
                        this.customAutoAssignWorkflowDelegate: this.autoAssignWorkflowDelegate;
                Logger.debug(this, "Using the auto assign workflow with the delegate: " + delegate.getName());

                delegate.assign(event.getContentlet(), event.getUser());
            }
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }
    } // notify.

    private boolean validContentType(final Contentlet contentlet) {

        // is valid if not host and
        // no auto assign attribute and if exists no in true

        boolean autoAssign = true;

        if (null != contentlet.get(AUTO_ASSIGN_WORKFLOW)) {

            autoAssign = contentlet.getBoolProperty(AUTO_ASSIGN_WORKFLOW); // if AUTO_ASSIGN_WORKFLOW is set and FALSE, won't auto assign
        }

        final ContentType contentType = contentlet.getContentType();
        return null != contentType && !Host.HOST_VELOCITY_VAR_NAME.equals(contentType.variable()) && autoAssign;
    }

    private static void assign (final Contentlet contentlet, final User user) {

        try {

            if (null != contentlet &&
                    UtilMethods.isSet(contentlet.getIdentifier())) {

                final Identifier identifier = APILocator.getIdentifierAPI()
                        .find(contentlet.getIdentifier());

                if (null != identifier && UtilMethods.isSet(identifier.getId())) {

                    final List<WorkflowAction> workflowActions = APILocator.getWorkflowAPI() // todo: see if what you need is the first step of all schemes or if this is ok
                            .findAvailableDefaultActionsByContentType(contentlet.getContentType(), user);

                    if (UtilMethods.isSet(workflowActions)) {

                        APILocator.getWorkflowAPI().saveWorkflowTask(
                                createWorkflowTask(contentlet, user, workflowActions));
                    } else {

                        setToFirstWorkflowStep(contentlet, user);
                    }
                }
            }
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(UnassignedWorkflowContentletCheckinListener.class, e.getMessage(), e);
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

        final String stepName    = UtilHTML.escapeHTMLSpecialChars(workflowStep.getName());
        final String title       = "Auto assign to the step: " + stepName;
        final String description = "The content titled \"" + UtilHTML.escapeHTMLSpecialChars(getTitle(contentlet)) +
                "\" has been moved automatically to the step " + stepName;

        return APILocator.getWorkflowAPI().createWorkflowTask(contentlet, user, workflowStep, title, description);
    }

    private static String getTitle (final Contentlet contentlet) {

        return null != contentlet && null != contentlet.getTitle()? contentlet.getTitle().trim(): "unknown";
    }

    private static void setToFirstWorkflowStep(final Contentlet contentlet, final User user) throws DotDataException {

        final List<WorkflowScheme> schemes = APILocator.getWorkflowAPI().
                findSchemesForContentType(contentlet.getContentType());

        if (UtilMethods.isSet(schemes)) {
            final WorkflowScheme workflowScheme = schemes.get(0);
            final WorkflowStep workflowStep = APILocator.getWorkflowAPI()
                    .findFirstStep(workflowScheme.getId()).get();
            APILocator.getWorkflowAPI().saveWorkflowTask(createWorkflowTask(contentlet, user, workflowStep));
        }
    }
} // E:O:F:UnassignedWorkflowContentletCheckinListener.
