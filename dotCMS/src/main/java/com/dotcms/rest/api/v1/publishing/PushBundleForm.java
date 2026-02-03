package com.dotcms.rest.api.v1.publishing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Form for pushing a bundle to environments for publishing.
 * Simple POJO matching existing form patterns in dotCMS.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
@Schema(description = "Form for pushing a bundle to environments")
public class PushBundleForm {

    @Schema(
            description = "Operation type: publish, expire, or publishexpire",
            example = "publish",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String operation;

    @Schema(
            description = "Scheduled publish date in ISO 8601 format with timezone offset",
            example = "2025-03-15T14:30:00-05:00"
    )
    private String publishDate;

    @Schema(
            description = "Scheduled expire date in ISO 8601 format with timezone offset",
            example = "2025-04-15T14:30:00-05:00"
    )
    private String expireDate;

    @Schema(
            description = "List of environment IDs to push the bundle to",
            example = "[\"env-123\", \"env-456\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<String> environments;

    @Schema(
            description = "Push publishing filter key",
            example = "ForcePush.yml",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String filterKey;

    /**
     * Default constructor for Jackson deserialization.
     */
    public PushBundleForm() {
    }

    /**
     * Constructor with all fields for testing.
     *
     * @param operation    Operation type (publish, expire, publishexpire)
     * @param publishDate  Publish date in ISO 8601 format
     * @param expireDate   Expire date in ISO 8601 format
     * @param environments List of environment IDs
     * @param filterKey    Filter key for push publishing
     */
    @JsonCreator
    public PushBundleForm(
            @JsonProperty("operation") final String operation,
            @JsonProperty("publishDate") final String publishDate,
            @JsonProperty("expireDate") final String expireDate,
            @JsonProperty("environments") final List<String> environments,
            @JsonProperty("filterKey") final String filterKey) {
        this.operation = operation;
        this.publishDate = publishDate;
        this.expireDate = expireDate;
        this.environments = environments;
        this.filterKey = filterKey;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(final String operation) {
        this.operation = operation;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(final String publishDate) {
        this.publishDate = publishDate;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(final String expireDate) {
        this.expireDate = expireDate;
    }

    public List<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(final List<String> environments) {
        this.environments = environments;
    }

    public String getFilterKey() {
        return filterKey;
    }

    public void setFilterKey(final String filterKey) {
        this.filterKey = filterKey;
    }

}
