package com.dotcms.content.index.domain;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Vendor-neutral representation of a single named aggregation result.
 *
 * <p>Mirrors the shape that Velocity templates relied on before the Elasticsearch → OpenSearch
 * migration, where {@code $results.aggregations.<name>} returned an
 * {@code org.elasticsearch.search.aggregations.Aggregation}. Templates then walked
 * {@code .buckets} (for {@code terms} aggregations) or {@code .getHits().getHits()} (for the
 * {@code top_hits} metric aggregation).</p>
 *
 * <p>The set of aggregations is exposed as a plain {@code Map<String, Aggregation>} (Velocity
 * resolves {@code $aggregations.content_types} through {@code Map#get}), so this is the only new
 * type required — there is no separate container class. Use {@link #from(org.elasticsearch.search.aggregations.Aggregations)}
 * / {@link #fromOS(Map)} to build that map from a vendor response.</p>
 *
 * <p>Factory methods are the only places where vendor imports are allowed in this file.</p>
 *
 * @see AggregationBucket
 */
@Value.Immutable
public interface Aggregation extends Iterable<AggregationBucket> {

    /** The aggregation name as declared in the query (e.g. {@code content_types}). */
    String getName();

    /** The vendor-reported aggregation type (e.g. {@code sterms}, {@code lterms}, {@code top_hits}). */
    @Value.Default
    default String getType() {
        return "unknown";
    }

    /** Buckets for multi-bucket ({@code terms}) aggregations; empty for metric aggregations. */
    @Value.Default
    default List<AggregationBucket> getBuckets() {
        return Collections.emptyList();
    }

    /** Hits for the {@code top_hits} metric aggregation; {@code null} for other aggregation types. */
    @Nullable
    SearchHits getHits();

    /** Iterate the buckets directly: {@code #foreach($bucket in $agg)}. */
    @Override
    default Iterator<AggregationBucket> iterator() {
        return getBuckets().iterator();
    }

    static ImmutableAggregation.Builder builder() {
        return ImmutableAggregation.builder();
    }

    // -------------------------------------------------------------------------
    // ES factories
    // -------------------------------------------------------------------------

    /** Maps the full set of Elasticsearch aggregations to a {@code name -> Aggregation} map. */
    static Map<String, Aggregation> from(
            final org.elasticsearch.search.aggregations.Aggregations esAggs) {
        if (esAggs == null) {
            return Collections.emptyMap();
        }
        final Map<String, Aggregation> map = new LinkedHashMap<>();
        for (final org.elasticsearch.search.aggregations.Aggregation agg : esAggs.asList()) {
            map.put(agg.getName(), fromSingle(agg));
        }
        return map;
    }

    private static Aggregation fromSingle(final org.elasticsearch.search.aggregations.Aggregation esAgg) {
        final ImmutableAggregation.Builder builder = builder()
                .name(esAgg.getName())
                .type(esAgg.getType());

        if (esAgg instanceof org.elasticsearch.search.aggregations.bucket.terms.Terms) {
            final org.elasticsearch.search.aggregations.bucket.terms.Terms terms =
                    (org.elasticsearch.search.aggregations.bucket.terms.Terms) esAgg;
            builder.buckets(terms.getBuckets().stream()
                    .map(AggregationBucket::from)
                    .collect(Collectors.toList()));
        } else if (esAgg instanceof org.elasticsearch.search.aggregations.metrics.TopHits) {
            final org.elasticsearch.search.aggregations.metrics.TopHits topHits =
                    (org.elasticsearch.search.aggregations.metrics.TopHits) esAgg;
            builder.hits(SearchHits.from(topHits.getHits()));
        }

        return builder.build();
    }

    // -------------------------------------------------------------------------
    // OS factories
    // -------------------------------------------------------------------------

    /** Maps the full set of OpenSearch aggregations to a {@code name -> Aggregation} map. */
    static Map<String, Aggregation> fromOS(
            final Map<String, org.opensearch.client.opensearch._types.aggregations.Aggregate> osAggs) {
        if (osAggs == null || osAggs.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Aggregation> map = new LinkedHashMap<>();
        for (final Map.Entry<String, org.opensearch.client.opensearch._types.aggregations.Aggregate> entry
                : osAggs.entrySet()) {
            final Aggregation aggregation = fromSingleOS(entry.getKey(), entry.getValue());
            if (aggregation != null) {
                map.put(entry.getKey(), aggregation);
            }
        }
        return map;
    }

    @Nullable
    private static Aggregation fromSingleOS(final String name,
            final org.opensearch.client.opensearch._types.aggregations.Aggregate agg) {

        final ImmutableAggregation.Builder builder = builder().name(name);

        if (agg.isSterms()) {
            return builder.type("sterms")
                    .buckets(agg.sterms().buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isLterms()) {
            return builder.type("lterms")
                    .buckets(agg.lterms().buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isDterms()) {
            return builder.type("dterms")
                    .buckets(agg.dterms().buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isTopHits()) {
            return builder.type("top_hits")
                    .hits(SearchHits.from(agg.topHits().hits()))
                    .build();
        }

        return null;
    }
}
