package com.dotcms.cache;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.business.Cachable;
import java.util.Set;

/**
 * This cache is used to map the Vanity URLs path to the Vanity Url
 * content.
 *
 * @author oswaldogallango
 */
public abstract class VanityUrlCache implements Cachable {

    /**
     * Add or update in the cache the given Vanity URL
     * based on given the given key
     *
     * @return CachedVanityUrl object
     */
    abstract public CachedVanityUrl add(String key, VanityUrl vanityUrl);

    /**
     * Retrieves the Cached Vanity URL associated to the given
     * key
     *
     * @return CachedVanityUrl
     */
    abstract public CachedVanityUrl get(String key);

    /**
     * Removes all entries from cache
     */
    abstract public void clearCache();

    /**
     * This method removes the Cached Vanity Url entry from the cache
     * based on the key
     */
    abstract public void remove(String key);

    /**
     * Get the associated list of CachedVanityUrl to current host Id and language Id key
     * @param key The current key composed of the host Id and languageId
     * @return a list of CachedVanityUrl
     */
    abstract public Set<CachedVanityUrl> getCachedVanityUrls(String key);

    /**
     * Associate a list of CachedVanityUrl to a Host Id and language id key
     * @param key The current key composed of the host Id and languageId
     * @param cachedVanityUrlList The list of CachedVanityUrls
     */
    abstract public void setCachedVanityUrls(String key, Set<CachedVanityUrl> cachedVanityUrlList);

    /**
     * remove the associate a list of CachedVanityUrl associated to a Host Id and language id key
     * @param key The current key composed of the host Id and languageId
     */
    abstract public void removeCachedVanityUrls(String key);
}
