package com.dotcms.security.apps;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public abstract class AppsCache implements Cachable {

    static final String SECRETS_CACHE_GROUP = "SECRETS_CACHE_GROUP";
    static final String SECRETS_CACHE_KEYS_GROUP = "KEYS_CACHE_GROUP";
    static final String SECRETS_CACHE_KEY = "KEYS_CACHE";

    static final String DESCRIPTORS_CACHE_GROUP = "DESCRIPTORS_CACHE_GROUP";
    static final String DESCRIPTORS_LIST_KEY = "DESCRIPTORS_LIST_KEY";
    static final String DESCRIPTORS_MAPPED_BY_KEY = "DESCRIPTORS_MAPPED_BY_KEY";

    static final char[] CACHE_404 = new char[] {'4', '0', '4'};

    public abstract List<AppDescriptorMeta> getAppDescriptorsMeta(Supplier<List<AppDescriptorMeta>> supplier);

    public abstract Map<String, AppDescriptorMeta> getAppDescriptorsMap(final Supplier<List<AppDescriptorMeta>> supplier);

    public abstract void invalidateDescriptorsCache();

    public abstract Set<String> getKeysFromCache(final Supplier<Set<String>> supplier) throws DotCacheException;

    public abstract Set<String> getKeysFromCache() throws DotCacheException;

    public abstract char[] getFromCache(final String key, final Supplier<char[]> defaultValue);

    public abstract char[] getFromCache(final String key);

    public abstract void flushSecret();

    public abstract void flushSecret(final String key) throws DotCacheException ;

    public abstract void putSecret(final String key, final char[] chars);

    public abstract void putKeys(final Set<String> keys);

}
