package com.dotmarketing.business;

import com.dotcms.cache.CacheValue;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.UtilMethods;

/**
 * Provides the caching implementation for HTML pages. This approach uses a main key to retrieve a cached page, and a
 * subkey to retrieve the different versions of it. With this structure, during the removal of a page, all the different
 * versions of it will also be deleted easily. So, basically:
 * <ul>
 * 	<li>
 * 		The main key is composed of:
 * 		<ul>
 * 		<li>The page Inode.</li>
 * 		<li>The page modification date in milliseconds.</li>
 * 		</ul>
 *  </li>
 *  <li>
 * 		The subkey is composed of:
 * 		<ul>
 * 		<li>The current user ID.</li>
 * 		<li>The currently selected language ID.</li>
 * 		<li>The URL map.</li>
 * 		<li>The query String in the URL.</li>
 * 		</ul>
 *  </li>
 * </ul>
 *
 * @author Jose Castro
 * @version 1.0
 * @since 10-17-2014
 *
 */
public class BlockPageCacheImpl extends BlockPageCache {


    private static final String PRIMARY_GROUP = "BlockDirectiveHTMLPageCache";

    private DotCacheAdministrator cache = null;

    /**
     * Default constructor. Initializes the internal caching structures.
     */
    public BlockPageCacheImpl() {
        this.cache = CacheLocator.getCacheAdministrator();

    }

    @Override
    public String getPrimaryGroup() {
        return PRIMARY_GROUP;
    }

    @Override
    public String[] getGroups() {
        return new String[]{PRIMARY_GROUP};

    }

    @Override
    public void clearCache() {
        cache.flushGroup(PRIMARY_GROUP);
    }

    @Override
    public void add(IHTMLPage page, final String pageContent, PageCacheParameters pageCacheParams) {
        if (UtilMethods.isEmpty(() -> page.getIdentifier()) || pageCacheParams == null) {
            return;
        }

        final String cacheKey = pageCacheParams.getKey();

        long ttl = page.getCacheTTL()>0 ? page.getCacheTTL() * 1000 : Long.MAX_VALUE;
        CacheValue cacheValue = new CacheValue(pageContent, ttl);

        this.cache.put(cacheKey, cacheValue, PRIMARY_GROUP);

    }


    @Override
    public String get(final IHTMLPage page, final PageCacheParameters pageCacheParams) {

        if (UtilMethods.isEmpty(() -> page.getIdentifier()) || pageCacheParams == null) {
            return null;
        }

        final String cacheKey = pageCacheParams.getKey();

        // Look up the cached versions of the page based on inode and moddate
        Object cachedValue = this.cache.getNoThrow(cacheKey, PRIMARY_GROUP);
        if(cachedValue instanceof String){
            return (String) cachedValue;
        }
        if (cachedValue instanceof CacheValue) {
            return (String) ((CacheValue) cachedValue).value;
        }

        return null;

    }

    @Override
    public void remove(IHTMLPage page) {

        // not implemented

    }

}
