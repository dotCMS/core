package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Immutable wrapper for Elasticsearch TotalHits functionality.
 * This interface provides access to total hit count information without direct dependency on Elasticsearch.
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableTotalHits.class)
@JsonDeserialize(as = ImmutableTotalHits.class)
public interface TotalHits {

    /**
     * The total number of hits that match the query.
     *
     * @return the total hit count
     */
    long getValue();

    /**
     * Creates a new TotalHits builder.
     *
     * @return a new builder instance
     */
    static ImmutableTotalHits.Builder builder() {
        return ImmutableTotalHits.builder();
    }

    /**
     * Creates a TotalHits from an Elasticsearch TotalHits.
     *
     * @param esTotalHits the Elasticsearch TotalHits to wrap
     * @return a new TotalHits instance
     */
    static TotalHits from(org.apache.lucene.search.TotalHits esTotalHits) {
        return builder()
                .value(esTotalHits.value)
                .build();
    }
}