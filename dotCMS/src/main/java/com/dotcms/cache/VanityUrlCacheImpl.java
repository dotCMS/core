package com.dotcms.cache;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements the cache for Vanity URLs.
 * Is used to map the Vanity URLs path to the Vanity URL
 * content
 *
 * @author oswaldogallango
 */
public class VanityUrlCacheImpl extends VanityUrlCache {

    private DotCacheAdministrator cache;

    private static final String primaryGroup = "VanityURLCache";
    private static final String cachedVanityUrlGroup = "cachedVanityUrlGroup";
    // region's name for the cache
    private String[] groupNames = {primaryGroup, cachedVanityUrlGroup};

    public VanityUrlCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public CachedVanityUrl add(String key, VanityUrl vanityUrl) {
        // Add the key to the cache
        CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        cache.put(getPrimaryGroup()+key, cachedVanityUrl, getPrimaryGroup());
        return cachedVanityUrl;
    }

    @Override
    public CachedVanityUrl get(String key) {
        CachedVanityUrl cachedVanityUrl = null;
        try {
            cachedVanityUrl = (CachedVanityUrl) cache.get(getPrimaryGroup()+key, getPrimaryGroup());
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
    public void remove(String key) {
        try {
            //Remove VanityUrl from the CachedVanityUrlCache
            CachedVanityUrl vanity = (CachedVanityUrl) cache.get(getPrimaryGroup()+key, getPrimaryGroup());
            Set<CachedVanityUrl> cachedVanityUrlList = getCachedVanityUrls(vanity.getSiteId());
            cachedVanityUrlList.remove(vanity);
            setCachedVanityUrls(vanity.getSiteId(),cachedVanityUrlList );

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
    public Set<CachedVanityUrl> getCachedVanityUrls(Host host){
        return getCachedVanityUrls(host.getIdentifier());
    }

    @Override
    public Set<CachedVanityUrl> getCachedVanityUrls(String hostId){
        Set<CachedVanityUrl> vanityUrlList = null;
        try {
            vanityUrlList = (Set<CachedVanityUrl>) cache.get(getCachedVanityUrlGroup()+hostId, getCachedVanityUrlGroup());
        } catch (DotCacheException e) {
            Logger.error(this, "Cache Entry not found", e);
        }catch (Exception e) {
            Logger.error(this, "Cache Entry not found 2", e);
        }
        if(vanityUrlList == null){
            vanityUrlList = new HashSet<>();
        }
        return vanityUrlList;
    }

    @Override
    public void setCachedVanityUrls(Host host, Set<CachedVanityUrl> cachedVanityUrlList){
        setCachedVanityUrls(host.getIdentifier(), cachedVanityUrlList);
    }

    @Override
    public void setCachedVanityUrls(String hostId, Set<CachedVanityUrl> cachedVanityUrlList){
        cache.put(getCachedVanityUrlGroup()+hostId, cachedVanityUrlList, getCachedVanityUrlGroup());
    }
}
