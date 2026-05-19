package com.dotcms.content.index.domain;

import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

/**
 * Vendor-neutral representation of a single bucket in a terms aggregation.
 *
 * <p>Replaces direct use of {@code org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket}
 * and {@code org.opensearch.client.opensearch._types.aggregations.StringTermsBucket} in
 * application code.</p>
 */
@Value.Immutable
public interface AggregationBucket {

    /** Bucket key as a String (numeric keys are converted via {@code toString()}). */
    String key();

    /** Number of documents in this bucket. */
    long docCount();

    static ImmutableAggregationBucket.Builder builder() {
        return ImmutableAggregationBucket.builder();
    }

    // -------------------------------------------------------------------------
    // ES factories
    // -------------------------------------------------------------------------

    /** Creates a bucket from an Elasticsearch terms bucket. */
    static AggregationBucket from(
            final org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket esBucket) {
        return builder()
                .key(esBucket.getKeyAsString())
                .docCount(esBucket.getDocCount())
                .build();
    }

    // -------------------------------------------------------------------------
    // OS factories
    // -------------------------------------------------------------------------

    /** Creates a bucket from an OpenSearch string-terms bucket. */
    static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.StringTermsBucket osBucket) {
        return builder()
                .key(osBucket.key())
                .docCount(osBucket.docCount())
                .build();
    }

    /** Creates a bucket from an OpenSearch long-terms bucket. */
    static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.LongTermsBucket osBucket) {
        return builder()
                .key(String.valueOf(osBucket.key()))
                .docCount(osBucket.docCount())
                .build();
    }

    /** Creates a bucket from an OpenSearch double-terms bucket. */
    static AggregationBucket fromOS(
            final org.opensearch.client.opensearch._types.aggregations.DoubleTermsBucket osBucket) {
        return builder()
                .key(String.valueOf(osBucket.key()))
                .docCount(osBucket.docCount())
                .build();
    }
}
