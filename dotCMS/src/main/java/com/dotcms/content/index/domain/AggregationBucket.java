package com.dotcms.content.index.domain;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;

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
 *
 * @param key            bucket key as a String (numeric keys are converted via {@code toString()})
 * @param docCount       number of documents in this bucket
 * @param subAggregations sub-aggregations nested under this bucket (e.g. a {@code top_hits} or a
 *                        nested {@code terms}); empty when the bucket has no sub-aggregations
 */
public record AggregationBucket(
        String key,
        long docCount,
        Map<String, Aggregation> subAggregations) {

    /**
     * Canonical constructor. {@code subAggregations} defaults to an empty map when {@code null}
     * (mirrors the previous Immutables collection default).
     */
    public AggregationBucket {
        subAggregations = subAggregations == null ? Collections.emptyMap() : subAggregations;
    }

    // -------------------------------------------------------------------------
    // Legacy / Velocity-friendly getters (mirror the ES Terms.Bucket API shape)
    // -------------------------------------------------------------------------

    public String getKey() {
        return key();
    }

    public String getKeyAsString() {
        return key();
    }

    /**
     * The key parsed as a {@link Number}, mirroring {@code Terms.Bucket#getKeyAsNumber()}.
     * Returns {@code null} when the key is not numeric (so {@code $!{bucket.getKeyAsNumber()}}
     * renders empty rather than throwing).
     */
    @Nullable
    public Number getKeyAsNumber() {
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

    public long getDocCount() {
        return docCount();
    }

    /** Nested sub-aggregations, mirroring {@code Terms.Bucket#getAggregations()}. */
    public Map<String, Aggregation> getAggregations() {
        return subAggregations();
    }

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // ES factories
    // -------------------------------------------------------------------------

    /** Creates a bucket from an Elasticsearch terms bucket, including its sub-aggregations. */
    public static AggregationBucket from(
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
    public static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.StringTermsBucket osBucket) {
        return builder()
                .key(osBucket.key())
                .docCount(osBucket.docCount())
                .subAggregations(Aggregation.fromOS(osBucket.aggregations()))
                .build();
    }

    /** Creates a bucket from an OpenSearch long-terms bucket, including its sub-aggregations. */
    public static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.LongTermsBucket osBucket) {
        return builder()
                .key(String.valueOf(osBucket.key()))
                .docCount(osBucket.docCount())
                .subAggregations(Aggregation.fromOS(osBucket.aggregations()))
                .build();
    }

    /** Creates a bucket from an OpenSearch double-terms bucket, including its sub-aggregations. */
    public static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.DoubleTermsBucket osBucket) {
        return builder()
                .key(String.valueOf(osBucket.key()))
                .docCount(osBucket.docCount())
                .subAggregations(Aggregation.fromOS(osBucket.aggregations()))
                .build();
    }

    /**
     * Fluent builder for {@link AggregationBucket}. An unset {@code subAggregations} defaults to an
     * empty map, preserving the lenient behaviour of the former Immutables builder.
     */
    public static final class Builder {

        private String key;
        private long docCount;
        private Map<String, Aggregation> subAggregations = Collections.emptyMap();

        public Builder key(final String key) {
            this.key = key;
            return this;
        }

        public Builder docCount(final long docCount) {
            this.docCount = docCount;
            return this;
        }

        public Builder subAggregations(final Map<String, Aggregation> subAggregations) {
            this.subAggregations = subAggregations;
            return this;
        }

        public AggregationBucket build() {
            return new AggregationBucket(key, docCount, subAggregations);
        }
    }
}
