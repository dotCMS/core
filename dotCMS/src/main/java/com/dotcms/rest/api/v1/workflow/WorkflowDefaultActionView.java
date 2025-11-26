package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;

/**
 * View just to encapsulate a scheme and an action for the default workflow actions
 * @author oswaldogallango
 */
public class WorkflowDefaultActionView {

    private final WorkflowScheme scheme;
    private final WorkflowAction action;
    private final WorkflowStep firstStep;

    public WorkflowDefaultActionView(final WorkflowScheme scheme, final WorkflowAction action, final WorkflowStep step) {

        this.scheme = scheme;
        this.action = action;
        this.firstStep = step;
    }

    public WorkflowScheme getScheme() {
        return scheme;
    }

    public WorkflowAction getAction() {
        return action;
    }

    public WorkflowStep getFirstStep() {
        return firstStep;
    }
}
