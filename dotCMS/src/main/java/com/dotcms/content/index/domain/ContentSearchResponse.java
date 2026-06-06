package com.dotcms.content.index.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

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
 *   <li>{@link #aggregationTree()} — the full {@link Aggregations} tree, preserving nested
 *       sub-aggregations and the {@code top_hits} metric aggregation. This is what
 *       {@code ContentSearchResults#getAggregations()} exposes to Velocity so legacy templates that
 *       walk {@code .buckets} / {@code getKeyAsNumber()} / {@code getAggregations()} keep working.</li>
 * </ul>
 *
 * <p>The flat map is always derived from the tree (see {@link #flatten(Aggregations)}) so the two
 * views can never disagree.</p>
 *
 * <p>Factory methods ({@code from(ES)}, {@code from(OS)}) map vendor types to this
 * neutral DTO; they are the only places where vendor imports are allowed in this file.</p>
 */
@Value.Immutable
public interface ContentSearchResponse {

    /** Neutral search hits (already vendor-independent). */
    SearchHits hits();

    /**
     * Scroll ID returned by the cluster, or {@code null} when not a scroll request.
     * ES: {@code SearchResponse.getScrollId()} / OS: {@code SearchResponse.scrollId()}
     */
    @Nullable
    String scrollId();

    /** Time the cluster took to execute the query, in milliseconds. */
    long tookMillis();

    /**
     * Full neutral aggregation tree, keyed by aggregation name (nested sub-aggregations and
     * {@code top_hits} preserved). Velocity resolves {@code $results.aggregations.<name>} through
     * this map.
     */
    @Value.Default
    default Map<String, Aggregation> aggregationTree() {
        return Collections.emptyMap();
    }

    /**
     * First-level terms aggregations, keyed by aggregation name.
     * Derived from {@link #aggregationTree()}; only aggregations that have buckets are included.
     */
    @Value.Derived
    default Map<String, List<AggregationBucket>> aggregations() {
        return flatten(aggregationTree());
    }

    static ImmutableContentSearchResponse.Builder builder() {
        return ImmutableContentSearchResponse.builder();
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

    static ContentSearchResponse from(
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

    static ContentSearchResponse from(
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
}
