/**
 * 
 */
package com.dotmarketing.business;

import com.dotmarketing.business.cache.transport.CacheTransport;

import java.util.List;
import java.util.Map;
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
	 */
	void flushAlLocalOnly ();
	
	/**
	 * Flushes all items that belong to the specified group on the local server only
	 * @param group
	 */
	void flushGroupLocalOnly ( String group );
	
	/**
	 * Get an object from the cache
	 * @param key
	 * @return
	 */
	Object get ( String key, String group ) throws DotCacheException;
	
	/**
	 * Puts an object in a cache
	 * This will create journal entries for other servers in a clustered environment. 
	 * @param key
	 * @param content
	 * @param groups
	 */
	void put ( String key, Object content, String group );

	/**
	 * Remove an object from the cache.  
	 * This will create journal entries for other servers in a clustered environment. 
	 * @param key
	 */
	void remove ( String key, String group );
	
	/**
	 * Remove an object from the cache on the local server only
	 * @param key
	 */
	void removeLocalOnly ( String key, String group );
	
	/**
	 * Should be called on shutdown of the dotcms
	 */
	void shutdown ();

	List<Map<String, Object>> getCacheStatsList ();

	Class getImplementationClass ();

	DotCacheAdministrator getImplementationObject ();

	/**
	 * Returns the CacheTransport in use if any
	 *
	 * @return
	 */
	CacheTransport getTransport ();

	/**
	 * Sets a new CacheTransport implementation to be use, if there was already
	 * one in use the shutdown method will be executed in that implementation.
	 *
	 * @param transport
	 */
	void setTransport ( CacheTransport transport );

}