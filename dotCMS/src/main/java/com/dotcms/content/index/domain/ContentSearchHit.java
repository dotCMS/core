package com.dotcms.content.index.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The lean {@link SearchHit} shape: everything a content search result needs and nothing else.
 *
 * <p>This is the hit every content query produces. It deliberately carries no highlight state — not
 * even an empty map reference — because highlighting is a Site Search concern and content queries
 * never request it. {@link SearchHit#highlightsFor(String)} therefore answers with an empty list here,
 * inherited straight from the interface, so a caller holding a {@code SearchHit} needs no type check.
 *
 * <p>Instances come from {@link SearchHit.Builder#build()}, which selects this shape whenever the
 * engine response carried no highlight fragments. It is also the deserialization target for the
 * {@link SearchHit} interface.</p>
 *
 * @param getId          the unique identifier of this search hit (the document ID)
 * @param getIndex       the index name where this search hit was found, or {@code null} if not available
 * @param getSourceAsMap the source document as a map of field names to values
 * @param getScore       the search relevance score for this hit
 * @param getFields      the document fields retrieved by the search query (additional fields beyond
 *                       the source document that were explicitly requested), empty if none were requested
 * @param getSortValues  the per-hit sort values the engine returns when the query sorts by a field
 *                       (e.g. the computed distance for a {@code _geo_distance} sort), in the order the
 *                       {@code sort} clause declared; empty for relevance-only (unsorted) queries
 * @author Fabrizio Araya
 * @see SearchHit
 * @see SiteSearchHit
 */
public record ContentSearchHit(
        @JsonProperty("id") String getId,
        @JsonProperty("index") String getIndex,
        @JsonProperty("sourceAsMap") Map<String, Object> getSourceAsMap,
        @JsonProperty("score") float getScore,
        @JsonProperty("fields") Map<String, Object> getFields,
        @JsonProperty("sortValues") List<Object> getSortValues) implements SearchHit {

    /**
     * Canonical constructor. Collection components default to an empty map/list when {@code null} so
     * the accessors never return {@code null} (mirrors the previous Immutables collection defaults).
     */
    public ContentSearchHit {
        getSourceAsMap = getSourceAsMap == null ? Map.of() : getSourceAsMap;
        getFields = getFields == null ? Map.of() : getFields;
        getSortValues = getSortValues == null ? List.of() : getSortValues;
    }
}
