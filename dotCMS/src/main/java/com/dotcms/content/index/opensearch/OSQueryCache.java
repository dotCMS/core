package com.dotcms.content.index.opensearch;

import java.util.Optional;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.google.common.annotations.VisibleForTesting;

/**
 * This cache will take an OpenSearch SearchRequest as a key and will return the search results for
 * that SearchRequest from cache if they are available. This entire cache can be turned off by setting
 * OS_CACHE_SEARCH_QUERIES = false in your dotmarketing-config.properties
 *
 * This is the OpenSearch equivalent of ESQueryCache, using opensearch-java client library instead
 * of the Elasticsearch client library.
 *
 * @author fabrizio
 *
 */
public class OSQueryCache implements Cachable {

    private final DotCacheAdministrator cache;

    @VisibleForTesting
    public OSQueryCache(DotCacheAdministrator cache) {
        this.cache = cache;
    }

    public OSQueryCache() {
        this(CacheLocator.getCacheAdministrator());
    }

    final static String[] groups = new String[] {"osquerycache","osquerycountcache"};

    @Override
    public String getPrimaryGroup() {
        return groups[0];
    }

    @Override
    public String[] getGroups() {
        return groups;
    }

    @Override
    public void clearCache() {
       for(final String group : groups){
           cache.flushGroup(group);
       }
    }

    /**
     * This provides the hash String for the given SearchRequest/CountRequest.
     * OpenSearch requests provide consistent hashCode() implementations that
     * we can use for caching purposes.
     *
     * @param request the OpenSearch request (SearchRequest or CountRequest)
     * @return hash string for cache key
     */
    @VisibleForTesting
    final String hash(final Object request) {
        if (request instanceof CountRequest) {
            final CountRequest countRequest = (CountRequest) request;
            // OpenSearch CountRequest should have a proper hashCode implementation
            return String.valueOf(countRequest.hashCode());
        } else if (request instanceof SearchRequest) {
            final SearchRequest searchRequest = (SearchRequest) request;
            // OpenSearch SearchRequest should have a proper hashCode implementation
            return String.valueOf(searchRequest.hashCode());
        } else {
            // Fallback for any other request type
            return String.valueOf(request.hashCode());
        }
    }

    /**
     * Takes a SearchRequest and returns an Optional<HitsMetadata> for it.
     * HitsMetadata is the OpenSearch equivalent of Elasticsearch's SearchHits.
     *
     * @param searchRequest the OpenSearch SearchRequest
     * @return Optional containing cached search results if available
     */
    public Optional<HitsMetadata<Object>> get(final SearchRequest searchRequest) {
        final String hash = hash(searchRequest);
        return Optional.ofNullable((HitsMetadata<Object>) cache.getNoThrow(hash, groups[0]));
    }

    /**
     * Puts search results into the cache using the SearchRequest as the key.
     *
     * @param searchRequest the OpenSearch SearchRequest used as cache key
     * @param hits the search results to cache
     */
    public void put(final SearchRequest searchRequest, final HitsMetadata<Object> hits) {
        final String hash = hash(searchRequest);
        cache.put(hash, hits, groups[0]);
    }

    /**
     * Takes a CountRequest and returns an Optional<Long> for it.
     *
     * @param countRequest the OpenSearch CountRequest
     * @return Optional containing cached count if available
     */
    public Optional<Long> get(final CountRequest countRequest) {
        final String hash = hash(countRequest);
        return Optional.ofNullable((Long) cache.getNoThrow(hash, groups[1]));
    }

    /**
     * Puts Long Count into the cache using the CountRequest as the key.
     *
     * @param countRequest the OpenSearch CountRequest used as cache key
     * @param count the count result to cache
     */
    public void put(final CountRequest countRequest, final Long count) {
        final String hash = hash(countRequest);
        cache.put(hash, count, groups[1]);
    }
}