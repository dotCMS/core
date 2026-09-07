package com.dotcms.content.index.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable domain representation of search results from any search engine.
 *
 * <p>This record provides a unified abstraction layer for collections of search results,
 * allowing the application to work with search hits without depending on specific
 * search engine libraries (Elasticsearch, OpenSearch, etc.).</p>
 *
 * <p>Accessors are bean-style ({@code getHits()}, {@code getTotalHits()}) so the type works
 * directly from Velocity templates (e.g. {@code $topHits.getHits()}) without extra alias methods;
 * the record components are named accordingly and the {@link JsonProperty} annotations keep the
 * JSON contract clean ({@code hits}, {@code totalHits}, {@code hasError}).</p>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * SearchHits results = SearchHits.from(elasticsearchHits);
 * for (SearchHit hit : results) {
 *     String docId = hit.getId();
 * }
 * long totalCount = results.getTotalHits().value();
 * if (results.hasError()) { ... }
 * </pre>
 *
 * @param hasError     {@code true} if this represents an error state, {@code false} for a
 *                     successful search
 * @param getHits      the list of search hits
 * @param getTotalHits the total hits information
 * @author Fabrizio Araya
 * @see SearchHit
 * @see TotalHits
 * @see com.dotcms.content.index.ContentFactoryIndexOperations
 */
public record SearchHits(
        @JsonProperty("hasError") boolean hasError,
        @JsonProperty("hits") List<SearchHit> getHits,
        @JsonProperty("totalHits") TotalHits getTotalHits) implements Iterable<SearchHit> {

    /**
     * Canonical constructor. {@code getHits} defaults to an empty list and {@code getTotalHits} to
     * {@link TotalHits#empty()} when {@code null}, so the accessors never return {@code null}
     * (mirrors the previous Immutables defaults).
     */
    public SearchHits {
        getHits = getHits == null ? List.of() : getHits;
        getTotalHits = getTotalHits == null ? TotalHits.empty() : getTotalHits;
    }

    /**
     * Returns an iterator over the search hits (implements {@link Iterable}).
     *
     * @return an iterator for the search hits
     */
    @Override
    public @NotNull Iterator<SearchHit> iterator() {
        return getHits().iterator();
    }

    /**
     * Creates a new SearchHits builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty SearchHits instance.
     *
     * @return an empty SearchHits
     */
    public static SearchHits empty() {
        return builder()
                .totalHits(TotalHits.empty())
                .build();
    }

    /**
     * Creates an error SearchHits instance to represent search errors.
     *
     * @return a SearchHits instance representing an error state
     */
    public static SearchHits errorHit() {
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
    public static SearchHits from(org.elasticsearch.search.SearchHits esSearchHits) {
        if (esSearchHits == null) {
            return empty();
        }

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
    public static SearchHits from(org.opensearch.client.opensearch.core.search.HitsMetadata<?> osHitsMetadata) {
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
     *
     * @param hits list of Elasticsearch SearchHits to convert
     * @return list of converted SearchHits instances
     */
    public static List<SearchHits> from(List<org.elasticsearch.search.SearchHits> hits) {
        return hits.stream().map(SearchHits::from).collect(Collectors.toList());
    }

    /**
     * Creates a list of SearchHits from a list of OpenSearch HitsMetadata.
     *
     * @param hitsMetadata list of OpenSearch HitsMetadata
     * @return list of SearchHits
     */
    public static List<SearchHits> fromOpenSearch(List<org.opensearch.client.opensearch.core.search.HitsMetadata<?>> hitsMetadata) {
        return hitsMetadata.stream().map(SearchHits::from).collect(Collectors.toList());
    }

    /**
     * Fluent builder for {@link SearchHits}. Unset attributes default to a non-error state, an empty
     * hit list and {@link TotalHits#empty()}, preserving the lenient behaviour of the former
     * Immutables builder.
     */
    public static final class Builder {

        private boolean hasError;
        private List<SearchHit> hits = new ArrayList<>();
        private TotalHits totalHits = TotalHits.empty();

        public Builder hasError(final boolean hasError) {
            this.hasError = hasError;
            return this;
        }

        public Builder hits(final List<SearchHit> hits) {
            this.hits = new ArrayList<>(hits);
            return this;
        }

        public Builder addHits(final SearchHit hit) {
            this.hits.add(hit);
            return this;
        }

        public Builder addAllHits(final Iterable<? extends SearchHit> hits) {
            hits.forEach(this.hits::add);
            return this;
        }

        public Builder totalHits(final TotalHits totalHits) {
            this.totalHits = totalHits;
            return this;
        }

        public SearchHits build() {
            return new SearchHits(hasError, hits, totalHits);
        }
    }
}
