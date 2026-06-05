package com.dotcms.content.index.domain;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Vendor-neutral representation of a single bucket in a terms aggregation.
 *
 * <p>Replaces direct use of {@code org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket}
 * and {@code org.opensearch.client.opensearch._types.aggregations.StringTermsBucket} in
 * application code.</p>
 *
 * <p>In addition to the fluent neutral accessors ({@link #key()} / {@link #docCount()}), this type
 * exposes the legacy ES-style getters ({@link #getKey()}, {@link #getKeyAsString()},
 * {@link #getKeyAsNumber()}, {@link #getDocCount()}) and the nested {@link #getAggregations()} so
 * that Velocity templates written against the old Elasticsearch {@code Terms.Bucket} API keep
 * working unchanged after the OpenSearch migration.</p>
 */
@Value.Immutable
public interface AggregationBucket {

    /** Bucket key as a String (numeric keys are converted via {@code toString()}). */
    String key();

    /** Number of documents in this bucket. */
    long docCount();

    /**
     * Sub-aggregations nested under this bucket (e.g. a {@code top_hits} or a nested {@code terms}).
     * Empty when the bucket has no sub-aggregations.
     */
    @Value.Default
    default Map<String, Aggregation> subAggregations() {
        return Collections.emptyMap();
    }

    // -------------------------------------------------------------------------
    // Legacy / Velocity-friendly getters (mirror the ES Terms.Bucket API shape)
    // -------------------------------------------------------------------------

    default String getKey() {
        return key();
    }

    default String getKeyAsString() {
        return key();
    }

    /**
     * The key parsed as a {@link Number}, mirroring {@code Terms.Bucket#getKeyAsNumber()}.
     * Returns {@code null} when the key is not numeric (so {@code $!{bucket.getKeyAsNumber()}}
     * renders empty rather than throwing).
     */
    @Nullable
    default Number getKeyAsNumber() {
        try {
            return Long.valueOf(key());
        } catch (final NumberFormatException longMiss) {
            try {
                return Double.valueOf(key());
            } catch (final NumberFormatException doubleMiss) {
                return null;
            }
        }
    }

    default long getDocCount() {
        return docCount();
    }

    /** Nested sub-aggregations, mirroring {@code Terms.Bucket#getAggregations()}. */
    default Map<String, Aggregation> getAggregations() {
        return subAggregations();
    }

    static ImmutableAggregationBucket.Builder builder() {
        return ImmutableAggregationBucket.builder();
    }

    // -------------------------------------------------------------------------
    // ES factories
    // -------------------------------------------------------------------------

    /** Creates a bucket from an Elasticsearch terms bucket, including its sub-aggregations. */
    static AggregationBucket from(
            final org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket esBucket) {
        return builder()
                .key(esBucket.getKeyAsString())
                .docCount(esBucket.getDocCount())
                .subAggregations(Aggregation.from(esBucket.getAggregations()))
                .build();
    }

    // -------------------------------------------------------------------------
    // OS factories
    // -------------------------------------------------------------------------

    /** Creates a bucket from an OpenSearch string-terms bucket, including its sub-aggregations. */
    static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.StringTermsBucket osBucket) {
        return builder()
                .key(osBucket.key())
                .docCount(osBucket.docCount())
                .subAggregations(Aggregation.fromOS(osBucket.aggregations()))
                .build();
    }

    /** Creates a bucket from an OpenSearch long-terms bucket, including its sub-aggregations. */
    static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.LongTermsBucket osBucket) {
        return builder()
                .key(String.valueOf(osBucket.key()))
                .docCount(osBucket.docCount())
                .subAggregations(Aggregation.fromOS(osBucket.aggregations()))
                .build();
    }

    /** Creates a bucket from an OpenSearch double-terms bucket, including its sub-aggregations. */
    static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.DoubleTermsBucket osBucket) {
        return builder()
                .key(String.valueOf(osBucket.key()))
                .docCount(osBucket.docCount())
                .subAggregations(Aggregation.fromOS(osBucket.aggregations()))
                .build();
    }
}
