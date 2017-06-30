package com.dotcms.cache;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.List;

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
    public VanityUrl add(String key, VanityUrl vanityUrl) {
        // Add the key to the cache
        cache.put(getPrimaryGroup()+key, vanityUrl, getPrimaryGroup());
        return vanityUrl;
    }

    @Override
    public VanityUrl get(String key) {
        VanityUrl vanityUrl = null;
        try {
            vanityUrl = (DefaultVanityUrl) cache.get(getPrimaryGroup()+key, getPrimaryGroup());
        } catch (DotCacheException e) {
            Logger.debug(this, "Cache Entry not found", e);
        }
        return vanityUrl;
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
            VanityUrl vanity = (VanityUrl) cache.get(getPrimaryGroup()+key, getPrimaryGroup());
            List<CachedVanityUrl> cachedVanityUrlList = getCachedVanityUrls(vanity.getSite());
            List<CachedVanityUrl> newListCachedVanityUrl = new ArrayList<>();
            for(CachedVanityUrl cachedVanityUrl : cachedVanityUrlList){
                if(!cachedVanityUrl.getVanityUrlId().equals(vanity.getIdentifier())){
                    newListCachedVanityUrl.add(cachedVanityUrl);
                }
            }
            setCachedVanityUrls(vanity.getSite(),newListCachedVanityUrl );

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
    public List<CachedVanityUrl> getCachedVanityUrls(Host host){
        return getCachedVanityUrls(host.getIdentifier());
    }

    @Override
    public List<CachedVanityUrl> getCachedVanityUrls(String hostId){
        List<CachedVanityUrl> vanityUrlList = null;
        try {
            vanityUrlList = (List<CachedVanityUrl>) cache.get(getCachedVanityUrlGroup()+hostId, getCachedVanityUrlGroup());
        } catch (DotCacheException e) {
            Logger.error(this, "Cache Entry not found", e);
        }catch (Exception e) {
            Logger.error(this, "Cache Entry not found 2", e);
        }
        if(vanityUrlList == null){
            vanityUrlList = new ArrayList<>();
        }
        return vanityUrlList;
    }

    @Override
    public void setCachedVanityUrls(Host host, List<CachedVanityUrl> cachedVanityUrlList){
        setCachedVanityUrls(host.getIdentifier(), cachedVanityUrlList);
    }

    @Override
    public void setCachedVanityUrls(String hostId, List<CachedVanityUrl> cachedVanityUrlList){
        cache.put(getCachedVanityUrlGroup()+hostId, cachedVanityUrlList, getCachedVanityUrlGroup());
    }
}
