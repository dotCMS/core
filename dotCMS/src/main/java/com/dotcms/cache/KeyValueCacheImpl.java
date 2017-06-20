package com.dotcms.cache;

import com.dotcms.content.model.KeyValue;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

/**
 * This class implements the cache for Vanity URLs.
 * Is used to map the Vanity URLs path to the Vanity URL
 * content
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public class KeyValueCacheImpl extends KeyValueCache {

	private DotCacheAdministrator cache;

	private String primaryGroup = "VanityURLCache";
	// region's name for the cache
	private String[] groupNames = {primaryGroup};

	public KeyValueCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	public KeyValue add(String key, KeyValue vanityUrl) {
		// Add the key to the cache
		cache.put(key, vanityUrl, primaryGroup);

		return vanityUrl;	
	}

	@Override
	public KeyValue get(String key) {
		KeyValue vanityUrl = null;
		try{
			vanityUrl = (KeyValue) cache.get(key,primaryGroup);
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return vanityUrl;	
	}

	/**
	 * Clear the cache
	 */
	public void clearCache() {
		// clear the cache
		cache.flushGroup(primaryGroup);
	}

	/**
	 * This method removes the given Vanity Url path from the cache
	 * @param key The vanity url path
	 * @param languageId The vanity url language ID
	 */
	public void remove(String key){
		try{
			cache.remove(key,primaryGroup);
		}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
	}

	/**
	 * Get the cache groups
	 * @return array of cache groups
	 */
	public String[] getGroups() {
		return groupNames;
	}

	/**
	 * get The cache primary group
	 * @return primary group name
	 */
	public String getPrimaryGroup() {
		return primaryGroup;
	} 

}
