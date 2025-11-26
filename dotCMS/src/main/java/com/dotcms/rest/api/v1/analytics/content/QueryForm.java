package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;

import java.io.Serializable;

/**
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class QueryForm implements Serializable {

    private AnalyticsQuery query;

    public QueryForm() {
        // Default constructor for Jackson deserialization
    }

    public AnalyticsQuery getQuery() {
        return query;
    }

    public void setQuery(final AnalyticsQuery query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return "QueryForm{" +
                "query='" + query + '\'' +
                '}';
    }

}