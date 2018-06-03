package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FireBulkActionsForm {

    private final String       query;
    private final List<String> contentletIds;
    private final String       workflowActionId;

    @JsonCreator
    public FireBulkActionsForm(
            @JsonProperty("query") final String  query,
            @JsonProperty("contentletIds") final List<String> contentletIds,
            @JsonProperty("workflowActionId") final String workflowActionId) {
        this.query = query;
        this.contentletIds = contentletIds;
        this.workflowActionId = workflowActionId;
    }

    public String getQuery() {
        return query;
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
