package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;

import java.io.Serializable;

/**
 * Provides a simplified view of the status of a Contentlet in a dotCMS Workflow. This is used by
 * the back-end UI, and can be extended as rquired.
 *
 * @author Jose Castro
 * @since Dec 4th, 2023
 */
public class ContentletWorkflowStatusView implements Serializable {

    final WorkflowStep step;
    final WorkflowScheme scheme;
    final WorkflowTask task;

    public ContentletWorkflowStatusView(final WorkflowScheme scheme, final WorkflowStep wfStep,
                                        final WorkflowTask wfTask) {
        this.scheme = scheme;
        this.step = wfStep;
        this.task = wfTask;
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
