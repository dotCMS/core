package com.dotcms.cache;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Cachable;
import java.util.List;

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
     * @return Contentlet
     */
    abstract public VanityUrl add(String key, VanityUrl vanityUrl);

    /**
     * Retrieves the Vanity URL associated to the given
     * key
     *
     * @return DefaultVanityUrl
     */
    abstract public VanityUrl get(String key);

    /**
     * Removes all entries from cache
     */
    abstract public void clearCache();

    /**
     * This method removes the DefaultVanityUrl entry from the cache
     * based on the key
     */
    abstract public void remove(String key);

    /**
     * Get the associated list of CachedVanityUrl to current host
     * @param host The current Host
     * @return a list of CachedVanityUrl
     */
    abstract public List<CachedVanityUrl> getCachedVanityUrls(Host host);

    /**
     * Get the associated list of CachedVanityUrl to current host Id
     * @param hostId The current Host Id
     * @return a list of CachedVanityUrl
     */
    abstract public List<CachedVanityUrl> getCachedVanityUrls(String hostId);

    /**
     * Associate a list of CachedVanityUrl to a Host
     * @param host The current Host
     * @param cachedVanityUrlList The list of CachedVanityUrls
     */
    abstract public void setCachedVanityUrls(Host host, List<CachedVanityUrl> cachedVanityUrlList);

    /**
     * Associate a list of CachedVanityUrl to a Host
     * @param hostId The current Host Id
     * @param cachedVanityUrlList The list of CachedVanityUrls
     */
    abstract public void setCachedVanityUrls(String hostId, List<CachedVanityUrl> cachedVanityUrlList);
}
