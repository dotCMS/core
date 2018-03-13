package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;

/**
 * Workflow State Filter
 * @author jsanca
 */
public class WorkflowStateFilter {

    /**
     * Determine if the content state is valid to the workflow action state's
     * @param contentletStatusOptions ContentletStateOptions
     * @return boolean
     */
    public boolean filter (final WorkflowAction workflowAction,
                           final ContentletStateOptions contentletStatusOptions) {

        if (!contentletStatusOptions.isLocked() && workflowAction.shouldShowOnUnlock()) { // unlocked

            return this.filterAction(workflowAction, contentletStatusOptions);
        } else {

            if (contentletStatusOptions.isCanLock() && contentletStatusOptions.isLocked()
                    && workflowAction.shouldShowOnLock()) {

                return this.filterAction(workflowAction, contentletStatusOptions);
            }
        }

        return false;
    } // filter.

    private boolean filterAction (final WorkflowAction workflowAction,
                                  final ContentletStateOptions contentletStatusOptions) {

        return  (contentletStatusOptions.isNew()      && workflowAction.shouldShowOnNew())         ||
                (contentletStatusOptions.isPublish()  && workflowAction.shouldShowOnPublished())   ||
                (!contentletStatusOptions.isPublish() && !contentletStatusOptions.isArchived()
                                                      && workflowAction.shouldShowOnUnpublished()) ||
                (contentletStatusOptions.isArchived() && workflowAction.shouldShowOnArchived());
    } // filterAction.
} // WorkflowStateFilter.
