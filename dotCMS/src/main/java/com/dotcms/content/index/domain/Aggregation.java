package com.dotcms.content.index.domain;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

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
 * <p>The components are named {@code getName} / {@code getType} / {@code getBuckets} / {@code getHits}
 * / {@code getMetadata} so the canonical record accessors are bean-style; this keeps
 * {@code $results.aggregations.<name>.buckets} (property access, resolved via {@code getBuckets()})
 * working from Velocity.</p>
 *
 * <p>Factory methods are the only places where vendor imports are allowed in this file.</p>
 *
 * @param getName     the aggregation name as declared in the query (e.g. {@code content_types})
 * @param getType     the vendor-reported aggregation type (e.g. {@code sterms}, {@code lterms},
 *                    {@code top_hits}); defaults to {@code unknown}
 * @param getBuckets  buckets for multi-bucket ({@code terms}) aggregations; empty for metric aggregations
 * @param getHits     hits for the {@code top_hits} metric aggregation; {@code null} for other types
 * @param getMetadata the optional {@code meta} object attached to the aggregation in the query;
 *                    mirrors {@code org.elasticsearch.search.aggregations.Aggregation#getMetadata()}
 *                    (and OpenSearch's {@code Aggregate#meta()}) so it survives a rollback to the ES
 *                    type, which exposes the same accessor; empty map when no {@code meta} was set
 * @see AggregationBucket
 */
public record Aggregation(
        String getName,
        String getType,
        List<AggregationBucket> getBuckets,
        @Nullable SearchHits getHits,
        Map<String, Object> getMetadata) implements Iterable<AggregationBucket> {

    /**
     * Canonical constructor. {@code getType} defaults to {@code "unknown"}, {@code getBuckets}
     * to an empty list and {@code getMetadata} to an empty map when {@code null} (mirrors the
     * previous Immutables defaults).
     */
    public Aggregation {
        getType = getType == null ? "unknown" : getType;
        getBuckets = getBuckets == null ? Collections.emptyList() : getBuckets;
        getMetadata = getMetadata == null ? Collections.emptyMap() : getMetadata;
    }

    /** Iterate the buckets directly: {@code #foreach($bucket in $agg)}. */
    @Override
    public @NotNull Iterator<AggregationBucket> iterator() {
        return getBuckets().iterator();
    }

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // ES factories
    // -------------------------------------------------------------------------

    /** Maps the full set of Elasticsearch aggregations to a {@code name -> Aggregation} map. */
    public static Map<String, Aggregation> from(
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
        final Builder builder = builder()
                .name(esAgg.getName())
                .type(esAgg.getType())
                .metadata(esAgg.getMetadata());

        if (esAgg instanceof org.elasticsearch.search.aggregations.bucket.terms.Terms) {
            final org.elasticsearch.search.aggregations.bucket.terms.Terms terms =
                    (org.elasticsearch.search.aggregations.bucket.terms.Terms) esAgg;
            builder.buckets(terms.getBuckets().stream()
                    .map(AggregationBucket::from)
                    .collect(Collectors.toList()));
        } else if (esAgg instanceof org.elasticsearch.search.aggregations.bucket.histogram.Histogram) {
            final org.elasticsearch.search.aggregations.bucket.histogram.Histogram histogram =
                    (org.elasticsearch.search.aggregations.bucket.histogram.Histogram) esAgg;
            builder.buckets(histogram.getBuckets().stream()
                    .map(AggregationBucket::fromHistogram)
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
    public static Map<String, Aggregation> fromOS(
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

        final Builder builder = builder().name(name);

        if (agg.isSterms()) {
            final org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate sterms =
                    agg.sterms();
            return builder.type("sterms")
                    .metadata(fromOSMeta(sterms.meta()))
                    .buckets(sterms.buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isLterms()) {
            final org.opensearch.client.opensearch._types.aggregations.LongTermsAggregate lterms =
                    agg.lterms();
            return builder.type("lterms")
                    .metadata(fromOSMeta(lterms.meta()))
                    .buckets(lterms.buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isDterms()) {
            final org.opensearch.client.opensearch._types.aggregations.DoubleTermsAggregate dterms =
                    agg.dterms();
            return builder.type("dterms")
                    .metadata(fromOSMeta(dterms.meta()))
                    .buckets(dterms.buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isDateHistogram()) {
            // The ES path types a date histogram as "date_histogram" (esAgg.getType()); mirror it so
            // the vendor-neutral output matches across providers (issue #36360, I-6). Buckets carry
            // epoch-millis keys, matching AggregationBucket.fromHistogram on the ES side.
            final org.opensearch.client.opensearch._types.aggregations.DateHistogramAggregate dateHistogram =
                    agg.dateHistogram();
            return builder.type("date_histogram")
                    .metadata(fromOSMeta(dateHistogram.meta()))
                    .buckets(dateHistogram.buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isHistogram()) {
            // Numeric histogram — typed "histogram" to match the ES path (issue #36360, I-6).
            final org.opensearch.client.opensearch._types.aggregations.HistogramAggregate histogram =
                    agg.histogram();
            return builder.type("histogram")
                    .metadata(fromOSMeta(histogram.meta()))
                    .buckets(histogram.buckets().array().stream()
                            .map(AggregationBucket::fromOS)
                            .collect(Collectors.toList()))
                    .build();
        } else if (agg.isTopHits()) {
            final org.opensearch.client.opensearch._types.aggregations.TopHitsAggregate topHits =
                    agg.topHits();
            return builder.type("top_hits")
                    .metadata(fromOSMeta(topHits.meta()))
                    .hits(SearchHits.from(topHits.hits()))
                    .build();
        }

        return null;
    }

    /**
     * Converts an OpenSearch aggregation {@code meta} map ({@code Map<String, JsonData>}) into the
     * neutral plain-Java {@code Map<String, Object>} so it matches the shape Elasticsearch already
     * returns from {@code Aggregation#getMetadata()}. Each {@code JsonData} is unwrapped to its
     * closest plain value (Map/List/String/Number/Boolean); if a value cannot be mapped it falls
     * back to its raw JSON string rather than failing the whole aggregation.
     */
    private static Map<String, Object> fromOSMeta(
            final Map<String, org.opensearch.client.json.JsonData> osMeta) {
        if (osMeta == null || osMeta.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Object> meta = new LinkedHashMap<>();
        for (final Map.Entry<String, org.opensearch.client.json.JsonData> entry : osMeta.entrySet()) {
            try {
                meta.put(entry.getKey(), entry.getValue().to(Object.class));
            } catch (final RuntimeException cannotMap) {
                meta.put(entry.getKey(), entry.getValue().toJson().toString());
            }
        }
        return meta;
    }

    /**
     * Fluent builder for {@link Aggregation}. An unset {@code type} defaults to {@code "unknown"},
     * unset {@code buckets} to an empty list, {@code hits} to {@code null} and {@code metadata} to
     * an empty map, preserving the lenient behaviour of the former Immutables builder.
     */
    public static final class Builder {

        private String name;
        private String type;
        private List<AggregationBucket> buckets = Collections.emptyList();
        private SearchHits hits;
        private Map<String, Object> metadata = Collections.emptyMap();

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public Builder buckets(final List<AggregationBucket> buckets) {
            this.buckets = buckets;
            return this;
        }

        public Builder hits(final SearchHits hits) {
            this.hits = hits;
            return this;
        }

        public Builder metadata(final Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Aggregation build() {
            return new Aggregation(name, type, buckets, hits, metadata);
        }
    }
}
