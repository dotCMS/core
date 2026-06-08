package com.dotcms.content.index.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Vendor-neutral representation of a raw search response.
 *
 * <p>Replaces direct use of {@code org.elasticsearch.action.search.SearchResponse} in
 * application code. Carries the search hits, timing metadata, scroll ID, and the aggregations.</p>
 *
 * <p>Aggregations are exposed two ways:</p>
 * <ul>
 *   <li>{@link #aggregations()} — a flat {@code Map<String, List<AggregationBucket>>} of the
 *       first-level {@code terms} aggregations. This is the convenient shape for Java callers that
 *       only need bucket keys and counts (e.g. {@code ContentTypeAPIImpl}, {@code WorkflowHelper}).</li>
 *   <li>{@link #aggregationTree()} — the full neutral aggregation tree, preserving nested
 *       sub-aggregations and the {@code top_hits} metric aggregation. This is what
 *       {@code ContentSearchResults#getAggregations()} exposes to Velocity so legacy templates that
 *       walk {@code .buckets} / {@code getKeyAsNumber()} / {@code getAggregations()} keep working.</li>
 * </ul>
 *
 * <p>The flat map is always derived from the tree (see {@link #flatten(Map)}) so the two
 * views can never disagree.</p>
 *
 * <p>Factory methods ({@code from(ES)}, {@code from(OS)}) map vendor types to this
 * neutral DTO; they are the only places where vendor imports are allowed in this file.</p>
 *
 * @param hits           neutral search hits (already vendor-independent)
 * @param scrollId       scroll ID returned by the cluster, or {@code null} when not a scroll request
 *                       (ES: {@code SearchResponse.getScrollId()} / OS: {@code SearchResponse.scrollId()})
 * @param tookMillis     time the cluster took to execute the query, in milliseconds
 * @param aggregationTree full neutral aggregation tree, keyed by aggregation name (nested
 *                        sub-aggregations and {@code top_hits} preserved); Velocity resolves
 *                        {@code $results.aggregations.<name>} through this map
 */
public record ContentSearchResponse(
        SearchHits hits,
        @Nullable String scrollId,
        long tookMillis,
        Map<String, Aggregation> aggregationTree) {

    /**
     * Canonical constructor. {@code aggregationTree} defaults to an empty map when {@code null}
     * (mirrors the previous Immutables collection default).
     */
    public ContentSearchResponse {
        aggregationTree = aggregationTree == null ? Collections.emptyMap() : aggregationTree;
    }

    /**
     * First-level terms aggregations, keyed by aggregation name.
     * Derived from {@link #aggregationTree()}; only aggregations that have buckets are included.
     */
    public Map<String, List<AggregationBucket>> aggregations() {
        return flatten(aggregationTree());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Derives the flat first-level terms map from an aggregation tree: every bucket aggregation
     * (e.g. {@code terms}) contributes its bucket list under its name — <b>including when the bucket
     * list is empty</b>, so callers can rely on the key being present for a declared aggregation.
     * Metric aggregations such as {@code top_hits} (which carry hits, not buckets) are skipped,
     * mirroring the legacy behaviour where only {@code terms} aggregations were mapped.
     *
     * <p>The discriminator is {@link Aggregation#getHits()}: bucket aggregations leave it
     * {@code null}; {@code top_hits} sets it (possibly empty). Filtering on {@code getHits() == null}
     * — not on whether buckets are empty — keeps empty-result terms aggregations in the map.</p>
     */
    static Map<String, List<AggregationBucket>> flatten(final Map<String, Aggregation> tree) {
        if (tree == null || tree.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, List<AggregationBucket>> map = new LinkedHashMap<>();
        for (final Aggregation aggregation : tree.values()) {
            if (aggregation.getHits() == null) {
                map.put(aggregation.getName(), aggregation.getBuckets());
            }
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // ES factory
    // -------------------------------------------------------------------------

    public static ContentSearchResponse from(
            final org.elasticsearch.action.search.SearchResponse esResponse) {

        return builder()
                .hits(esResponse.getHits() != null
                        ? SearchHits.from(esResponse.getHits())
                        : SearchHits.empty())
                .scrollId(esResponse.getScrollId())
                .tookMillis(esResponse.getTook() != null ? esResponse.getTook().getMillis() : 0L)
                .aggregationTree(Aggregation.from(esResponse.getAggregations()))
                .build();
    }

    // -------------------------------------------------------------------------
    // OS factory
    // -------------------------------------------------------------------------

    public static ContentSearchResponse from(
            final org.opensearch.client.opensearch.core.SearchResponse<Object> osResponse) {

        return builder()
                .hits(osResponse.hits() != null
                        ? SearchHits.from(osResponse.hits())
                        : SearchHits.empty())
                .scrollId(osResponse.scrollId())
                .tookMillis(osResponse.took())
                .aggregationTree(Aggregation.fromOS(osResponse.aggregations()))
                .build();
    }

    /**
     * Fluent builder for {@link ContentSearchResponse}. An unset {@code aggregationTree} defaults to
     * an empty map and an unset {@code scrollId} to {@code null}, preserving the lenient behaviour of
     * the former Immutables builder.
     */
    public static final class Builder {

        private SearchHits hits;
        private String scrollId;
        private long tookMillis;
        private Map<String, Aggregation> aggregationTree = Collections.emptyMap();

        public Builder hits(final SearchHits hits) {
            this.hits = hits;
            return this;
        }

        public Builder scrollId(final String scrollId) {
            this.scrollId = scrollId;
            return this;
        }

        public Builder tookMillis(final long tookMillis) {
            this.tookMillis = tookMillis;
            return this;
        }

        public Builder aggregationTree(final Map<String, Aggregation> aggregationTree) {
            this.aggregationTree = aggregationTree;
            return this;
        }

        public ContentSearchResponse build() {
            return new ContentSearchResponse(hits, scrollId, tookMillis, aggregationTree);
        }
    }
}
