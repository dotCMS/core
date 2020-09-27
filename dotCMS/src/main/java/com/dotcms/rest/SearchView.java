package com.dotcms.rest;

public class SearchView {

    private final long resultsSize;
    private final long queryTook;
    private final long contentTook;
    private final JsonObjectView jsonObjectView;

    public SearchView(final long resultsSize,
                      final long queryTook,
                      final long contentTook,
                      final JsonObjectView jsonObjectView) {

        this.resultsSize = resultsSize;
        this.queryTook = queryTook;
        this.contentTook = contentTook;
        this.jsonObjectView = jsonObjectView;
    }

    public long getResultsSize() {
        return resultsSize;
    }

    public long getQueryTook() {
        return queryTook;
    }

    public long getContentTook() {
        return contentTook;
    }

    public JsonObjectView getJsonObjectView() {
        return jsonObjectView;
    }
}
