package com.dotcms.cache;

import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.keyvalue.business.KeyValue404;
import com.dotcms.keyvalue.model.KeyValue;
import static com.dotcms.util.CollectionsUtils.*;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

/**
 * Implementation class for the {@link KeyValueCache}. This cache contains 4 groups:
 * <ol>
 * <li>{@code PRIMARY_GROUP}: Holds the list of Key/Value contents that match a given key.</li>
 * <li>{@code BY_LANGUAGE_GROUP}: Holds the list of Key/Value contents that match a given key and
 * language ID.</li>
 * <li>{@code BY_CONTENT_TYPE_GROUP}: Holds the list of Key/Value contents that match a given key and
 * Content Type ID.</li>
 * <li>{@code BY_LANGUAGE_CONTENT_TYPE_GROUP}: Holds the single Key/Value content that match a given
 * key, language ID, and Content Type ID.</li>
 * </ol>
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public class KeyValueCacheImpl implements KeyValueCache {

    private final DotCacheAdministrator cache;

    private static final String PRIMARY_GROUP = "KeyValueCache";
    private static final String BY_LANGUAGE_GROUP = "KeyValueCacheByLanguage";
    private static final String BY_CONTENT_TYPE_GROUP = "KeyValueCacheByContentType";
    private static final String BY_LANGUAGE_CONTENT_TYPE_GROUP = "KeyValueCacheByLanguageContentType";
    private static final String BY_LANGUAGE_CONTENT_TYPE_LIVE_GROUP = "KeyValueCacheByLanguageContentTypeLive";
    private static final String[] GROUP_NAMES = {PRIMARY_GROUP, BY_LANGUAGE_GROUP, BY_LANGUAGE_CONTENT_TYPE_GROUP};
    private final static KeyValue KEY_VALUE_404=new KeyValue404();
    /**
     * Creates a new instance of the {@link KeyValueCache}.
     */
    public KeyValueCacheImpl() {
        this.cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public void add(final String key, final List<KeyValue> keyValues) {
        this.cache.put(key, keyValues, PRIMARY_GROUP);
    }

    @Override
    public void addByLanguage(final String key, final long languageId, final List<KeyValue> keyValues) {

        final Map<Long, List<KeyValue>> data = imap(languageId, keyValues);
        this.cache.put(key, data, BY_LANGUAGE_GROUP);
    }

    @Override
    public void addByContentType(final String key, final String contentTypeId, final List<KeyValue> keyValues) {
        final Map<String, List<KeyValue>> data = imap(contentTypeId, keyValues);
        this.cache.put(key, data, BY_CONTENT_TYPE_GROUP);
    }

    @Override
    public void addByLanguageAndContentType(final long languageId, final String contentTypeId, final KeyValue keyValue) {
        final Map<Long, Map<String, KeyValue>> data =
                imap(languageId, imap(contentTypeId, keyValue));
        this.cache.put(keyValue.getKey(), data, BY_LANGUAGE_CONTENT_TYPE_GROUP);
    }

    @Override
    public void add(final long languageId,
                    final String contentTypeId,
                    final boolean live,
                    final KeyValue keyValue) {
        final Map<Long, Map<String, Map<String, KeyValue>>> data =
                imap(languageId, imap(contentTypeId, imap(live, keyValue)));
        this.cache.put(keyValue.getKey(), data, BY_LANGUAGE_CONTENT_TYPE_LIVE_GROUP);
    }

    @Override
    public void add404ByLanguageAndContentType(final long languageId, final String contentTypeId,  final String key) {
        final Map<Long, Map<String, KeyValue>> data =
                imap(languageId, imap(contentTypeId, KEY_VALUE_404));
        this.cache.put(key, data, BY_LANGUAGE_CONTENT_TYPE_GROUP);
    }

    @Override
    public List<KeyValue> get(final String key) {
        try {
            @SuppressWarnings("unchecked")
            List<KeyValue> keyValues = List.class.cast(this.cache.get(key, PRIMARY_GROUP));
            return keyValues;
        } catch (DotCacheException e) {
            Logger.debug(this, String.format("Cache entry with key %s was not found.", key), e);
        }
        return null;
    }

    @Override
    public List<KeyValue> getByLanguage(final String key, final long languageId) {
        try {
            @SuppressWarnings("unchecked")
            Map<Long, List<KeyValue>> cachedValues = Map.class.cast(this.cache.get(key, BY_LANGUAGE_GROUP));
            if (null != cachedValues) {
                return cachedValues.get(languageId);
            }
        } catch (DotCacheException e) {
            Logger.debug(this, String.format("Cache entry with key %s was not found.", key), e);
        }
        return null;
    }

    @Override
    public List<KeyValue> getByContentType(final String key, final String contentTypeId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, List<KeyValue>> cachedValues = Map.class.cast(this.cache.get(key, BY_CONTENT_TYPE_GROUP));
            if (null != cachedValues) {
                return cachedValues.get(contentTypeId);
            }
        } catch (DotCacheException e) {
            Logger.debug(this, String.format("Cache entry with key %s was not found.", key), e);
        }
        return null;
    }

    @Override
    public KeyValue getByLanguageAndContentType(final String key, final long languageId, final String contentTypeId) {

        KeyValue keyValue = null;

        try {

            @SuppressWarnings("unchecked")
            final Map<Long, Map<String, KeyValue>> cachedValues = Map.class.cast(this.cache.get(key, BY_LANGUAGE_CONTENT_TYPE_GROUP));
            keyValue = (null != cachedValues && cachedValues.containsKey(languageId))?
                            cachedValues.get(languageId).get(contentTypeId):null;
        } catch (DotCacheException e) {
            Logger.debug(this, String.format("Cache entry with key %s was not found.", key), e);
        }

        return keyValue;
    }

    @Override
    public KeyValue get(
            final String key,
            final long languageId,
            final String contentTypeId,
            final boolean  live) {

        KeyValue keyValue = null;

       try {

            @SuppressWarnings("unchecked")
            final Map<Long, Map<String, Map<String, KeyValue>>> cachedValues = Map.class.cast(this.cache.get(key, BY_LANGUAGE_CONTENT_TYPE_LIVE_GROUP));
            keyValue = (null != cachedValues && cachedValues.containsKey(languageId))?
                    cachedValues.get(languageId).get(contentTypeId) != null
                            ? cachedValues.get(languageId).get(contentTypeId).get(live):null
                    :null;
        } catch (DotCacheException e) {
            Logger.debug(this, String.format("Cache entry with key %s was not found.", key), e);
        }

        return keyValue;
    }

    @Override
    public void clearCache() {
        this.cache.flushGroup(PRIMARY_GROUP);
        this.cache.flushGroup(BY_LANGUAGE_GROUP);
        this.cache.flushGroup(BY_CONTENT_TYPE_GROUP);
        this.cache.flushGroup(BY_LANGUAGE_CONTENT_TYPE_GROUP);
        this.cache.flushGroup(BY_LANGUAGE_CONTENT_TYPE_LIVE_GROUP);
    }

    @Override
    public void remove(final KeyValue keyValue) {
        remove(keyValue.getKey());
    }

    @Override
    public void remove(final Contentlet contentlet) {
        remove(contentlet.getStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR));
    }

    @Override
    public void remove(final String keyValueId) {
        try {
            this.cache.remove(keyValueId, PRIMARY_GROUP);
            this.cache.remove(keyValueId, BY_LANGUAGE_GROUP);
            this.cache.remove(keyValueId, BY_CONTENT_TYPE_GROUP);
            this.cache.remove(keyValueId, BY_LANGUAGE_CONTENT_TYPE_GROUP);
            this.cache.remove(keyValueId, BY_LANGUAGE_CONTENT_TYPE_LIVE_GROUP);
        } catch (Exception e) {
            Logger.debug(this, String.format("Cache entry with ID %s could not be removed.", keyValueId), e);
        }
    }

    @Override
    public String[] getGroups() {
        return GROUP_NAMES;
    }

    @Override
    public String getPrimaryGroup() {
        return PRIMARY_GROUP;
    }

}
