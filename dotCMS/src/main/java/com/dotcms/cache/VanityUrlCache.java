package com.dotcms.cache;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;

/**
 * This cache is used to map the Vanity URLs path to the Vanity Url
 * content.
 *
 * @author oswaldogallango
 */
public abstract class VanityUrlCache implements Cachable {

    /**
     * Retrieves the Cached Vanity URL associated to the given
     * key
     *
     * @return CachedVanityUrl
     */
    public abstract CachedVanityUrl get(String key);

    /**
     * Removes all entries from cache
     */
    public abstract void clearCache();

    /**
     * Removes from cache in all the registered regions a given VanityURL
     * @param vanityURL
     */
    public abstract void remove(final Contentlet vanityURL);

    /**
     * This method removes the Cached Vanity Url entry from the cache
     * based on the key
     */
    public abstract void remove(String key);

    /**
     * Add the vanity URL to the caches
     *
     * @param vanity The vanity URL to add
     */
    public abstract void update(VanityUrl vanity);

    /**
     * Add the vanity URL to the caches and secondary cache
     *
     * @param vanity The vanity URL to add
     */
    public abstract void update(CachedVanityUrl vanity);

    /**
     * Adds a single vanity URL to the cache, not affecting secondary caches.
     * @param vanity
     */
    public abstract void addSingle(final VanityUrl vanity);

    /**
     * Adds a single vanity URL to the cache, not affecting secondary caches.
     *
     * @param vanity The vanity URL to add
     */
    public abstract void addSingle(CachedVanityUrl vanity);


    /**
     * Get the associated list of CachedVanityUrl to current host Id and language Id key
     * @param key The current key composed of the host Id and languageId
     * @return a list of CachedVanityUrl
     */
    public abstract List<CachedVanityUrl> getCachedVanityUrls(String key);

    /**
     * Associate a list of CachedVanityUrl to a Host Id and language id
     *
     * @param cachedVanityUrlList The list of CachedVanityUrls
     */
    public abstract void setCachedVanityUrls(final String siteId, Long languageId,
            final List<CachedVanityUrl> cachedVanityUrlList);

}