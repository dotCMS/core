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
    long value();

    /**
     * The relation of the total hits to the actual number of hits.
     *
     * @return the relation indicating if the count is exact or a lower bound
     */
    @Value.Default
    default Relation relation() {
        return Relation.EQUAL_TO;
    }

    /**
     * Creates a new TotalHits builder.
     *
     * @return a new builder instance
     */
    static ImmutableTotalHits.Builder builder() {
        return ImmutableTotalHits.builder();
    }

    /**
     * Creates an empty TotalHits with value 0.
     *
     * @return a new TotalHits instance with value 0
     */
    static TotalHits empty() {
        return builder()
                .value(0L)
                .build();
    }

    /**
     * Creates a TotalHits from an Elasticsearch TotalHits.
     *
     * @param esTotalHits the Elasticsearch TotalHits to wrap
     * @return a new TotalHits instance
     */
    static TotalHits from(org.apache.lucene.search.TotalHits esTotalHits) {
        if (esTotalHits == null) {
            return empty();
        }
        return builder()
                .value(esTotalHits.value)
                .relation(Relation.from(esTotalHits.relation))
                .build();
    }
}