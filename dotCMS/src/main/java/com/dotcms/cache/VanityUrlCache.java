package com.dotcms.cache;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
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
     * Get the associated list of CachedVanityUrl to current host
     * @param host The current Host
     * @return a list of CachedVanityUrl
     */
    abstract public Set<CachedVanityUrl> getCachedVanityUrls(Host host);

    /**
     * Get the associated list of CachedVanityUrl to current host Id
     * @param hostId The current Host Id
     * @return a list of CachedVanityUrl
     */
    abstract public Set<CachedVanityUrl> getCachedVanityUrls(String hostId);

    /**
     * Associate a list of CachedVanityUrl to a Host
     * @param host The current Host
     * @param cachedVanityUrlList The list of CachedVanityUrls
     */
    abstract public void setCachedVanityUrls(Host host, Set<CachedVanityUrl> cachedVanityUrlList);

    /**
     * Associate a list of CachedVanityUrl to a Host
     * @param hostId The current Host Id
     * @param cachedVanityUrlList The list of CachedVanityUrls
     */
    abstract public void setCachedVanityUrls(String hostId, Set<CachedVanityUrl> cachedVanityUrlList);
}
