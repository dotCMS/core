package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowStep;

import java.util.Objects;

public class CountWorkflowStep {

    private final long          count;
    private final WorkflowStep workflowStep;

    public CountWorkflowStep(final long count, final WorkflowStep workflowStep) {
        this.count = count;
        this.workflowStep = workflowStep;
    }

    public long getCount() {
        return count;
    }

    public WorkflowStep getWorkflowStep() {
        return workflowStep;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CountWorkflowStep that = (CountWorkflowStep) o;
        return Objects.equals(workflowStep, that.workflowStep);
    }

    @Override
    public int hashCode() {

        return Objects.hash(workflowStep);
    }

    @Override
    public String toString() {
        return "CountWorkflowStep{" +
                "count=" + count +
                ", workflowStep=" + workflowStep +
                '}';
    }
}
