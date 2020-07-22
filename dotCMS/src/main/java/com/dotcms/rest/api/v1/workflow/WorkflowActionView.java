package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;

import java.util.List;

/**
 * Self contains Workflow Action, this one is a {@link WorkflowAction} + the action inputs.
 * @author jsanca
 */
public class WorkflowActionView extends WorkflowAction {

    private List<ActionInputView> actionInputs;

    public List<ActionInputView> getActionInputs() {
        return actionInputs;
    }

    public void setActionInputs(List<ActionInputView> actionInputs) {
        this.actionInputs = actionInputs;
    }
}
