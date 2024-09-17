package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;

import java.io.Serializable;

/**
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class QueryForm implements Serializable {

    private final AnalyticsQuery query;

    public QueryForm(final AnalyticsQuery query) {
        this.query = query;
    }

    public AnalyticsQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "QueryForm{" +
                "query='" + query + '\'' +
                '}';
    }

}