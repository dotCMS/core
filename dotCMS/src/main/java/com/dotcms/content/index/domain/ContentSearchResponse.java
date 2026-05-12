package com.dotcms.content.index.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Vendor-neutral representation of a raw search response.
 *
 * <p>Replaces direct use of {@code org.elasticsearch.action.search.SearchResponse} in
 * application code. Carries the search hits, timing metadata, scroll ID, and
 * the first-level terms aggregations (the only aggregation type used in dotCMS).</p>
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
     * First-level terms aggregations, keyed by aggregation name.
     * Only {@code terms} aggregations are mapped; other types are silently skipped.
     */
    @Value.Default
    default Map<String, List<AggregationBucket>> aggregations() {
        return Collections.emptyMap();
    }

    static ImmutableContentSearchResponse.Builder builder() {
        return ImmutableContentSearchResponse.builder();
    }

    // -------------------------------------------------------------------------
    // ES factory
    // -------------------------------------------------------------------------

    static ContentSearchResponse from(
            final org.elasticsearch.action.search.SearchResponse esResponse) {

        final Map<String, List<AggregationBucket>> aggs = new LinkedHashMap<>();
        if (esResponse.getAggregations() != null) {
            for (final org.elasticsearch.search.aggregations.Aggregation agg
                    : esResponse.getAggregations().asList()) {
                if (agg instanceof org.elasticsearch.search.aggregations.bucket.terms.Terms) {
                    final org.elasticsearch.search.aggregations.bucket.terms.Terms termAgg =
                            (org.elasticsearch.search.aggregations.bucket.terms.Terms) agg;
                    aggs.put(agg.getName(), termAgg.getBuckets().stream()
                            .map(AggregationBucket::from)
                            .collect(Collectors.toList()));
                }
            }
        }

        return builder()
                .hits(esResponse.getHits() != null
                        ? SearchHits.from(esResponse.getHits())
                        : SearchHits.empty())
                .scrollId(esResponse.getScrollId())
                .tookMillis(esResponse.getTook() != null ? esResponse.getTook().getMillis() : 0L)
                .aggregations(aggs)
                .build();
    }

    // -------------------------------------------------------------------------
    // OS factory
    // -------------------------------------------------------------------------

    static ContentSearchResponse from(
            final org.opensearch.client.opensearch.core.SearchResponse<Object> osResponse) {

        final Map<String, List<AggregationBucket>> aggs = new LinkedHashMap<>();
        if (osResponse.aggregations() != null) {
            for (final Map.Entry<String, org.opensearch.client.opensearch._types.aggregations.Aggregate>
                    entry : osResponse.aggregations().entrySet()) {
                final org.opensearch.client.opensearch._types.aggregations.Aggregate agg =
                        entry.getValue();
                if (agg.isSterms()) {
                    aggs.put(entry.getKey(), agg.sterms().buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()));
                } else if (agg.isLterms()) {
                    aggs.put(entry.getKey(), agg.lterms().buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()));
                } else if (agg.isDterms()) {
                    aggs.put(entry.getKey(), agg.dterms().buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()));
                }
            }
        }

        return builder()
                .hits(osResponse.hits() != null
                        ? SearchHits.from(osResponse.hits())
                        : SearchHits.empty())
                .scrollId(osResponse.scrollId())
                .tookMillis(osResponse.took())
                .aggregations(aggs)
                .build();
    }
}
