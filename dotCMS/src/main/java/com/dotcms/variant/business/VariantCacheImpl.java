package com.dotcms.variant.business;

import com.dotcms.util.DotPreconditions;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

public class VariantCacheImpl implements VariantCache {

    static final String VARIANT_BY_ID = "VARIANT_BY_ID_";
    static final String VARIANT_BY_NAME = "VARIANT_BY_NAME_";

    @Override
    public void put(final Variant variant) {

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(VARIANT_BY_ID + variant.name(), variant, getPrimaryGroup());
        cache.put(VARIANT_BY_NAME + variant.name(), variant, getPrimaryGroup());

        putByName(variant);
    }

    @Override
    public Variant get(final String name) {
        DotPreconditions.checkNotNull(name);

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            return (Variant) cache.get(getKeyByName(name), getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    private String getKeyByName(String name) {
        return VARIANT_BY_NAME + name;
    }

    @Override
    public void remove(final Variant variant){
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.remove(getKeyById(variant.name()), getPrimaryGroup());
        cache.remove(getKeyByName(variant.name()), getPrimaryGroup());
    }

    @Override
    public String getPrimaryGroup() {
        return "VariantCacheImpl";
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void clearCache() {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.flushGroup(getPrimaryGroup());
    }

    private String getKeyById(String id) {
        return VARIANT_BY_ID + id;
    }

    private void putByName(final Variant variant) {
        put(variant.name(), variant);
    }

    @Override
    public void put(final String name, final Variant variant) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(getKeyById(name), variant, getPrimaryGroup());
        cache.put(getKeyByName(name), variant, getPrimaryGroup());
    }
}
