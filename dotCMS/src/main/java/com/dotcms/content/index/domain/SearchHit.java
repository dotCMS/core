package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.elasticsearch.common.document.DocumentField;
import org.immutables.value.Value;

import java.util.Map;

/**
 * Immutable domain representation of a single search result hit from any search engine.
 *
 * <p>This interface provides a unified abstraction layer for individual search results,
 * allowing the application to work with search hits without depending on specific
 * search engine libraries (Elasticsearch, OpenSearch, etc.).</p>
 *
 * <p><strong>Key Benefits:</strong></p>
 * <ul>
 *   <li>Search engine agnostic - switch between Elasticsearch and OpenSearch transparently</li>
 *   <li>Type-safe immutable objects using Immutables library</li>
 *   <li>JSON serialization support for REST APIs and caching</li>
 *   <li>Factory methods for easy conversion from underlying search engine types</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // Create from Elasticsearch hit
 * SearchHit hit = SearchHit.from(elasticsearchHit);
 *
 * // Create from OpenSearch hit
 * SearchHit hit = SearchHit.from(openSearchHit);
 *
 * // Access unified data
 * String docId = hit.id();
 * Map&lt;String, Object&gt; content = hit.sourceAsMap();
 * float relevanceScore = hit.score();
 * </pre>
 *
 * @author Fabrizio Araya
 * @see SearchHits
 * @see com.dotcms.content.index.ContentFactoryIndexOperations
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSearchHit.class)
@JsonDeserialize(as = ImmutableSearchHit.class)
public interface SearchHit {

    /**
     * Returns the unique identifier of this search hit.
     *
     * @return the document ID
     */
    String id();

    /**
     * Returns the index name where this search hit was found.
     *
     * @return the index name, or null if not available
     */
    String index();

    /**
     * Returns the source document as a map of field names to values.
     *
     * @return the source document map
     */
    Map<String, Object> sourceAsMap();

    /**
     * Returns the search relevance score for this hit.
     *
     * @return the score
     */
    float score();

    /**
     * Returns the document fields retrieved by the search query.
     * These are additional fields beyond the source document that were explicitly
     * requested in the search query using field selectors.
     *
     * @return a map of field names to field values, empty if no fields were requested
     */
    Map<String, Object> fields();

    /**
     * Creates a new SearchHit builder.
     *
     * @return a new builder instance
     */
    static ImmutableSearchHit.Builder builder() {
        return ImmutableSearchHit.builder();
    }

    /**
     * Creates a SearchHit from an Elasticsearch SearchHit.
     *
     * @param esSearchHit the Elasticsearch SearchHit to wrap
     * @return a new SearchHit instance
     */
    static SearchHit from(org.elasticsearch.search.SearchHit esSearchHit) {
        return builder()
                .id(esSearchHit.getId())
                .sourceAsMap(esSearchHit.getSourceAsMap())
                .fields(esSearchHit.getFields())
                .score(esSearchHit.getScore())
                .index(esSearchHit.getIndex())
                .build();
    }

    /**
     * Creates a SearchHit from an OpenSearch Hit.
     *
     * @param osHit the OpenSearch Hit to wrap
     * @return a new SearchHit instance
     */
    @SuppressWarnings("unchecked")
    static SearchHit from(org.opensearch.client.opensearch.core.search.Hit<?> osHit) {
        // Extract source as Map - OpenSearch Hit.source() returns the typed source object
        Map<String, Object> sourceMap;
        Object source = osHit.source();
        if (source instanceof Map) {
            sourceMap = (Map<String, Object>) source;
        } else {
            // If "source" is a typed object, we might need custom mapping logic here
            // For now, we'll create an empty map as fallback
            sourceMap = Map.of();
        }

        return builder()
                .id(osHit.id())
                .sourceAsMap(sourceMap)
                .score(osHit.score() != null ? osHit.score().floatValue() : 0.0f)
                .build();
    }
}