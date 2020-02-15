/**
 * 
 */
package com.dotmarketing.business;

import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.transport.CacheTransport;

import java.util.List;
import java.util.Set;


/**
 * @author Jason Tesser
 * @since 1.6
 *
 */
public interface DotCacheAdministrator  {

	public static final String ROOT_GOUP = "root";

	/**
	 * Initializes the CacheProviders
	 */
	void initProviders ();

	/**
	 * Returns all groups in the cache
	 *
	 * @return
	 */
	Set<String> getGroups ();
	
	/**
	 * Flush the entire cache immediately.
	 */
	void flushAll ();

	/**
	 * Flushes all items that belong to the specified group.
	 * @param group
	 */
	void flushGroup ( String group );
	
	/**
	 * Flushes all cache on the local server only
	 * Depending on value for ignoreDistributed flag, it won't flush the groups when distributed
	 */
	void flushAlLocalOnly (boolean ignoreDistributed);
	
	/**
	 * Flushes all items that belong to the specified group on the local server only
	 * Depending on value for ignoreDistributed flag, it won't flush the group when distributed
	 * @param group
	 */
	void flushGroupLocalOnly ( String group, boolean ignoreDistributed );
	
	/**
	 * Get an object from the cache
	 * @param key
	 * @return
	 */
	Object get ( String key, String group ) throws DotCacheException;
	
	   /**
     * Get an object from the cache
     * @param key
     * @return
     */
    default Object getNoThrow ( String key, String group ) {
        try {
            return get(key, group);
        }
        catch(DotCacheException e) {
            return null;
        }
        
    };
	/**
	 * Puts an object in a cache
	 * This will create journal entries for other servers in a clustered environment. 
	 * @param key
	 * @param content
	 * @param group
	 */
	void put ( String key, Object content, String group );

	/**
	 * Remove an object from the cache.  
	 * This will create journal entries for other servers in a clustered environment. 
	 * @param key
	 */
	void remove ( String key, String group );

	/**
	 * Remove an object from the cache on the local server only.
	 * Depending on value for ignoreDistributed flag, it won't remove the object when distributed
	 * @param key
	 */
	void removeLocalOnly ( String key, String group, boolean ignoreDistributed );

	/**
	 * Should be called on shutdown of the dotcms
	 */
	void shutdown ();

	List<CacheProviderStats> getCacheStatsList ();

	Class getImplementationClass ();

	DotCacheAdministrator getImplementationObject ();

	public void invalidateCacheMesageFromCluster ( String message );

	/**
	 * Returns the CacheTransport in use if any
	 *
	 * @return
	 */
	default CacheTransport getTransport () {
	    return CacheLocator.getCacheTransport();
	}


}