package com.dotcms.content.index.domain;

import com.dotcms.content.index.domain.ImmutableSearchHits;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Immutable wrapper for Elasticsearch SearchHits functionality.
 * This interface provides access to search results without direct dependency on Elasticsearch.
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSearchHits.class)
@JsonDeserialize(as = ImmutableSearchHits.class)
public interface SearchHits {

    /**
     * Returns the list of search hits.
     *
     * @return the list of search hits
     */
    List<SearchHit> getHits();

    /**
     * Returns the total hits information.
     *
     * @return the total hits
     */
    TotalHits getTotalHits();

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
                .totalHits(TotalHits.builder().value(0).build())
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
}