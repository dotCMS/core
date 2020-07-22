package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;

import java.util.List;

public class WorkflowActionView extends WorkflowAction {

    private List<ActionInputView> actionInputs;

    public List<ActionInputView> getActionInputs() {
        return actionInputs;
    }

    public void setActionInputs(List<ActionInputView> actionInputs) {
        this.actionInputs = actionInputs;
    }
}
