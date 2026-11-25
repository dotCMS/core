package com.dotcms.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

/**
 * Result of a content search: how many contentlets matched, how long the query and the content
 * pull took, the contentlets themselves, and any warnings about how they were rendered.
 */
public class SearchView {

    private final long resultsSize;
    private final long queryTook;
    private final long contentTook;
    private final JsonObjectView jsonObjectView;
    private final List<String> warnings;

    public SearchView(final long resultsSize,
                      final long queryTook,
                      final long contentTook,
                      final JsonObjectView jsonObjectView) {
        this(resultsSize, queryTook, contentTook, jsonObjectView, Collections.emptyList());
    }

    /**
     * Builds a search result.
     *
     * @param resultsSize    how many contentlets matched the query
     * @param queryTook      milliseconds spent counting the matches
     * @param contentTook    milliseconds spent pulling and converting the contentlets
     * @param jsonObjectView the contentlets as JSON
     * @param warnings       notes about how the contentlets were rendered; empty when there are none
     */
    public SearchView(final long resultsSize,
                      final long queryTook,
                      final long contentTook,
                      final JsonObjectView jsonObjectView,
                      final List<String> warnings) {

        this.resultsSize = resultsSize;
        this.queryTook = queryTook;
        this.contentTook = contentTook;
        this.jsonObjectView = jsonObjectView;
        this.warnings = warnings == null ? Collections.emptyList() : List.copyOf(warnings);
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

    /**
     * Notes about how the contentlets were rendered, for example that widget field values were
     * returned as text because widget field scripting is off. Empty when there is nothing to report.
     *
     * @return the warnings, never null
     */
    @Schema(description = "Notes about how the contentlets were rendered, such as widget field values "
            + "returned as text because widget field scripting is off. Empty when there is nothing to report.")
    public List<String> getWarnings() {
        return warnings;
    }

}
