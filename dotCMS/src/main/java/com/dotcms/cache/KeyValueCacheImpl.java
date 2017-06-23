package com.dotcms.cache;

import com.dotcms.content.model.KeyValue;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

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

    private String primaryGroup = "KeyValueCache";
    private String[] groupNames = {primaryGroup};

    /**
     * Creates a new instance of the {@link KeyValueCache}.
     */
    public KeyValueCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public KeyValue add(String key, KeyValue keyValue) {
        // Add the key to the cache
        this.cache.put(key, keyValue, this.primaryGroup);
        return keyValue;
    }

    @Override
    public KeyValue addByContentType(String key, KeyValue keyValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public KeyValue addByContentTypeAndLanguage(String key, KeyValue keyValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public KeyValue get(String key) {
        KeyValue vanityUrl = null;
        try {
            vanityUrl = (KeyValue) this.cache.get(key, this.primaryGroup);
        } catch (DotCacheException e) {
            Logger.debug(this, "Cache Entry not found", e);
        }
        return vanityUrl;
    }

    @Override
    public KeyValue getByContentType(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public KeyValue getByContentTypeAndLanguage(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clearCache() {
        // clear the cache
        this.cache.flushGroup(this.primaryGroup);
    }

    @Override
    public void remove(String key) {
        try {
            this.cache.remove(key, this.primaryGroup);
        } catch (Exception e) {
            Logger.debug(this, "Cache not able to be removed", e);
        }
    }

    @Override
    public String[] getGroups() {
        return this.groupNames;
    }

    @Override
    public String getPrimaryGroup() {
        return this.primaryGroup;
    }

    @Override
    public String generateCacheKey(KeyValue keyValue) {
        return generateCacheKey(keyValue.getKey(), keyValue.getLanguageId());
    }

    @Override
    public String generateCacheKey(String key, long languageId) {
        return key + "|lang_" + languageId;
    }

}
