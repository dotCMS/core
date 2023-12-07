package com.dotcms.rest.api.v1.workflow;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Provides a simplified view of the status of a Contentlet in a dotCMS Workflow. This is used by
 * the back-end UI, and can be extended as rquired.
 *
 * @author Jose Castro
 * @since Dec 4th, 2023
 */
public class ContentletWorkflowStatusView {

    final WorkflowStep step;
    final WorkflowScheme scheme;
    final WorkflowTask task;

    public ContentletWorkflowStatusView(final WorkflowScheme scheme, final WorkflowStep wfStep,
                                        final WorkflowTask wfTask) {
        this.scheme = scheme;
        this.step = wfStep;
        this.task = handleWorkflowTaskData(wfTask);
    }

    /**
     * Takes the existing Workflow Task object, and replaces the {@code assignedTo} field in the
     * form of a UUID with the actual name of the User or Role to which the task is assigned.
     *
     * @param wfTask The {@link WorkflowTask} object to be modified.
     *
     * @return The modified {@link WorkflowTask} object.
     */
    private WorkflowTask handleWorkflowTaskData(final WorkflowTask wfTask) {
        if (null == wfTask || UtilMethods.isNotSet(wfTask.getId())) {
            return null;
        }
        try {
            final String assignedUserName =
                    APILocator.getRoleAPI().loadRoleById(wfTask.getAssignedTo()).getName();
            wfTask.setAssignedTo(assignedUserName);
        } catch (final DotDataException e) {
            Logger.warn(this.getClass(), String.format("Could not load role with ID '%s': %s",
                    wfTask.getAssignedTo(), ExceptionUtil.getErrorMessage(e)));
        }
        return wfTask;
    }

    public WorkflowScheme getScheme() {
        return this.scheme;
    }

    public WorkflowStep getStep() {
        return this.step;
    }

    public WorkflowTask getTask() {
        return this.task;
    }

}
