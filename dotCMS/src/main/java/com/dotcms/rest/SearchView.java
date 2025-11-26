package com.dotcms.rest;

/**
 * This View contains the results of searching for Contentlets in dotCMS. It's used by several
 * content search REST Endpoints in order to provide Users the results, and additional information
 * related to the execution of the query per se.
 *
 * @author Jonathan Sanchez
 * @since Oct 9th, 2020
 */
public class SearchView {

    private final long resultsSize;
    private final long queryTook;
    private final long contentTook;
    private final JsonObjectView jsonObjectView;

    /**
     * Creates an instance of this View class with the results returned from a content query, and
     * additional search metadata.
     *
     * @param resultsSize    The total number of results that are pulled from the query <b>WITHOUT
     *                       taking pagination into account</b>. For instance, if you're pulling 10
     *                       results per result page, but your query matches 35 results total, this
     *                       value will be 35.
     * @param queryTook      Represents the time in milliseconds that dotCMS needed to retrieve the
     *                       total number of results being returned by the query.
     * @param contentTook    Represents the time in milliseconds that dotCMS needed to retrieve the
     *                       actual results and transform them into the appropriate JSON
     *                       representation.
     * @param jsonObjectView Contains the actual list of returned Contentlets in the appropriate
     *                       JSON format, in the form of a {@link JsonObjectView} object.
     */
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
