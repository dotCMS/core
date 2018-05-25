package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.rest.api.Validated;

import java.util.List;

/**
 * Encapsulates the input for the BulkAction Form.
 * @author jsanca
 */
public class BulkActionForm extends Validated {

    private final List<String> contentletIds;
    private final String       workflowSchemeId;
    private final String       workflowStepId;
    private final String       query;
    @JsonCreator
    public BulkActionForm(@JsonProperty("contentletIds") final List<String> contentletIds,
                          @JsonProperty("workflowSchemeId") final String workflowSchemeId,
                          @JsonProperty("workflowStepId") final String workflowStepId,
                          @JsonProperty("query") final String query) {

        this.contentletIds      = contentletIds;
        this.workflowSchemeId   = workflowSchemeId;
        this.workflowStepId     = workflowStepId;
        this.query              = query;
    }

    public List<String> getContentletIds() {
        return contentletIds;
    }

    public String getWorkflowSchemeId() {
        return workflowSchemeId;
    }

    public String getWorkflowStepId() {
        return workflowStepId;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "BulkActionForm{" +
                "contentletIds=" + contentletIds +
                ", workflowSchemeId='" + workflowSchemeId + '\'' +
                ", workflowStepId='" + workflowStepId + '\'' +
                ", query='" + query + '\'' +
                '}';
    }
}
