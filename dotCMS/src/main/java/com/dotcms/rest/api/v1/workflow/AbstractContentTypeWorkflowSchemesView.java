package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ContentTypeWorkflowSchemesView.class)
@JsonDeserialize(as = ContentTypeWorkflowSchemesView.class)
@Schema(description = "Workflow schemes associated with a specific content type")
public interface AbstractContentTypeWorkflowSchemesView {

    @JsonProperty("contentTypeId")
    @Schema(
            description = "UUID of the content type",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String contentTypeId();

    @JsonProperty("contentTypeVariable")
    @Schema(
            description = "Variable name of the content type",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String contentTypeVariable();

    @JsonProperty("schemes")
    @Schema(
            description = "Workflow schemes associated with the content type",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<WorkflowScheme> schemes();
}