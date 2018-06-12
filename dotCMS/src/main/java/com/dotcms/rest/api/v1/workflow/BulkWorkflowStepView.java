package com.dotcms.rest.api.v1.workflow;

import java.util.List;

public class BulkWorkflowStepView {

    private final CountWorkflowStep step;
    private final List<CountWorkflowAction> actions;

    BulkWorkflowStepView(final CountWorkflowStep step, final List<CountWorkflowAction> actions) {
        this.step = step;
        this.actions = actions;
    }

    public CountWorkflowStep getStep() {
        return step;
    }

    public List<CountWorkflowAction> getActions() {
        return actions;
    }
}
