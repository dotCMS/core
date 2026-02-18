package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.elasticsearch.common.document.DocumentField;
import org.immutables.value.Value;

import java.util.Map;

/**
 * Immutable wrapper for Elasticsearch SearchHit and OpenSearch Hit functionality.
 * This interface provides access to search result hit data without direct dependency on Elasticsearch or OpenSearch.
 *
 * @author Fabrizio Araya
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
     * Returns the index name of this search hit.
     * @return
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
     * Returns the fields of this hit.
     * @return the fields
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