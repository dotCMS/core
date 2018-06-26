package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.contentlet.util.ActionletUtil;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import java.util.Objects;

public class CountWorkflowAction {

    private final long count;
    private final WorkflowAction workflowAction;
    private final boolean pushPublish;

    public CountWorkflowAction(final long count, final WorkflowAction workflowAction) {
        this.count = count;
        this.workflowAction = workflowAction;
        this.pushPublish = ActionletUtil.hasPushPublishActionlet(workflowAction);
    }

    public long getCount() {
        return count;
    }

    public WorkflowAction getWorkflowAction() {
        return workflowAction;
    }

    public boolean isPushPublish() {
        return pushPublish;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CountWorkflowAction that = (CountWorkflowAction) o;
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
