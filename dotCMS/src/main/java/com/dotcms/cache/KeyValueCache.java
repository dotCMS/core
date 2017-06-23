package com.dotcms.cache;

import com.dotcms.content.model.KeyValue;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;

/**
 * This cache is used to map Key/Value contents in dotCMS. This cache is used to map a property key
 * to its associated value, which is specially useful for objects such as Language Variables.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 */
public interface KeyValueCache extends Cachable {

    /**
     * Add or update in the cache the given Key/Value based on given the given key
     * 
     * @param key
     * @param keyValue
     * @return Contentlet
     * @throws DotDataException
     * @throws DotCacheException
     */
    public KeyValue add(String key, KeyValue keyValue);

    public KeyValue addByContentType(String key, KeyValue keyValue);

    public KeyValue addByContentTypeAndLanguage(String key, KeyValue keyValue);

    /**
     * Retrieves the Key/Value associated to the given key
     * 
     * @param Key
     * @return KeyValue
     * @throws DotDataException
     */
    public KeyValue get(String key);
    
    public KeyValue getByContentType(String key);
    
    public KeyValue getByContentTypeAndLanguage(String key);

    /**
     * Removes all entries from cache
     */
    public void clearCache();

    /**
     * This method removes the Key/Value entry from the cache based on the key
     * 
     * @param object
     * @throws DotDataException
     * @throws DotCacheException
     */
    public void remove(String key);

    /**
     * 
     * @param keyValue
     * @return
     */
    public String generateCacheKey(KeyValue keyValue);

    /**
     * 
     * @param key
     * @param languageId
     * @return
     */
    public String generateCacheKey(String key, long languageId);

}
