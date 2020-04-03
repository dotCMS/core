package com.dotcms.content.elasticsearch;

import java.util.Optional;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.SearchHits;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Config;
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

    private final boolean shouldUseCache;

    @VisibleForTesting
    public ESQueryCache(DotCacheAdministrator cache, boolean shouldUseCache) {
        this.cache = cache;
        this.shouldUseCache = shouldUseCache;
    }


    public ESQueryCache() {
        this(CacheLocator.getCacheAdministrator(), LicenseManager.getInstance().isEnterprise()
                        && Config.getBooleanProperty("ES_CACHE_SEARCH_QUERIES", true));
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
     * a fast hashing algo
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
     * taks a searchRequest and returns the SearchHits for it
     * 
     * @param searchRequest
     * @return
     */
    public Optional<SearchHits> get(final SearchRequest searchRequest) {
        if (searchRequest == null || !shouldUseCache) {
            return Optional.empty();
        }
        final String hash = hash(searchRequest);
        return Optional.ofNullable((SearchHits) cache.getNoThrow(hash, groups[0]));

    }


    /**
     * Puts SearchHits into the cache
     * 
     * @param searchRequest
     * @param hits
     */
    public void put(final SearchRequest searchRequest, final SearchHits hits) {
        if (searchRequest == null || !shouldUseCache) {
            return;
        }
        final String hash = hash(searchRequest);
        cache.put(hash, hits, groups[0]);
    }
}
