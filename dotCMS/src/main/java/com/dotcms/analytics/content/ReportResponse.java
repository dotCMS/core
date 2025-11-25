package com.dotcms.analytics.content;

import com.dotcms.analytics.model.ResultSetItem;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the response of a report.
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class ReportResponse implements Serializable {

    private final List<ResultSetItem> results;

    public ReportResponse(final List<ResultSetItem> results){
        this.results = results;
    }

    public List<ResultSetItem> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "ReportResponse{" +
                "results=" + results +
                '}';
    }
}
