package com.dotcms.cache;

import java.util.List;

import com.dotcms.keyvalue.model.KeyValue;
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
     * @param key TODO
     * @param keyValues
     * @throws DotDataException
     * @throws DotCacheException
     */
    public void add(final String key, final List<KeyValue> keyValues);

    public void addByLanguage(final String key, final long languageId, final List<KeyValue> keyValues);

    public void addByContentType(final String key, final String contentTypeId, final List<KeyValue> keyValues);

    public void addByLanguageAndContentType(final long languageId, final String contentTypeId, final KeyValue keyValue);

    /**
     * Retrieves the Key/Value associated to the given key
     * 
     * @param Key
     * @return KeyValue
     * @throws DotDataException
     */
    public List<KeyValue> get(final String key);

    public List<KeyValue> getByLanguage(final String key, final long languageId);

    public List<KeyValue> getByContentType(final String key, final String contentTypeId);

    public KeyValue getByLanguageAndContentType(final String key, final long languageId, final String contentTypeId);

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
    public void remove(final KeyValue keyValue);

    /**
     * 
     * @param key
     * @param languageId
     * @return
     */
    /*public String generateKeyByLanguage(final String key, final long languageId);

    public String generateKeyByContentType(final String key, final String contentTypeId);

    public String generateKeyByLanguageContentType(final String key, final long languageId, final String contentTypeId);*/

}
