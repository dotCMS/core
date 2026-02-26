package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable domain representation of search results from any search engine.
 *
 * <p>This interface provides a unified abstraction layer for collections of search results,
 * allowing the application to work with search hits without depending on specific
 * search engine libraries (Elasticsearch, OpenSearch, etc.).</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Search engine agnostic - works with Elasticsearch, OpenSearch, or other engines</li>
 *   <li>Iterable interface support for easy iteration over results</li>
 *   <li>Total hits metadata with relation information (exact count vs estimate)</li>
 *   <li>Error state tracking for failed searches</li>
 *   <li>Type-safe immutable objects using Immutables library</li>
 *   <li>JSON serialization support for REST APIs and caching</li>
 *   <li>Factory methods for conversion from underlying search engine types</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // Create from Elasticsearch results
 * SearchHits results = SearchHits.from(elasticsearchHits);
 *
 * // Create from OpenSearch results
 * SearchHits results = SearchHits.from(openSearchHitsMetadata);
 *
 * // Iterate through results
 * for (SearchHit hit : results) {
 *     String docId = hit.id();
 *     Map&lt;String, Object&gt; content = hit.sourceAsMap();
 * }
 *
 * // Access metadata
 * long totalCount = results.totalHits().value();
 * boolean isExactCount = results.totalHits().relation() == Relation.EQUAL_TO;
 *
 * // Handle errors
 * if (results.hasError()) {
 *     // Handle search failure gracefully
 * }
 * </pre>
 *
 * @author Fabrizio Araya
 * @see SearchHit
 * @see TotalHits
 * @see com.dotcms.content.index.ContentFactoryIndexOperations
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSearchHits.class)
@JsonDeserialize(as = ImmutableSearchHits.class)
public interface SearchHits extends Iterable<SearchHit>{

    /**
     * Indicates whether this SearchHits instance represents an error state.
     * When true, the search operation failed and the hits should be considered invalid.
     *
     * @return true if this represents an error state, false for successful searches
     */
    @Default
    default boolean hasError(){ return false;}
    /**
     * Returns the list of search hits.
     *
     * @return the list of search hits
     */
    List<SearchHit> hits();

    /**
     * Returns the total hits information.
     *
     * @return the total hits
     */
    TotalHits totalHits();

    /**
     * Returns an iterator over the search hits.
     * Implements the Iterable interface.
     *
     * @return an iterator for the search hits
     */
    @Override
    default @NotNull Iterator<SearchHit> iterator() {
        return hits().iterator();
    }

    /**
     * Creates a new SearchHits builder.
     *
     * @return a new builder instance
     */
    static ImmutableSearchHits.Builder builder() {
        return ImmutableSearchHits.builder();
    }

    /**
     * Creates an empty SearchHits instance.
     *
     * @return an empty SearchHits
     */
    static SearchHits empty() {
        return builder()
                .totalHits(TotalHits.empty())
                .build();
    }

    /**
     * Creates an error SearchHits instance to represent search errors.
     * This replaces the old ERROR_HIT constant that used Elasticsearch classes directly.
     *
     * @return a SearchHits instance representing an error state
     */
    static SearchHits errorHit() {
        return builder()
                .hasError(true)
                .totalHits(TotalHits.empty())
                .build();
    }

    /**
     * Creates a SearchHits from an Elasticsearch SearchHits.
     *
     * @param esSearchHits the Elasticsearch SearchHits to wrap
     * @return a new SearchHits instance
     */
    static SearchHits from(org.elasticsearch.search.SearchHits esSearchHits) {
        final List<SearchHit> hits = Arrays.stream(esSearchHits.getHits())
                .map(SearchHit::from)
                .collect(Collectors.toList());

        return builder()
                .hits(hits)
                .totalHits(TotalHits.from(esSearchHits.getTotalHits()))
                .build();
    }

    /**
     * Creates a SearchHits from an OpenSearch HitsMetadata.
     *
     * @param osHitsMetadata the OpenSearch HitsMetadata to wrap
     * @return a new SearchHits instance
     */
    static SearchHits from(org.opensearch.client.opensearch.core.search.HitsMetadata<?> osHitsMetadata) {
        if (osHitsMetadata == null) {
            return empty();
        }

        final List<SearchHit> hits = osHitsMetadata.hits().stream()
                .map(SearchHit::from)
                .collect(Collectors.toList());

        return builder()
                .hits(hits)
                .totalHits(TotalHits.from(osHitsMetadata.total()))
                .build();
    }

    /**
     * Creates a list of SearchHits from a list of Elasticsearch SearchHits.
     * Utility method for batch conversion of multiple search result sets.
     *
     * @param hits list of Elasticsearch SearchHits to convert
     * @return list of converted SearchHits instances
     */
    static List<SearchHits> from(List<org.elasticsearch.search.SearchHits> hits) {
        return hits.stream().map(SearchHits::from).collect(Collectors.toList());
    }

    /**
     * Creates a list of SearchHits from a list of OpenSearch HitsMetadata.
     * @param hitsMetadata list of OpenSearch HitsMetadata
     * @return list of SearchHits
     */
    static List<SearchHits> fromOpenSearch(List<org.opensearch.client.opensearch.core.search.HitsMetadata<?>> hitsMetadata) {
        return hitsMetadata.stream().map(SearchHits::from).collect(Collectors.toList());
    }
}