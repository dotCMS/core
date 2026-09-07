package com.dotcms.rest;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Search Form to make a ES query
 * @author jsanca
 */
@Schema(description = "Content search request. `query` is a Lucene expression; the remaining fields "
        + "control paging, sorting, language, and how results are rendered.")
@JsonDeserialize(builder = SearchForm.Builder.class)
public class SearchForm {

    @Schema(
            description = "Lucene query. IMPORTANT: custom (user-defined) fields MUST be qualified with the "
                    + "content type's variable name — `ContentTypeVar.fieldVar`. A BARE field name matches "
                    + "nothing and returns zero results with NO error (a common silent failure). "
                    + "For example, to find featured Books use `+AwazonBook.featured:true`, not `+featured:true`; "
                    + "to match a slug use `+AwazonBook.slug:my-slug`. "
                    + "Restrict the type with `+contentType:AwazonBook` (contentType is a system field, unqualified). "
                    + "\n\nResults are also HOST-scoped: unless you add a host clause, the search resolves against "
                    + "the current request's site and will NOT return content that lives on a different host "
                    + "(e.g. content saved to `SYSTEM_HOST`/default while you query as another site). "
                    + "Constrain the host explicitly with `+conHost:<siteIdentifier>` (or `+conHost:SYSTEM_HOST`), "
                    + "and add `+live:true` / `+working:true` and `+deleted:false` as needed. "
                    + "See the Lucene content-search syntax docs.",
            example = "+contentType:AwazonBook +AwazonBook.featured:true +live:true +deleted:false")
    private final String query;

    @Schema(description = "Sort clause, e.g. `AwazonBook.title asc` or `modDate desc`. Custom fields are "
            + "qualified the same way as in `query`.", example = "modDate desc")
    private final String sort;

    @Schema(description = "Maximum number of contentlets to return (page size).", example = "20", defaultValue = "20")
    private final int limit;

    @Schema(description = "Zero-based result offset for paging.", example = "0", defaultValue = "0")
    private final int offset;

    @Schema(description = "Optional user id to run the search as (permissions are applied for this user). "
            + "Defaults to the authenticated caller when omitted.")
    private final String userId;

    @Schema(description = "When set to `true`, each matching contentlet's `htmlpageasset`/widget content is "
            + "rendered and included in the response. Omit for raw field data only.")
    private final String render;

    @Schema(description = "Relationship-loading depth (0-3): how many levels of related content to inline. "
            + "`-1` (default) loads none.", example = "1", defaultValue = "-1")
    private final int depth;

    @Schema(description = "Language id to search in. Defaults to the system default language when omitted.",
            example = "1")
    private final long languageId;

    @Schema(description = "When `true`, include full category metadata for category fields in the results.")
    private final boolean allCategoriesInfo;

    private SearchForm (final Builder builder) {

        this.query  = builder.query;
        this.sort   = builder.sort;
        this.limit  = builder.limit;
        this.offset = builder.offset;
        this.userId = builder.userId;
        this.render = builder.render;
        this.depth  = builder.depth;
        this.languageId = builder.languageId;
        this.allCategoriesInfo = builder.allCategoriesInfo;
    }

    public String getQuery() {
        return query;
    }

    public String getSort() {
        return sort;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getUserId() {
        return userId;
    }

    public String getRender() {
        return this.render;
    }

    public int getDepth() {
        return depth;
    }

    public long getLanguageId() {
        return languageId;
    }

    public boolean isAllCategoriesInfo() {
        return allCategoriesInfo;
    }

    public static final class Builder {

        private  @JsonProperty String query  = "";
        private  @JsonProperty String sort   = "";
        private  @JsonProperty int    limit  = 20;
        private  @JsonProperty int    offset = 0;
        private  @JsonProperty String userId;
        private  @JsonProperty String render;
        private  @JsonProperty int depth       = -1;
        private  @JsonProperty long languageId = -1;
        private  @JsonProperty boolean allCategoriesInfo;

        public Builder query(final String query) {
            this.query = query;
            return this;
        }

        public Builder sort(final String sort) {
            this.sort = sort;
            return this;
        }

        public Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(final int offset) {
            this.offset = offset;
            return this;
        }

        public Builder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        public Builder render(final String render) {
            this.render = render;
            return this;
        }

        public Builder depth(final int depth) {
            this.depth = depth;
            return this;
        }

        public Builder languageId(final int languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder allCategoriesInfo(final boolean allCategoriesInfo) {
            this.allCategoriesInfo = allCategoriesInfo;
            return this;
        }

        public SearchForm build () {

            if (-1 == this.languageId) {
                this.languageId =
                        APILocator.getLanguageAPI().getDefaultLanguage().getId();
            }

            return new SearchForm(this);
        }

    }

}
