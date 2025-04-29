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
 *     <code>
 * {
 *         "globalSearch": "{{STRING}}",
 *         "searchableFieldsByContentType": {
 *             "{{CONTENT_TYPE_ID_OR_VAR_NAME}}": {
 *                 "binary": "{{STRING}}",
 *                 "blockEditor": "{{STRING}}",
 *                 "category": "{{STRING: Comma-separated list of Category IDs}}",
 *                 "checkbox": "{{STRING: Comma-separated list of values}}",
 *                 "custom": "{{STRING}}",
 *                 "date": "{{STRING}}: Can use ranges by including the TO between dates. Brackets are optional",
 *                 "dateAndTime": "{{STRING}}: Date or date and time. Can use ranges by including the TO between dates. Brackets are optional",
 *                 "json": "{{STRING}: Matches any String in the JSON object}",
 *                 "keyValue": "{{STRING}: Matches keys and values in the JSON object}}",
 *                 "multiSelect": "{{STRING: Comma-separated list of values, NOT labels}}",
 *                 "radio": "{{STRING}: Matches values, NOT labels}",
 *                 "relationships": "{{STRING: ID of the child Contentlet that must be referenced by the Contentlet(s) you want to retrieve}}",
 *                 "select": "{{STRING}: Matches values, NOT labels}",
 *                 "tag": "{{STRING:comma-separated list of Tag names}}",
 *                 "title": "{{STRING}}",
 *                 "textArea": "{{STRING}}",
 *                 "time": "{{STRING}: Can use ranges by including the TO string between times. Brackets are optional}",
 *                 "wysiwyg": "{{STRING}}"
 *             }
 *         },
 *         "systemSearchableFields": {
 *             "siteId": "{{STRING: The ID of the Site that contains the contents to look for. When NOT set, and the 'systemHostContent' is NOT set either, the search will include contents under System Host}}",
 *             "folderId": "{{STRING}}: The ID of the Folder that contains the contents to look for. When set, the 'siteId' attribute is completely ignored",
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
 * </code>
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

    /**
     * Creates an instance of this class using the provided Builder.
     *
     * @param builder The {@link Builder} instance to use.
     */
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

    /**
     * Returns the global search term. This attribute represents, for instance, the global search
     * box that you can see in the {@code Search} portlet, and in the dynamic search dialog in the
     * {@code Relationships} field.
     *
     * @return The global search term.
     */
    public String globalSearch() {
        return this.globalSearch;
    }

    /**
     * Returns a map containing all the searchable fields for each content type. The key of the map
     * is the content type ID or variable name, and the value is another map containing the fields
     * and their values.
     *
     * @return A map containing all the searchable fields for each content type.
     */
    public Map<String, Map<String, Object>> searchableFields() {
        return this.searchableFieldsByContentType;
    }

    /**
     * Returns a list of searchable fields for a specific content type.
     *
     * @param contentTypeId The ID or variable name of the content type.
     *
     * @return A list of searchable fields for the specified content type.
     */
    public List<String> searchableFields(final String contentTypeId) {
        return null != this.searchableFieldsByContentType
                ? new ArrayList<>(this.searchableFieldsByContentType.getOrDefault(contentTypeId, new HashMap<>()).keySet())
                : List.of();
    }

    /**
     * Returns an Optional with the value of a specific field for a specific content type.
     *
     * @param contentTypeIdOrVar The ID or variable name of the content type.
     * @param fieldVarName       The variable name of the field.
     *
     * @return An {@link Optional} with the value of the field, or an empty Optional if the field is
     * not found.
     */
    public Optional<Object> searchableFieldsByContentTypeAndField(final String contentTypeIdOrVar,
                                                                  final String fieldVarName) {
        return null != this.searchableFieldsByContentType && null != this.searchableFieldsByContentType.get(contentTypeIdOrVar)
                ? Optional.of(this.searchableFieldsByContentType.get(contentTypeIdOrVar).get(fieldVarName))
                : Optional.empty();
    }

    /**
     * Returns a map containing all the system searchable fields. These fields are used to filter
     * content based on system properties like the site ID, language ID, workflow scheme ID, etc.
     * and not actual fields in a Content Type.
     *
     * @return A map containing all the system searchable fields.
     */
    public Map<String, Object> systemSearchableFields() {
        return null != this.systemSearchableFields
                ? this.systemSearchableFields
                : Map.of();
    }

    /**
     * Returns the site ID to filter the content by.
     *
     * @return The site ID to filter the content by.
     */
    public String siteId() {
        return (String) this.systemSearchableFields().getOrDefault("siteId", BLANK);
    }

    /**
     * Returns the ID of the Folder containing the contents that will be filtered.
     *
     * @return The folder ID to filter the content by.
     */
    public String folderId() {
        return (String) this.systemSearchableFields().getOrDefault("folderId", BLANK);
    }

    /**
     * Returns the language ID to filter the content by.
     *
     * @return The language ID to filter the content by.
     */
    public int languageId() {
        return (int) this.systemSearchableFields().getOrDefault("languageId", -1);
    }

    /**
     * Returns the workflow scheme ID to filter the content by.
     *
     * @return The workflow scheme ID to filter the content by.
     */
    public String workflowSchemeId() {
        return (String) this.systemSearchableFields().getOrDefault("workflowSchemeId", BLANK);
    }

    /**
     * Returns the workflow step ID to filter the content by.
     *
     * @return The workflow step ID to filter the content by.
     */
    public String workflowStepId() {
        return (String) this.systemSearchableFields().getOrDefault("workflowStepId", BLANK);
    }

    /**
     * Returns the variant name to filter the content by.
     *
     * @return The variant name to filter the content by.
     */
    public String variantName() {
        return (String) this.systemSearchableFields().getOrDefault("variantName", VariantAPI.DEFAULT_VARIANT.name());
    }

    /**
     * Returns a boolean indicating whether the generated Lucene query must also look for content
     * living under System Host or not.
     *
     * @return If the generated Lucene query must look for content living under System Host, returns
     * {@code true}.
     */
    public boolean systemHostContent() {
        return (boolean) this.systemSearchableFields().getOrDefault("systemHostContent", true);
    }

    /**
     * Returns the criterion being used to filter results by.
     *
     * @return The criterion being used to filter results by.
     */
    public String orderBy() {
        return this.orderBy;
    }

    /**
     * Returns a list of content type IDs that can be used to filter the search results.
     *
     * @return A list of content type IDs that can be used to filter the search results.
     */
    public List<String> contentTypeIds() {
        if (null == this.searchableFieldsByContentType) {
            return List.of();
        }
        return new ArrayList<>(this.searchableFieldsByContentType.keySet());
    }

    /**
     * Returns a String indicating whether the search results must include archived content or not.
     * This is an optional parameter, so it's being handled as a String to NOT add it to the Lucene
     * query when it's not specified in the form.
     *
     * @return If the search results must include archived content, returns {@code "true"}.
     */
    public String archivedContent() {
        return this.archivedContent;
    }

    /**
     * Returns a String indicating whether the search results must include unpublished content or
     * not. This is an optional parameter, so it's being handled as a String to NOT add it to the
     * Lucene query when it's not specified in the form.
     *
     * @return If the search results must include unpublished content, returns {@code "true"}.
     */
    public String unpublishedContent() {
        return this.unpublishedContent;
    }

    /**
     * Returns a boolean indicating whether the search results must include locked content or not.
     *
     * @return If the search results must include locked content, returns {@code true}.
     */
    public String lockedContent() {
        return this.lockedContent;
    }

    /**
     * Returns the page number to be used to paginate the search results.
     *
     * @return The page number to be used to paginate the search results.
     */
    public int page() {
        return this.page;
    }

    /**
     * Returns the number of results to be shown per page.
     *
     * @return The number of results to be shown per page.
     */
    public int perPage() {
        return this.perPage;
    }

    /**
     * Returns the offset to be used to paginate the search results.
     *
     * @return The offset to be used to paginate the search results.
     */
    public int offset() {
        if (this.page != 0) {
            return this.perPage * (this.page - 1);
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ContentSearchForm{" +
                "globalSearch='" + globalSearch + '\'' +
                ", searchableFieldsByContentType=" + searchableFieldsByContentType +
                ", systemSearchableFields=" + systemSearchableFields +
                ", archivedContent='" + archivedContent + '\'' +
                ", unpublishedContent='" + unpublishedContent + '\'' +
                ", lockedContent='" + lockedContent + '\'' +
                ", orderBy='" + orderBy + '\'' +
                ", page=" + page +
                ", perPage=" + perPage +
                '}';
    }

    /**
     * Allows you to create an instance of the {@link ContentSearchForm} class using a Builder.
     */
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
