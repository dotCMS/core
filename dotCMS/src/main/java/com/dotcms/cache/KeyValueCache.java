package com.dotcms.cache;

import java.util.List;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * This cache is used to map Key/Value contents in dotCMS. This cache is used to map a property key
 * to its associated value, which is specially useful for contents such as Language Variables.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 */
public interface KeyValueCache extends Cachable {

    /**
     * Adds/updates the list of {@link KeyValue} objects associated to a given key to the Key/Value
     * cache.
     * 
     * @param key - The key.
     * @param keyValues - The list of {@link KeyValue} contents that match the specified key.
     */
    public void add(final String key, final List<KeyValue> keyValues);

    /**
     * Adds/updates the list of {@link KeyValue} contents associated to a given key and language ID
     * to the Key/Value cache.
     * 
     * @param key - The key.
     * @param languageId - The ID of the language that the Key/Value contents were created for.
     * @param keyValues - The list of {@link KeyValue} contents that match the specified key and
     *        language ID.
     */
    public void addByLanguage(final String key, final long languageId, final List<KeyValue> keyValues);

    /**
     * Adds/updates the list of {@link KeyValue} contents associated to a given key and Content Type
     * ID to the Key/Value cache.
     * 
     * @param key - The key.
     * @param contentTypeId - The ID of the Content Type that the Key/Value contents belong to.
     * @param keyValues - The list of {@link KeyValue} contents that match the specified key and
     *        Content Type ID.
     */
    public void addByContentType(final String key, final String contentTypeId, final List<KeyValue> keyValues);

    /**
     * Adds/updates the {@link KeyValue} content associated to a given language ID and Content Type
     * ID to the Key/Value cache.
     * 
     * @param languageId - The ID of the language that the Key/Value object was created for.
     * @param contentTypeId - The ID of the Content Type that the Key/Value content belongs to.
     * @param keyValue - The {@link KeyValue} content.
     */
    public void addByLanguageAndContentType(final long languageId, final String contentTypeId, final KeyValue keyValue);

    /**
     * Adds/updates the {@link KeyValue} content associated to a given language ID, Content Type
     * ID and live or working mode to the Key/Value cache.
     *
     * @param languageId - The ID of the language that the Key/Value object was created for.
     * @param contentTypeId - The ID of the Content Type that the Key/Value content belongs to.
     * @param live true if is live mode, false if it is working mode
     * @param keyValue - The {@link KeyValue} content.
     */
    void add(final long languageId,
                final String contentTypeId,
                final boolean live,
                final KeyValue keyValue);
    /**
     * Retrieves the list of {@link KeyValue} contents associated to the specified key.
     * 
     * @param key - The key.
     * @return The list of all the Key/Value contents matching the same key.
     */
    public List<KeyValue> get(final String key);

    /**
     * Retrieves the list of {@link KeyValue} contents associated to the specified key and language
     * ID.
     * 
     * @param key - The key.
     * @param languageId - The ID of the language that the Key/Value contents were created for.
     * @return The list of all the Key/Value contents matching the same key and language ID.
     */
    public List<KeyValue> getByLanguage(final String key, final long languageId);

    /**
     * Retrieves the list of {@link KeyValue} contents associated to the specified key and Content
     * Type ID.
     * 
     * @param key - The key.
     * @param contentTypeId - The ID of the Content Type that the Key/Value contents belong to.
     * @return The list of all the Key/Value contents matching the same key and Content Type ID.
     */
    public List<KeyValue> getByContentType(final String key, final String contentTypeId);

    /**
     * Retrieves the list of {@link KeyValue} contents associated to the specified key, language ID,
     * and Content Type ID.
     * 
     * @param key - The key.
     * @param languageId - The ID of the language that the Key/Value object was created for.
     * @param contentTypeId - The ID of the Content Type that the Key/Value content belongs to.
     * @return The list of all the Key/Value contents matching the same key, language ID, and
     *         Content Type ID.
     */
    public KeyValue getByLanguageAndContentType(final String key, final long languageId, final String contentTypeId);

    /**
     * Retrieves the list of {@link KeyValue} contents associated to the specified key, language ID,
     * and Content Type ID.
     *
     * @param key - The key.
     * @param languageId - The ID of the language that the Key/Value object was created for.
     * @param contentTypeId - The ID of the Content Type that the Key/Value content belongs to.
     * @return The list of all the Key/Value contents matching the same key, language ID, and
     *         Content Type ID.
     * @param live true if is live mode, false if it is working mode
     * @return
     */
    KeyValue get(
        final String key,
        final long languageId,
        final String contentTypeId,
        final boolean  live);

    /**
     * Removes all entries from every group of this cache structure.
     */
    public void clearCache();

    /**
     * Removes the specified {@link KeyValue} content from the cache.
     * 
     * @param keyValue - The Key/Value content to remove.
     */
    public void remove(final KeyValue keyValue);

    /**
     * Removes the specified {@link KeyValue} ID from the cache.
     * 
     * @param keyValueId - The Key/Value ID to remove.
     */
    public void remove(final String keyValueId);

    /**
     * Removes the specified {@link Contentlet} content from the cache.
     * Contentlet must have a key field.
     *
     * @param contentlet - The Key/Value content to remove.
     */
    void remove(Contentlet contentlet);

    void add404ByLanguageAndContentType(long languageId, String contentTypeId, String key);
}
