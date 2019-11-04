package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowScheme;

import java.util.List;

public class BulkWorkflowSchemeView {

    private final WorkflowScheme scheme;
    private final List<BulkWorkflowStepView> steps;

    public BulkWorkflowSchemeView(final WorkflowScheme scheme, final List<BulkWorkflowStepView> steps) {
        this.scheme = scheme;
        this.steps = steps;
    }

    public WorkflowScheme getScheme() {
        return scheme;
    }

    public List<BulkWorkflowStepView> getSteps() {
        return steps;
    }

    @Override
    public String toString() {
        return "BulkWorkflowSchemeView{" +
                "scheme=" + scheme +
                ", steps=" + steps +
                '}';
    }
}
