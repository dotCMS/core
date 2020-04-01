package com.dotcms.content.elasticsearch;

import java.util.Arrays;
import java.util.Optional;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.SearchHits;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;

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
        Arrays.asList(getGroups()).forEach(group -> cache.flushGroup(group));

    }

    /**
     * a fast hashing algo
     * 
     * @param queryString
     * @return
     */
    @VisibleForTesting
    final String hash(final String queryString) {

        return Hashing.murmur3_128().newHasher().putBytes(queryString.getBytes()).hash().toString();
    }


    /**
     * Optionally returns a
     * 
     * @param queryString
     * @return
     */
    public Optional<SearchHits> get(final String queryString) {

        if (!shouldUseCache || queryString == null || PageMode.get().isAdmin) {
            return Optional.empty();
        }
        SearchHits hits = (SearchHits) cache.getNoThrow(hash(queryString), getPrimaryGroup());
        return Optional.ofNullable(hits);

    }

    

    

    /**
     * taks a searchRequest and returns the SearchHits for it
     * 
     * @param searchRequest
     * @return
     */
    public Optional<SearchHits> get(final SearchRequest searchRequest) {
        if (searchRequest == null || searchRequest.source() == null) {
            return Optional.empty();
        }
        final String source = searchRequest.source().toString() + String.join(",",searchRequest.indices());
        return get(source);

    }

    /**
     * Puts SearchHits into the cache
     * 
     * @param queryString
     * @param hits
     */
    public void put(final String queryString, final SearchHits hits) {
        if (!shouldUseCache || queryString == null || hits == null || PageMode.get().isAdmin) {
            return;
        }
        cache.put(hash(queryString), hits, groups[0]);
    }

    /**
     * Puts SearchHits into the cache
     * 
     * @param searchRequest
     * @param hits
     */
    public void put(final SearchRequest searchRequest, final SearchHits hits) {
        final String source = searchRequest.source().toString() + String.join(",",searchRequest.indices());
        put(source, hits);
    }
}
