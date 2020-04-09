package com.dotcms.content.elasticsearch;

import java.util.Optional;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.SearchHits;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.google.common.annotations.VisibleForTesting;

/**
 * This cache will take an ElasticSearch SearchRequest as a key and will return the SearchHits for
 * that SearchRequest from cache if they are available.  This entire cache can be turned off by setting
 * ES_CACHE_SEARCH_QUERIES = false in your dotmarketing-config.properties
 * 
 * @author will
 *
 */
public class ESQueryCache implements Cachable {


    private final DotCacheAdministrator cache;



    
    @VisibleForTesting
    public ESQueryCache(DotCacheAdministrator cache) {
        this.cache = cache;

    }


    public ESQueryCache() {
        this(CacheLocator.getCacheAdministrator());
    }


    final static String[] groups = new String[] {"esquerycache"};

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
        cache.flushGroup(groups[0]);

    }

    /**
     * This provides the hash String for the given SearchRequest.
     * Taking a look at the SearchRequest.hashCode(), it seems like 
     * it will suit our purpose
     * 
     * @param queryString
     * @return
     */
    @VisibleForTesting
    final String hash(final SearchRequest searchRequest) {


        // return
        // Hashing.murmur3_128().newHasher().putBytes(searchRequest.toString().getBytes()).hash().toString();

        return String.valueOf(searchRequest.hashCode());
    }



    /**
     * taks a SearchRequest and returns and Optional<SearchHits> for it
     * 
     * @param searchRequest
     * @return
     */
    public Optional<SearchHits> get(final SearchRequest searchRequest) {
        final String hash = hash(searchRequest);
        return Optional.ofNullable((SearchHits) cache.getNoThrow(hash, groups[0]));

    }


    /**
     * Puts SearchHits into the cache using the SearchRequest as the key
     * 
     * @param searchRequest
     * @param hits
     */
    public void put(final SearchRequest searchRequest, final SearchHits hits) {
        final String hash = hash(searchRequest);
        cache.put(hash, hits, groups[0]);
    }
}
