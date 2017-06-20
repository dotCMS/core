package com.dotcms.cache;

import com.dotcms.content.model.KeyValue;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;

/**
 * This cache is used to map Key/Value contents in dotCMS.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 */
public abstract class KeyValueCache implements Cachable {

	/**
	 * Add or update in the cache the given Vanity URL
	 * based on given the given key
	 * @param key 
	 * @param vanityUrl
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract public KeyValue add(String key, KeyValue vanityUrl);

	/**
	 * Retrieves the Vanity URL associated to the given
	 * key
	 * @param Key
	 * @return DefaultVanityUrl
	 * @throws DotDataException
	 */
	abstract public KeyValue get(String key);

	/**
	 * Removes all entries from cache
	 */
	abstract public void clearCache();

	/**
	 * This method removes the DefaultVanityUrl entry from the cache
	 * based on the key
	 * @param object
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract public void remove(String key);

}
