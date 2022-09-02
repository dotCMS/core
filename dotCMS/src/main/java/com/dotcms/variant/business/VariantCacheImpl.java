package com.dotcms.variant.business;

import com.dotcms.util.DotPreconditions;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

public class VariantCacheImpl implements VariantCache{

    static final String VARIANT_BY_ID = "VARIANT_BY_ID_";
    static final String VARIANT_BY_NAME = "VARIANT_BY_NAME_";

    @Override
    public void put(final Variant variant) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(VARIANT_BY_ID + variant.identifier(), variant, getPrimaryGroup());
        cache.put(VARIANT_BY_NAME + variant.name(), variant, getPrimaryGroup());
    }

    @Override
    public Variant getById(final String id) {

        DotPreconditions.checkNotNull(id);

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            return (Variant) cache.get(VARIANT_BY_ID + id, getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    @Override
    public Variant getByName(final String name) {
        DotPreconditions.checkNotNull(name);

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            return (Variant) cache.get(VARIANT_BY_NAME + name, getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    @Override
    public void remove(final Variant variant){
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.remove(VARIANT_BY_ID + variant.identifier(), getPrimaryGroup());
        cache.remove(VARIANT_BY_NAME + variant.name(), getPrimaryGroup());
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
}
