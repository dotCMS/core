package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;

import java.util.Objects;

public class CountWorkflowAction {

    private final long            count;
    private final WorkflowAction workflowAction;

    public CountWorkflowAction(final long count, final WorkflowAction workflowAction) {
        this.count = count;
        this.workflowAction = workflowAction;
    }

    public long getCount() {
        return count;
    }

    public WorkflowAction getWorkflowAction() {
        return workflowAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountWorkflowAction that = (CountWorkflowAction) o;
        return Objects.equals(workflowAction, that.workflowAction);
    }

    @Override
    public int hashCode() {

        return Objects.hash(workflowAction);
    }

    @Override
    public String toString() {
        return "CountWorkflowAction{" +
                "count=" + count +
                ", workflowAction=" + workflowAction +
                '}';
    }
}
