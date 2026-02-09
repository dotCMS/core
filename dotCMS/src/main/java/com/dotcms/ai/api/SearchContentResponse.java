package com.dotcms.ai.api;

import com.dotcms.ai.api.embeddings.SearchMatch;

import java.util.List;
import java.util.Map;

public class SearchContentResponse {

    private final List<SearchMatch> matches;
    private final  int total;                       // filtered count (approx if paging)
    private final  Map<String,Object> meta;         // timings, model, etc.

    public SearchContentResponse(final List<SearchMatch> matches,
                             final int total,
                             final Map<String, Object> meta) {

        this.matches = matches;
        this.total = total;
        this.meta = meta;
    }

    public List<SearchMatch> getMatches() {
        return matches;
    }

    public int getTotal() {
        return total;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public static SearchContentResponse of(final List<SearchMatch> ragSearchMatches,
                                       final int total) {
        final SearchContentResponse ragSearchResponse = new SearchContentResponse(ragSearchMatches, total, Map.of());
        return ragSearchResponse;
    }

    public static SearchContentResponse of(final List<SearchMatch> ragSearchMatches,
                                       final int total,
                                       final Map<String,Object> meta) {
        final SearchContentResponse ragSearchResponse = new SearchContentResponse(ragSearchMatches, total, meta);
        return ragSearchResponse;
    }

}
