package com.dotcms.rest;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Search Form to make a ES query
 * @author jsanca
 */
@JsonDeserialize(builder = SearchForm.Builder.class)
public class SearchForm {

    private final String query;
    private final String sort;
    private final int limit;
    private final int offset;
    private final String userId;
    private final String render;
    private final int depth;
    private final long languageId;
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
        private  @JsonProperty int    limit  = 0;
        private  @JsonProperty int    offset = 20;
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
