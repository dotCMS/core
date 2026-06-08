package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Schema(description = "Request body for PUT /api/v1/workflow/contentlet/actions/bulk/fire. "
        + "Either 'query' OR 'contentletIds' must be supplied (not both). "
        + "'workflowActionId' is required and must belong to the workflow scheme that owns "
        + "the input contentlets — otherwise the contentlets are skipped and the response "
        + "returns a non-empty 'skippedCount' with a 'skipReason'. To bypass scheme checks "
        + "(e.g. for System Workflow actions like Move on content from a non-system scheme), "
        + "use PUT /api/v1/workflow/actions/{actionId}/fire instead.")
public class FireBulkActionsForm {

    @Schema(description = "Lucene query that selects the contentlets to act on. "
            + "Mutually exclusive with 'contentletIds'.",
            example = "+contentType:webPageContent +languageId:1")
    private final String       query;

    @Schema(description = "Explicit list of contentlet identifiers to act on. "
            + "Mutually exclusive with 'query'.")
    private final List<String> contentletIds;

    @Schema(description = "The workflow action identifier (UUID) to fire. "
            + "Discover via GET /api/v1/workflow/schemes/{schemeId}/actions or "
            + "GET /api/v1/workflow/contentlet/{inode}/actions. "
            + "Not the system action enum value (NEW, EDIT, PUBLISH, …).",
            required = true,
            example = "ceca4ee9-1b7b-4f21-a5f4-5d8e9b8b8a1d")
    private final String       workflowActionId;

    @Schema(description = "Optional bag of action-specific parameters: assign-and-comment, "
            + "push-publish settings, and a free-form map for actionlet-specific keys "
            + "(see AdditionalParamsBean).")
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
