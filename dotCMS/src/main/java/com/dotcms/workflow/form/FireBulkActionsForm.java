package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class FireBulkActionsForm {

    private final String       query;
    private final List<String> contentletIds;
    private final String       workflowActionId;
    private final AdditionalParamsBean additionalParams;

    @JsonCreator
    public FireBulkActionsForm(
            @JsonProperty("query") final String  query,
            @JsonProperty("contentletIds") final List<String> contentletIds,
            @JsonProperty("workflowActionId") final String workflowActionId,
            @JsonProperty("additionalParams") final AdditionalParamsBean additionalParams) {
        this.query = query;
        this.contentletIds = contentletIds;
        this.workflowActionId = workflowActionId;
        this.additionalParams = additionalParams;
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

    public AdditionalParamsBean getPopupParamsBean() {
        return additionalParams;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).toString();
    }
}
