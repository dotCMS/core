/**
 * 
 */
package com.dotmarketing.business;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgroups.JChannel;


/**
 * @author Jason Tesser
 * @since 1.6
 *
 */
public interface DotCacheAdministrator  {

	public static final String ROOT_GOUP = "root";
	
	/**
	 * Returns all keys within the group
	 * @param group
	 * @return
	 */
	public Set<String> getKeys(String group); 
	
	/**
	 * Flush the entire cache immediately.
	 */
	public void flushAll();

	/**
	 * Flushes all items that belong to the specified group.
	 * @param group
	 */
	public void flushGroup(String group);
	
	/**
	 * Flushes all cache locally
	 */
	public void flushAlLocalOnlyl();
	
	/**
	 * Flushes all items that belong to the specified group on the local server only
	 * @param group
	 */
	public void flushGroupLocalOnly(String group);
	
	/**
	 * Get an object from the cache
	 * @param key
	 * @return
	 */
	public Object get(String key, String group)throws DotCacheException;
	
	/**
	 * Puts an object in a cache
	 * This will create journal entries for other servers in a clustered environment. 
	 * @param key
	 * @param content
	 * @param groups
	 */
	public void put(String key, Object content, String group);
	
	/**
	 * Remove an object from the cache.  
	 * This will create journal entries for other servers in a clustered environment. 
	 * @param key
	 */
	public void remove(String key, String group);
	
	/**
	 * Remove an object from the cache on the local server only
	 * @param key
	 */
	public void removeLocalOnly(String key, String group);
	
	/**
	 * Should be called on shutdown of the dotcms
	 */
	public void shutdown();
	
	public JChannel getJGroupsChannel();
	
	public List<Map<String, Object>> getCacheStatsList();
	
	public Class getImplementationClass();
	public DotCacheAdministrator getImplementationObject();
}
