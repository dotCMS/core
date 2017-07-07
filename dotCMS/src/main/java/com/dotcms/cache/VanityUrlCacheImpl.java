package com.dotcms.cache;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements {@link VanityUrlCache} the cache for Vanity URLs.
 * Is used to map the Vanity URLs path to the Vanity URL content
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 24, 2017
 */
public class VanityUrlCacheImpl extends VanityUrlCache {

    private static DotCacheAdministrator cache;

    private static final String primaryGroup = "VanityURLCache";
    private static final String cachedVanityUrlGroup = "cachedVanityUrlGroup";
    // region's name for the cache
    private static final String[] groupNames = {primaryGroup, cachedVanityUrlGroup};

    public VanityUrlCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public CachedVanityUrl add(final String key, final VanityUrl vanityUrl) {
        // Add the key to the cache
        CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        cache.put( key, cachedVanityUrl, getPrimaryGroup());

        return cachedVanityUrl;
    }

    @Override
    public CachedVanityUrl get(final String key) {
        CachedVanityUrl cachedVanityUrl = null;
        try {
            cachedVanityUrl = (CachedVanityUrl) cache
                    .get( key, getPrimaryGroup());
        } catch (DotCacheException e) {
            Logger.debug(this, "Cache Entry not found", e);
        }
        return cachedVanityUrl;
    }

    @Override
    public void clearCache() {
        // clear the cache
        for (String cacheGroup : getGroups()) {
            cache.flushGroup(cacheGroup);
        }
    }

    @Override
    public void remove(final String key) {
        try {
            cache.remove(key, getPrimaryGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache not able to be removed", e);
        }
    }

    /**
     * Get the cache groups
     *
     * @return array of cache groups
     */
    public String[] getGroups() {
        return groupNames;
    }

    /**
     * get The cache primary group
     *
     * @return primary group name
     */
    public String getPrimaryGroup() {
        return primaryGroup;
    }

    public String getCachedVanityUrlGroup() {
        return cachedVanityUrlGroup;
    }

    @Override
    public Set<CachedVanityUrl> getCachedVanityUrls(final String key) {
        Set<CachedVanityUrl> vanityUrlList = null;
        try {
            vanityUrlList = (Set<CachedVanityUrl>) cache.get( key, getCachedVanityUrlGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache Entry not found", e);
        }
        if (vanityUrlList == null) {
            vanityUrlList = new HashSet<>();
        }
        return vanityUrlList;
    }

    @Override
    public void setCachedVanityUrls(final String key,
            final Set<CachedVanityUrl> cachedVanityUrlList) {
        cache.put( key, cachedVanityUrlList, getCachedVanityUrlGroup());
    }

    @Override
    public void removeCachedVanityUrls(String key) {
        try {
            cache.remove(key, getCachedVanityUrlGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache not able to be removed", e);
        }
    }
}
