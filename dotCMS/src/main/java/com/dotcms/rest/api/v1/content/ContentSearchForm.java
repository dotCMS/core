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
 * Contains all the information needed to search for content in the dotCMS repository.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
@JsonDeserialize(builder = ContentSearchForm.Builder.class)
public class ContentSearchForm implements Serializable {

    private final String globalSearch;
    private final Map<String, Map<String, Object>> searchableFieldsByContentType;
    private final Map<String, Object> systemSearchableFields;

    private final boolean archivedContent;
    private final boolean unpublishedContent;
    private final boolean lockedContent;

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

    public boolean archivedContent() {
        return this.archivedContent;
    }

    public boolean unpublishedContent() {
        return this.unpublishedContent;
    }

    public boolean lockedContent() {
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
        private boolean archivedContent = false;
        @JsonProperty
        private boolean unpublishedContent = false;
        @JsonProperty
        private boolean lockedContent = false;

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

        public Builder archivedContent(final boolean archivedContent) {
            this.archivedContent = archivedContent;
            return this;
        }

        public Builder unpublishedContent(final boolean unpublishedContent) {
            this.unpublishedContent = unpublishedContent;
            return this;
        }

        public Builder lockedContent(final boolean lockedContent) {
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
