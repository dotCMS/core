package com.dotcms.ai.v2.rest;

import java.util.List;
import java.util.Map;

public class RagSearchResponse {

    private final List<RagSearchMatch> matches;
    private final  int total;                       // filtered count (approx if paging)
    private final  Map<String,Object> meta;         // timings, model, etc.

    public RagSearchResponse(final List<RagSearchMatch> matches,
                             final int total,
                             final Map<String, Object> meta) {

        this.matches = matches;
        this.total = total;
        this.meta = meta;
    }

    public List<RagSearchMatch> getMatches() {
        return matches;
    }

    public int getTotal() {
        return total;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public static RagSearchResponse of(final List<RagSearchMatch> ragSearchMatches,
                                       final int total) {
        final RagSearchResponse ragSearchResponse = new RagSearchResponse(ragSearchMatches, total, Map.of());
        return ragSearchResponse;
    }

    public static RagSearchResponse of(final List<RagSearchMatch> ragSearchMatches,
                                       final int total,
                                       final Map<String,Object> meta) {
        final RagSearchResponse ragSearchResponse = new RagSearchResponse(ragSearchMatches, total, meta);
        return ragSearchResponse;
    }
}
