package com.dotcms.cache;

import com.dotcms.content.model.VanityUrl;
import com.dotmarketing.business.Cachable;

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

}
