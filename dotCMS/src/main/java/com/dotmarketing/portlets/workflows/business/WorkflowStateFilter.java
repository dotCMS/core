package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;

/**
 * Workflow State Filter
 * @author jsanca
 */
public class WorkflowStateFilter {

    /**
     * Determine if the content state is valid to the workflow action state's
     *
     * (lock or unlock) and (new or ( publish or (unpublish or non-archived) or archived )
     *
     * @param contentletStatusOptions ContentletStateOptions
     * @return boolean
     */
    public boolean filter (final WorkflowAction workflowAction,
                           final ContentletStateOptions contentletStatusOptions) {

        final WorkflowAPI.RenderMode renderMode = contentletStatusOptions.getRenderMode();

        if ((renderMode == WorkflowAPI.RenderMode.EDITING && workflowAction.shouldShowOnEdit()) ||
                (renderMode == WorkflowAPI.RenderMode.LISTING && workflowAction.shouldShowOnListing())) {

            if (!contentletStatusOptions.isLocked() && workflowAction.shouldShowOnUnlock()) { // unlocked

                return this.filterAction(workflowAction, contentletStatusOptions);
            } else {

                if (contentletStatusOptions.isCanLock() && contentletStatusOptions.isLocked()
                        && workflowAction.shouldShowOnLock()) {

                    return this.filterAction(workflowAction, contentletStatusOptions);
                }
            }
        }

        return false;
    } // filter.

    private boolean filterAction (final WorkflowAction workflowAction,
                                  final ContentletStateOptions contentletStatusOptions) {

        final boolean unpublish =
                !contentletStatusOptions.isPublish() && !contentletStatusOptions.isArchived();

        return  contentletStatusOptions.isNew()?
                    workflowAction.shouldShowOnNew():
                    (contentletStatusOptions.isPublish()  && workflowAction.shouldShowOnPublished())   ||
                    (unpublish                            && workflowAction.shouldShowOnUnpublished()) ||
                    (contentletStatusOptions.isArchived() && workflowAction.shouldShowOnArchived());
    } // filterAction.
} // WorkflowStateFilter.
