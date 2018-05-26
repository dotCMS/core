package com.dotcms.workflow.form;

import java.util.List;

public class FireBulkActionsForm {

    private final List<String> contentletIds;
    private final String       workflowActionId;

    public FireBulkActionsForm(List<String> contentletIds, String workflowActionId) {
        this.contentletIds = contentletIds;
        this.workflowActionId = workflowActionId;
    }

    public List<String> getContentletIds() {
        return contentletIds;
    }

    public String getWorkflowActionId() {
        return workflowActionId;
    }

    @Override
    public String toString() {
        return "FireBulkActionsForm{" +
                "contentletIds=" + contentletIds +
                ", workflowActionId='" + workflowActionId + '\'' +
                '}';
    }
}
