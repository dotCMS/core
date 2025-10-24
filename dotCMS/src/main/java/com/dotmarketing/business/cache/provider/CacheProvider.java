package com.dotmarketing.business.cache.provider;

import com.dotmarketing.util.Logger;
import java.io.Serializable;
import java.util.Set;

/**
 * This class will be extended by any Cache implementation that needs/want to belong to the Cache Providers execution chain.
 * <br/>
 * <br/>
 * In order add and use a custom CacheProvider in the Cache Providers execution chain is required an Enterprise
 * License, without it only the default CacheProviders can be use ({@link com.dotmarketing.business.cache.provider.guava.GuavaCache},
 * {@link com.dotmarketing.business.cache.provider.h2.H2CacheLoader}).
 * <br/>
 * <br/>
 * With a valid Enterprise License the CacheProviders to use can be specified using properties specifying the chain for a specific cache
 * region <strong>cache.mycacheregionexample.chain</strong> in the <strong>dotmarketing-config.properties</strong> file,
 * with those properties you can specify the list of classes to use as CacheProviders for each region, that list will also define the order of execution of those providers.
 * <p/>
 * In order to define a default chain to use for cache regions that are not specified in the <strong>dotmarketing-config.properties</strong>
 * the property <strong>cache.default.chain</strong> must be used.
 * <p/>
 * <strong>Examples:</strong>
 * <ul>
 * <li>cache.default.chain=com.dotmarketing.business.cache.provider.guava.TestCacheProvider,com.dotmarketing.business.cache.provider.guava.GuavaCache,com.dotmarketing.business.cache.provider.h2.H2CacheLoader</li>
 * <li>cache.velocitymemoryonlycache.chain=com.dotmarketing.business.cache.provider.guava.GuavaCache</li>
 * <li>cache.velocityuservtlcache.chain=com.dotmarketing.business.cache.provider.redis.RedisProvider,com.dotmarketing.business.cache.provider.h2.H2CacheLoader</li>
 * </ul>
 *
 * @author Jonathan Gamba
 *         Date: 8/31/15
 */
public abstract class CacheProvider implements Serializable {

    protected static final String ONLY_MEMORY_GROUP = "VelocityMemoryOnlyCache".toLowerCase();
    protected static final String USER_VTLS_GROUP = "VelocityUserVTLCache".toLowerCase();

    /**
     * Returns the human readable name for this Cache Provider
     *
     * @return
     */
    public abstract String getName ();

    /**
     * Returns a unique key for this Cache Provider
     *
     * @return
     */
    public abstract String getKey ();

    /**
     * Specifies whether the underlying cache is implicitly shared over all nodes in a clustered environment
     */
    public abstract boolean isDistributed();

    public boolean isSingleton() {
        return true;
    }

    /**
     * Initializes the provider
     *
     * @throws Exception
     */
    public abstract void init () throws Exception;

    /**
     * Checks if the provider was initialized
     *
     * @throws Exception
     */
    public abstract boolean isInitialized () throws Exception;

    public void put(String group, String key, Object content, long ttlMillis){
        Logger.warn(this.getClass(), "This cache implementation does not support per object TTL, ignoring it.");
        put(group, key, content);
    }

    /**
     * Adds the given content to the given region and for the given key
     *
     * @param group
     * @param key
     * @param content
     */
    public abstract void put ( String group, String key, final Object content );

    /**
     * Searches and return the content in a given region and with a given key
     *
     * @param group
     * @param key
     * @return
     */
    public abstract Object get ( String group, String key );

    /**
     * Invalidates a given key for a given region
     *
     * @param group
     * @param key
     */
    public abstract void remove ( String group, String key );

    /**
     * Invalidates the given region
     *
     * @param group
     */
    public abstract void remove ( String group );

    /**
     * Invalidates all the regions
     */
    public abstract void removeAll ();

    /**
     * Returns the keys found inside a given region
     *
     * @param group
     * @return
     */
    public abstract Set<String> getKeys ( String group );

    /**
     * Returns all the regions found in this Provider
     *
     * @return
     */
    public abstract Set<String> getGroups ();

    /**
     * Returns stats information of the objects handle by this provider
     *
     * @return
     */
    public abstract CacheProviderStats getStats ();

    /**
     * Shutdowns the Provider
     */
    public abstract void shutdown ();

}
