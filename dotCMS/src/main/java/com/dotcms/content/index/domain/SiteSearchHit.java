package com.dotcms.content.index.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The {@link SearchHit} shape that carries highlight fragments, produced for Site Search.
 *
 * <p>Site Search renders each result as a snippet with the matched terms emphasised, so its queries
 * ask the engine to highlight the {@code content} field and its results need those fragments. No other
 * search path requests highlighting, which is why this shape exists separately from
 * {@link ContentSearchHit} instead of every hit carrying the field.</p>
 *
 * <p>Instances come from {@link SearchHit.Builder#build()}, which selects this shape whenever the
 * engine response actually carried highlight fragments — no caller flag is involved.</p>
 *
 * @param getId          the unique identifier of this search hit (the document ID)
 * @param getIndex       the index name where this search hit was found, or {@code null} if not available
 * @param getSourceAsMap the source document as a map of field names to values
 * @param getScore       the search relevance score for this hit
 * @param getFields      the document fields retrieved by the search query (additional fields beyond
 *                       the source document that were explicitly requested), empty if none were requested
 * @param getSortValues  the per-hit sort values the engine returns when the query sorts by a field, in
 *                       the order the {@code sort} clause declared; empty for unsorted queries
 * @param getHighlights  the per-field highlight fragments the engine returned, keyed by field name
 *                       (e.g. {@code content} for Site Search), each value holding that field's
 *                       fragments in engine order
 * @author Fabrizio Araya
 * @see SearchHit
 * @see ContentSearchHit
 */
public record SiteSearchHit(
        @JsonProperty("id") String getId,
        @JsonProperty("index") String getIndex,
        @JsonProperty("sourceAsMap") Map<String, Object> getSourceAsMap,
        @JsonProperty("score") float getScore,
        @JsonProperty("fields") Map<String, Object> getFields,
        @JsonProperty("sortValues") List<Object> getSortValues,
        @JsonProperty("highlights") Map<String, List<String>> getHighlights) implements SearchHit {

    /**
     * Canonical constructor. Collection components default to an empty map/list when {@code null} so
     * the accessors never return {@code null} (mirrors the previous Immutables collection defaults).
     */
    public SiteSearchHit {
        getSourceAsMap = getSourceAsMap == null ? Map.of() : getSourceAsMap;
        getFields = getFields == null ? Map.of() : getFields;
        getSortValues = getSortValues == null ? List.of() : getSortValues;
        getHighlights = getHighlights == null ? Map.of() : getHighlights;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Answers from the fragments this hit carries. Saves every caller the {@code getOrDefault}
     * dance — Site Search only ever asks for one field ({@code content}).</p>
     */
    @Override
    public List<String> highlightsFor(final String fieldName) {
        return getHighlights.getOrDefault(fieldName, List.of());
    }
}
