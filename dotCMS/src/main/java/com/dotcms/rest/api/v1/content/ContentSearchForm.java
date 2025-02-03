package com.dotcms.rest.api.v1.content;

import com.dotcms.variant.VariantAPI;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.liferay.util.StringPool.BLANK;

/**
 * This class contains all the information needed to search for content in the dotCMS repository.
 * All data is sent via JSON using the following format:
 * <pre>
 *     {@code
 *     {
 *         "globalSearch": "",
 *         "searchableFieldsByContentType": {
 *             "{{CONTENT_TYPE_ID_OR_VAR_NAME}}": {
 *                 "binary": "{{STRING}}",
 *                 "blockEditor": "{{STRING}}",
 *                 "category": "{{STRING:comma-separated list of values}}",
 *                 "checkbox": "{{STRING:comma-separated list of values}}",
 *                 "custom": "{{STRING}}",
 *                 "date": "{{STRING}}",
 *                 "dateAndTime": "{{STRING}}:date or date and time",
 *                 "json": "{{STRING}}",
 *                 "keyValue": "{{STRING}}",
 *                 "multiSelect": "{{STRING:comma-separated list of values}}",
 *                 "radio": "{{STRING}}",
 *                 "relationships": "{{STRING}}",
 *                 "select": "{{STRING}}",
 *                 "tag": "{{STRING:comma-separated list of values}}",
 *                 "title": "{{STRING}}",
 *                 "textArea": "{{STRING}}",
 *                 "time": "{{STRING}}",
 *                 "wysiwyg": "{{STRING}}"
 *             }
 *         },
 *         "systemSearchableFields": {
 *             "siteId": "{{STRING}}",
 *             "languageId": {{INTEGER}},
 *             "workflowSchemeId": "{{STRING}}",
 *             "workflowStepId": "{{STRING}}",
 *             "variantName": "{{STRING}}",
 *             "systemHostContent": {{BOOLEAN}}
 *         },
 *         "archivedContent": {{BOOLEAN}},
 *         "unpublishedContent": {{BOOLEAN}},
 *         "lockedContent": {{BOOLEAN}},
 *         "orderBy": "{{STRING}}",
 *         "page": {{INTEGER}},
 *         "perPage": {{INTEGER}}
 *     }
 *     }
 * </pre>
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
@JsonDeserialize(builder = ContentSearchForm.Builder.class)
public class ContentSearchForm implements Serializable {

    private final String globalSearch;
    private final Map<String, Map<String, Object>> searchableFieldsByContentType;
    private final Map<String, Object> systemSearchableFields;

    private final String archivedContent;
    private final String unpublishedContent;
    private final String lockedContent;

    private final String orderBy;
    private final int page;
    private final int perPage;

    private ContentSearchForm(final Builder builder) {
        this.globalSearch = builder.globalSearch;
        this.searchableFieldsByContentType = builder.searchableFieldsByContentType;
        this.systemSearchableFields = builder.systemSearchableFields;

        this.lockedContent = builder.lockedContent;
        this.unpublishedContent = builder.unpublishedContent;
        this.archivedContent = builder.archivedContent;

        this.orderBy = builder.orderBy;
        this.page = builder.page;
        this.perPage = builder.perPage;
    }

    public String globalSearch() {
        return this.globalSearch;
    }

    public Map<String, Map<String, Object>> searchableFields() {
        return this.searchableFieldsByContentType;
    }

    public List<String> searchableFields(final String contentTypeId) {
        return null != this.searchableFieldsByContentType
                ? new ArrayList<>(this.searchableFieldsByContentType.getOrDefault(contentTypeId, new HashMap<>()).keySet())
                : List.of();
    }

    public Optional<Object> searchableFieldsByContentTypeAndField(final String contentTypeId,
                                                                  final String fieldVarName) {
        return null != this.searchableFieldsByContentType && null != this.searchableFieldsByContentType.get(contentTypeId)
                ? Optional.of(this.searchableFieldsByContentType.get(contentTypeId).get(fieldVarName))
                : Optional.empty();
    }

    public Map<String, Object> systemSearchableFields() {
        return null != this.systemSearchableFields
                ? this.systemSearchableFields
                : Map.of();
    }

    public String siteId() {
        return (String) this.systemSearchableFields().getOrDefault("siteId", BLANK);
    }

    public int languageId() {
        return (int) this.systemSearchableFields().getOrDefault("languageId", -1);
    }

    public String workflowSchemeId() {
        return (String) this.systemSearchableFields().getOrDefault("workflowSchemeId", BLANK);
    }

    public String workflowStepId() {
        return (String) this.systemSearchableFields().getOrDefault("workflowStepId", BLANK);
    }

    public String variantName() {
        return (String) this.systemSearchableFields().getOrDefault("variantName", VariantAPI.DEFAULT_VARIANT.name());
    }

    public boolean systemHostContent() {
        return (boolean) this.systemSearchableFields().getOrDefault("systemHostContent", true);
    }

    public String orderBy() {
        return this.orderBy;
    }

    public List<String> contentTypeIds() {
        if (null == this.searchableFieldsByContentType) {
            return List.of();
        }
        return new ArrayList<>(this.searchableFieldsByContentType.keySet());
    }

    public String archivedContent() {
        return this.archivedContent;
    }

    public String unpublishedContent() {
        return this.unpublishedContent;
    }

    public String lockedContent() {
        return this.lockedContent;
    }

    public int page() {
        return this.page;
    }

    public int perPage() {
        return this.perPage;
    }

    public int offset() {
        if (this.page != 0) {
            return this.perPage * (this.page - 1);
        }
        return 0;
    }

    public static final class Builder {

        @JsonProperty
        private String globalSearch = BLANK;
        @JsonProperty
        private Map<String, Map<String, Object>> searchableFieldsByContentType = new HashMap<>();
        @JsonProperty
        private Map<String, Object> systemSearchableFields;

        @JsonProperty
        private String archivedContent = BLANK;
        @JsonProperty
        private String unpublishedContent = BLANK;
        @JsonProperty
        private String lockedContent = BLANK;

        @JsonProperty
        private String orderBy = BLANK;
        @JsonProperty
        private int page = 0;
        @JsonProperty
        private int perPage = 0;

        public Builder globalSearch(final String globalSearch) {
            this.globalSearch = globalSearch;
            return this;
        }

        public Builder searchableFieldsByContentType(final Map<String, Map<String, Object>> searchableFields) {
            this.searchableFieldsByContentType = searchableFields;
            return this;
        }

        public Builder systemSearchableFields(final Map<String, Object> systemSearchableFields) {
            this.systemSearchableFields = systemSearchableFields;
            return this;
        }

        public Builder archivedContent(final String archivedContent) {
            this.archivedContent = archivedContent;
            return this;
        }

        public Builder unpublishedContent(final String unpublishedContent) {
            this.unpublishedContent = unpublishedContent;
            return this;
        }

        public Builder lockedContent(final String lockedContent) {
            this.lockedContent = lockedContent;
            return this;
        }

        public Builder orderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder page(final int page) {
            this.page = page;
            return this;
        }

        public Builder perPage(final int perPage) {
            this.perPage = perPage;
            return this;
        }

        public ContentSearchForm build() {
            return new ContentSearchForm(this);
        }

    }

}
