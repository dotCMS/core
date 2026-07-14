package com.dotcms.content.index.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Immutable domain representation of a single search result hit from any search engine.
 *
 * <p>This record provides a unified abstraction layer for individual search results,
 * allowing the application to work with search hits without depending on specific
 * search engine libraries (Elasticsearch, OpenSearch, etc.).</p>
 *
 * <p>Accessors are bean-style ({@code getId()}, {@code getSourceAsMap()}, …) so the type works
 * directly from Velocity templates (e.g. {@code $hit.id}) without any extra alias methods.
 * The record components are named accordingly and the {@link JsonProperty} annotations keep the
 * JSON contract clean ({@code id}, {@code index}, …) for the caches/REST paths backed by this type.</p>
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
 * String docId = hit.getId();
 * Map&lt;String, Object&gt; content = hit.getSourceAsMap();
 * float relevanceScore = hit.getScore();
 * </pre>
 *
 * @param getId          the unique identifier of this search hit (the document ID)
 * @param getIndex       the index name where this search hit was found, or {@code null} if not available
 * @param getSourceAsMap the source document as a map of field names to values
 * @param getScore       the search relevance score for this hit
 * @param getFields      the document fields retrieved by the search query (additional fields beyond
 *                       the source document that were explicitly requested), empty if none were requested
 * @author Fabrizio Araya
 * @see SearchHits
 * @see com.dotcms.content.index.ContentFactoryIndexOperations
 */
public record SearchHit(
        @JsonProperty("id") String getId,
        @JsonProperty("index") String getIndex,
        @JsonProperty("sourceAsMap") Map<String, Object> getSourceAsMap,
        @JsonProperty("score") float getScore,
        @JsonProperty("fields") Map<String, Object> getFields) {

    /**
     * Canonical constructor. Collection components default to an empty map when {@code null} so the
     * accessors never return {@code null} (mirrors the previous Immutables collection defaults).
     */
    public SearchHit {
        getSourceAsMap = getSourceAsMap == null ? Map.of() : getSourceAsMap;
        getFields = getFields == null ? Map.of() : getFields;
    }

    /**
     * Creates a new SearchHit builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a SearchHit from an Elasticsearch SearchHit.
     *
     * @param esSearchHit the Elasticsearch SearchHit to wrap
     * @return a new SearchHit instance
     */
    public static SearchHit from(org.elasticsearch.search.SearchHit esSearchHit) {
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
    public static SearchHit from(org.opensearch.client.opensearch.core.search.Hit<?> osHit) {
        // Extract source as Map - OpenSearch Hit.source() returns the typed source object
        Map<String, Object> sourceMap;
        Object source = osHit.source();
        if (source instanceof Map) {
            sourceMap = (Map<String, Object>) source;
        } else if (source instanceof org.opensearch.client.json.JsonData) {
            // top_hits aggregation hits carry their _source as JsonData (HitsMetadata<JsonData>),
            // not a Map — unwrap it so the document survives the conversion instead of being dropped.
            Map<String, Object> unwrapped;
            try {
                unwrapped = ((org.opensearch.client.json.JsonData) source).to(Map.class);
            } catch (final RuntimeException cannotMap) {
                unwrapped = null;
            }
            sourceMap = unwrapped != null ? unwrapped : Map.of();
        } else {
            // Unknown typed source — fall back to an empty map rather than failing the conversion.
            sourceMap = Map.of();
        }

        return builder()
                .id(osHit.id())
                .index(osHit.index())
                .sourceAsMap(sourceMap)
                .score(osHit.score() != null ? osHit.score().floatValue() : 0.0f)
                .build();
    }

    /**
     * Fluent builder for {@link SearchHit}. Unset collection attributes default to an empty map and
     * an unset score defaults to {@code 0.0f}, preserving the lenient behaviour of the former
     * Immutables builder.
     */
    public static final class Builder {

        private String id;
        private String index;
        private Map<String, Object> sourceAsMap = Map.of();
        private float score;
        private Map<String, Object> fields = Map.of();

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder index(final String index) {
            this.index = index;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder sourceAsMap(final Map<String, ?> sourceAsMap) {
            this.sourceAsMap = (Map<String, Object>) sourceAsMap;
            return this;
        }

        public Builder score(final float score) {
            this.score = score;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder fields(final Map<String, ?> fields) {
            this.fields = (Map<String, Object>) fields;
            return this;
        }

        public SearchHit build() {
            return new SearchHit(id, index, sourceAsMap, score, fields);
        }
    }
}
