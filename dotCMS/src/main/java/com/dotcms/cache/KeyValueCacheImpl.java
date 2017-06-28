package com.dotcms.cache;

import java.util.List;
import java.util.Map;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

/**
 * Implementation class for the {@link KeyValueCache}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public class KeyValueCacheImpl implements KeyValueCache {

    private DotCacheAdministrator cache;

    private static final String primaryGroup = "KeyValueCache";
    private static final String byLanguageGroup = "KeyValueCacheByLanguage";
    private static final String byContentTypeGroup = "KeyValueCacheByContentType";
    private static final String byLanguageContentTypeGroup = "KeyValueCacheByLanguageContentType";
    private static final String[] groupNames = {primaryGroup, byLanguageGroup, byLanguageContentTypeGroup};

    /**
     * Creates a new instance of the {@link KeyValueCache}.
     */
    public KeyValueCacheImpl() {
        this.cache = CacheLocator.getCacheAdministrator();
    }

    /*
     * grupo 1 = key-lang 
     * grupo 2 = key-lang-conttype 
     * grupo 3 = key-contetype
     */

    @Override
    public void add(final String key, final List<KeyValue> keyValues) {
        this.cache.put(key, keyValues, primaryGroup);
    }

    @Override
    public void addByLanguage(final String key, final long languageId, final List<KeyValue> keyValues) {
        /*
         * List<KeyValue> cachedValues = getByLanguage(key, languageId); Map<Long, List<KeyValue>>
         * data; if (null == cachedValues) { data = CollectionsUtils.map(languageId, keyValues); }
         * else { cachedValues.addAll(keyValues); data = CollectionsUtils.map(languageId,
         * cachedValues); }
         */
        Map<Long, List<KeyValue>> data = CollectionsUtils.map(languageId, keyValues);
        this.cache.put(key, data, byLanguageGroup);
    }

    @Override
    public void addByContentType(final String key, final String contentTypeId, final List<KeyValue> keyValues) {
        Map<String, List<KeyValue>> data = CollectionsUtils.map(contentTypeId, keyValues);
        this.cache.put(key, data, byContentTypeGroup);
    }

    @Override
    public void addByLanguageAndContentType(final long languageId, final String contentTypeId, final KeyValue keyValue) {
        /*
         * final String key = generateKeyByLanguageContentType(keyValue.getKey(),
         * keyValue.getLanguageId(), keyValue.getType()); this.cache.put(key, keyValue,
         * byLanguageContentTypeGroup);
         */
        Map<Long, Map<String, KeyValue>> data = CollectionsUtils.map();
        data.put(languageId, CollectionsUtils.map(contentTypeId, keyValue));
        this.cache.put(keyValue.getKey(), data, byLanguageContentTypeGroup);
    }

    @Override
    public List<KeyValue> get(final String key) {
        try {
            @SuppressWarnings("unchecked")
            List<KeyValue> keyValues = List.class.cast(this.cache.get(key, primaryGroup));
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
            Map<Long, List<KeyValue>> cachedValues = Map.class.cast(this.cache.get(key, byLanguageGroup));
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
            Map<String, List<KeyValue>> cachedValues = Map.class.cast(this.cache.get(key, byContentTypeGroup));
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
        try {
            @SuppressWarnings("unchecked")
            Map<Long, Map<String, KeyValue>> cachedValues = Map.class.cast(this.cache.get(key, byLanguageContentTypeGroup));
            if (null != cachedValues) {
                return cachedValues.get(languageId).get(contentTypeId);
            }
        } catch (DotCacheException e) {
            Logger.debug(this, String.format("Cache entry with key %s was not found.", key), e);
        }
        return null;
    }

    @Override
    public void clearCache() {
        this.cache.flushGroup(primaryGroup);
        this.cache.flushGroup(byLanguageGroup);
        this.cache.flushGroup(byContentTypeGroup);
        this.cache.flushGroup(byLanguageContentTypeGroup);
    }

    @Override
    public void remove(final KeyValue keyValue) {
        try {
            this.cache.remove(keyValue.getKey(), primaryGroup);
            this.cache.remove(keyValue.getKey(), byLanguageGroup);
            this.cache.remove(keyValue.getKey(), byContentTypeGroup);
            this.cache.remove(keyValue.getKey(), byLanguageContentTypeGroup);
        } catch (Exception e) {
            Logger.debug(this, String.format("Cache entry with key %s could not be removed.", keyValue.getKey()), e);
        }
    }

    @Override
    public String[] getGroups() {
        return groupNames;
    }

    @Override
    public String getPrimaryGroup() {
        return primaryGroup;
    }

    /*@Override
    public String generateKeyByLanguage(String key, long languageId) {
        return generateKeyByLanguageContentType(key, languageId, StringPool.BLANK);
    }

    @Override
    public String generateKeyByContentType(final String key, final String contentTypeId) {
        return key + "|" + contentTypeId;
    }

    @Override
    public String generateKeyByLanguageContentType(String key, long languageId, String contentTypeId) {
        return key + "|" + languageId + "|" + contentTypeId;
    }*/

}
