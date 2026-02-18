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
 * Immutable wrapper for Elasticsearch SearchHits functionality.
 * This interface provides access to search results without direct dependency on Elasticsearch.
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSearchHits.class)
@JsonDeserialize(as = ImmutableSearchHits.class)
public interface SearchHits extends Iterable<SearchHit>{

    /**
     * Marker that serves to indicate we had an error
     * @return
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
     * Creates a list of SearchHits from a list of Elasticsearch SearchHits.
     * @param hits
     * @return
     */
    static List<SearchHits> from(List<org.elasticsearch.search.SearchHits> hits) {
        return hits.stream().map(SearchHits::from).collect(Collectors.toList());
    }
}