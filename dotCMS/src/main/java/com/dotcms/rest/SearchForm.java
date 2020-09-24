package com.dotcms.rest;

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

    private SearchForm (final Builder builder) {

        this.query  = builder.query;
        this.sort   = builder.sort;
        this.limit  = builder.limit;
        this.offset = builder.offset;
        this.userId = builder.userId;
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

    public static final class Builder {

        private  @JsonProperty String query  = "";
        private  @JsonProperty String sort   = "";
        private  @JsonProperty int    limit  = 0;
        private  @JsonProperty int    offset = 20;
        private  @JsonProperty String userId;

        public Builder query(final String query) {
            this.query = query;
            return this;
        }

        public Builder sort(final String sort) {
            this.sort = sort;
            return this;
        }

        public Builder offset(final int offset) {
            this.offset = offset;
            return this;
        }

        public Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        public Builder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        public SearchForm build () {
            return new SearchForm(this);
        }

    }

}
