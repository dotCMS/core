package com.dotcms.rest.api.v1.anaytics.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

/**
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
@JsonDeserialize(builder = QueryForm.Builder.class)
public class QueryForm implements Serializable {

    private final String query;

    private QueryForm(final Builder builder) {
        this.query = builder.query;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "QueryForm{" +
                "query='" + query + '\'' +
                '}';
    }

    public static class Builder {

        private String query;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public QueryForm build() {
            return new QueryForm(this);
        }

    }

}
