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

    @Nullable
    private final String site;

    /**
     * Creates a form with no site, so the import runs against the session's site.
     */
    public ContentImportForm(final String contentType, final String language,
            final String workflowActionId, final List<String> fields,
            @Nullable final Boolean stopOnError, @Nullable final Integer commitGranularity) {
        this(contentType, language, workflowActionId, fields, stopOnError, commitGranularity, null);
    }

    /**
     * Creates the import form from its JSON. Validates that a content type and a workflow action
     * are set.
     *
     * @param contentType       the content type's id or variable
     * @param language          the language id or ISO code; omitted or {@code -1} for a
     *                          multilingual file
     * @param workflowActionId  the workflow action to run on each row
     * @param fields            the key fields, as ids or variables
     * @param stopOnError       whether the import stops at the first row error
     * @param commitGranularity how many rows are committed per batch
     * @param site              the site's id or hostname to run against, or {@code null} for the
     *                          session's site (#37896)
     */
    @JsonCreator
    public ContentImportForm(
            @JsonProperty("contentType") final String contentType,
            @JsonProperty("language") final String language,
            @JsonProperty("workflowActionId") final String workflowActionId,
            @JsonProperty("fields") final List<String> fields,
            @JsonProperty("stopOnError") @Nullable final Boolean stopOnError,
            @JsonProperty("commitGranularity") @Nullable final Integer commitGranularity,
            @JsonProperty("site") @Nullable final String site
    ) {
        super();
        this.contentType = contentType;
        this.language = language;
        this.workflowActionId = workflowActionId;
        this.fields = fields;
        this.stopOnError = stopOnError;
        this.commitGranularity = commitGranularity;
        this.site = site;
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

    /**
     * The site the import runs against, as an identifier or hostname (#37896). {@code null} means
     * the session's site.
     *
     * @return the requested site, or {@code null}
     */
    @Nullable
    public String getSite() {
        return site;
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