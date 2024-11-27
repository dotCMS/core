package com.dotcms.rest.api.v1.content._import;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Form object that represents the JSON parameters for content import operations.
 */
public class ContentImportForm extends Validated {

    @NotNull(message = "A Content Type id or variable is required")
    private final String contentType;

    private final String language;

    @NotNull(message = "A Workflow Action id is required")
    private final String workflowActionId;

    private final List<String> fields;

    @JsonCreator
    public ContentImportForm(
            @JsonProperty("contentType") final String contentType,
            @JsonProperty("language") final String language,
            @JsonProperty("workflowActionId") final String workflowActionId,
            @JsonProperty("fields") final List<String> fields) {
        super();
        this.contentType = contentType;
        this.language = language;
        this.workflowActionId = workflowActionId;
        this.fields = fields;
        this.checkValid();
    }

    public String getContentType() {
        return contentType;
    }

    public String getLanguage() {
        return language;
    }

    public String getWorkflowActionId() {
        return workflowActionId;
    }

    public List<String> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return "ContentImportForm{" +
                "contentType='" + contentType + '\'' +
                ", language='" + language + '\'' +
                ", workflowActionId='" + workflowActionId + '\'' +
                ", fields=" + fields +
                '}';
    }
}