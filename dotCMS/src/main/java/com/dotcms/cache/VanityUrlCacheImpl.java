package com.dotcms.cache;

import com.dotcms.services.VanityUrlServices;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import java.util.LinkedHashSet;
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

    private DotCacheAdministrator cache;

    private static final String PRIMARY_GROUP = "VanityURLCache";
    private static final String CACHED_VANITY_URL_GROUP = "cachedVanityUrlGroup";

    private static final String[] groupNames = {PRIMARY_GROUP, CACHED_VANITY_URL_GROUP};

    public VanityUrlCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public CachedVanityUrl add(final String key, final CachedVanityUrl vanityUrl) {
        // Add the key to the cache
        cache.put(key, vanityUrl, getPrimaryGroup());
        return vanityUrl;
    }

    @Override
    public CachedVanityUrl get(final String key) {
        CachedVanityUrl cachedVanityUrl = null;
        try {
            cachedVanityUrl = (CachedVanityUrl) cache.get(key, getPrimaryGroup());
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
    public void remove(final Contentlet vanityURL) {

        try {

            /*
            First get the records we want to remove from secondary cache group and
            delete it from the primary cache group, we do this in order to avoid to
            flush the primary group.
             */
            Set<CachedVanityUrl> cachedVanityUrls = this
                    .getCachedVanityUrls(VanityUrlUtil.sanitizeSecondCachedKey(vanityURL));
            for (CachedVanityUrl toRemove : cachedVanityUrls) {
                this.remove(VanityUrlUtil.sanitizeKey(toRemove.getSiteId(), toRemove.getUrl(),
                        toRemove.getLanguageId()));
            }

            this.remove(VanityUrlUtil.sanitizeKey(vanityURL));
            this.removeCachedVanityUrls(VanityUrlUtil.sanitizeSecondCachedKey(vanityURL));

        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.debug(VanityUrlServices.class,
                    "Error trying to invalidate Vanity URL with identifier:" + vanityURL
                            .getIdentifier(), e);
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

    @Override
    public void update(CachedVanityUrl vanity) {

        try {

            //Update primary Cache
            this.add(VanityUrlUtil
                            .sanitizeKey(vanity.getSiteId(), vanity.getUrl(), vanity.getLanguageId()),
                    vanity);

            //update Secondary cache
            Set<CachedVanityUrl> siteCachedVanityUrl = this.getCachedVanityUrls(VanityUrlUtil
                    .sanitizeSecondCacheKey(vanity.getSiteId(), vanity.getLanguageId()));
            siteCachedVanityUrl.add(vanity);

            this.setCachedVanityUrls(
                    VanityUrlUtil
                            .sanitizeSecondCacheKey(vanity.getSiteId(), vanity.getLanguageId()),
                    siteCachedVanityUrl);
        } catch (DotRuntimeException e) {
            Logger.debug(this, "Error trying to update Vanity URL in cache", e);
        }
    }

    @Override
    public void update(VanityUrl vanity) {
        this.update(new CachedVanityUrl(vanity));
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
        return PRIMARY_GROUP;
    }

    public String getCachedVanityUrlGroup() {
        return CACHED_VANITY_URL_GROUP;
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
            vanityUrlList = new LinkedHashSet<>();
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
