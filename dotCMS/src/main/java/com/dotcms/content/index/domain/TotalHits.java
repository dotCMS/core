package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Immutable domain representation of search result count metadata from any search engine.
 *
 * <p>This interface provides a unified abstraction for total hit count information,
 * allowing the application to understand search result quantities without depending on
 * specific search engine libraries (Elasticsearch, OpenSearch, etc.).</p>
 *
 * <p><strong>Key Concepts:</strong></p>
 * <ul>
 *   <li><strong>Count Value:</strong> The numerical count of matching documents</li>
 *   <li><strong>Count Relation:</strong> Whether the count is exact or an estimate/lower bound</li>
 *   <li><strong>Performance Optimization:</strong> Search engines may provide approximate counts for very large result sets to improve query performance</li>
 * </ul>
 *
 * <p><strong>Count Relations Explained:</strong></p>
 * <ul>
 *   <li><strong>EQUAL_TO:</strong> The count is exact - there are precisely this many matching documents</li>
 *   <li><strong>GREATER_THAN_OR_EQUAL_TO:</strong> The count is a lower bound - there are at least this many matching documents, possibly more</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // Create from Elasticsearch TotalHits
 * TotalHits totalHits = TotalHits.from(elasticsearchTotalHits);
 *
 * // Create from OpenSearch TotalHits
 * TotalHits totalHits = TotalHits.from(openSearchTotalHits);
 *
 * // Check result count and accuracy
 * long count = totalHits.value();
 * if (totalHits.relation() == Relation.EQUAL_TO) {
 *     System.out.println("Found exactly " + count + " results");
 * } else {
 *     System.out.println("Found at least " + count + " results (possibly more)");
 * }
 *
 * // Handle different scenarios
 * if (totalHits.value() == 0) {
 *     // No results found
 * } else if (totalHits.value() &gt; 10000 && totalHits.relation() == Relation.GREATER_THAN_OR_EQUAL_TO) {
 *     // Large result set, consider refining search criteria
 * }
 * </pre>
 *
 * @author Fabrizio Araya
 * @see SearchHits
 * @see Relation
 * @see com.dotcms.content.index.ContentFactoryIndexOperations
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

    /**
     * Creates a TotalHits from an OpenSearch TotalHits.
     *
     * @param osTotalHits the OpenSearch TotalHits to wrap
     * @return a new TotalHits instance
     */
    static TotalHits from(org.opensearch.client.opensearch.core.search.TotalHits osTotalHits) {
        if (osTotalHits == null) {
            return empty();
        }
        return builder()
                .value(osTotalHits.value())
                .relation(Relation.from(osTotalHits.relation()))
                .build();
    }
}