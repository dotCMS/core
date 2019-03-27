package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;

/**
 * View just to encapsulate a scheme and an action for the default workflow actions
 * @author oswaldogallango
 */
public class WorkflowDefaultActionView {

    private final WorkflowScheme scheme;
    private final WorkflowAction action;

    public WorkflowDefaultActionView(WorkflowScheme scheme, WorkflowAction action) {

        this.scheme = scheme;
        this.action = action;
    }

    public WorkflowScheme getScheme() {
        return scheme;
    }

    public WorkflowAction getAction() {
        return action;
    }
}
