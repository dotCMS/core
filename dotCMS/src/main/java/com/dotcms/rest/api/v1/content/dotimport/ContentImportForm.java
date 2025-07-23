package com.dotcms.rest.api.v1.content.dotimport;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import javax.annotation.Nullable;

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

    @Nullable
    private final Boolean stopOnError;

    @Nullable
    private final Integer commitGranularity;

    @JsonCreator
    public ContentImportForm(
            @JsonProperty("contentType") final String contentType,
            @JsonProperty("language") final String language,
            @JsonProperty("workflowActionId") final String workflowActionId,
            @JsonProperty("fields") final List<String> fields,
            @JsonProperty("stopOnError") @Nullable final Boolean stopOnError,
            @JsonProperty("commitGranularity") @Nullable final Integer commitGranularity
    ) {
        super();
        this.contentType = contentType;
        this.language = language;
        this.workflowActionId = workflowActionId;
        this.fields = fields;
        this.stopOnError = stopOnError;
        this.commitGranularity = commitGranularity;
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

    @Nullable
    public Boolean getStopOnError() {
        return stopOnError;
    }

    @Nullable
    public Integer getCommitGranularity() {
        return commitGranularity;
    }

    @Override
    public String toString() {
        return "ContentImportForm{" +
                "contentType='" + contentType + '\'' +
                ", language='" + language + '\'' +
                ", workflowActionId='" + workflowActionId + '\'' +
                ", fields=" + fields +
                ", stopOnError=" + stopOnError +
                ", commitGranularity=" + commitGranularity +
                '}';
    }
}