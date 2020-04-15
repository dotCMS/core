package com.dotcms.security.apps;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Cache Admin allows management of Apps Cache.
 * Here we have The yml App Descriptors Cache as well as the secret storage.
 * The class is just a entry point to the cache administrator.
 * You can store secrets organized by key and also keep a list of all the keys that currently exist in the secret store.
 */
public class AppsCacheImpl extends AppsCache {

    private final DotCacheAdministrator cache;

    /**
     * Default constructor.
     */
    public AppsCacheImpl() {
        super();
        cache = CacheLocator.getCacheAdministrator();
    }

    /**
     * Mandatory Primary group getter
     * @return SECRETS_CACHE_GROUP
     */
    @Override
    public String getPrimaryGroup() {
        return SECRETS_CACHE_GROUP;
    }

    /**
     * Groups listing
     * @return groups String array
     */
    @Override
    public String[] getGroups() {
        return new String[]{SECRETS_CACHE_GROUP, SECRETS_CACHE_KEYS_GROUP, DESCRIPTORS_CACHE_GROUP};
    }

    /**
     * Mandatory clear cache implementation
     */
    @Override
    public void clearCache() {
        for (final String group : getGroups()) {
            cache.flushGroup(group);
        }
    }

    /**
     * This is the base cache method for AppDescriptors tracking
     * It takes a supplier that will be called in case no app descriptor is found in cache.
     * @param supplier Supplier delegated expected to load AppDescriptors
     * @return
     */
    public List<AppDescriptorMeta> getAppDescriptorsMeta( final Supplier<List<AppDescriptorMeta>> supplier){
        List<AppDescriptorMeta> appDescriptors = (List<AppDescriptorMeta>) cache.getNoThrow(DESCRIPTORS_LIST_KEY, DESCRIPTORS_CACHE_GROUP);
        if (!UtilMethods.isSet(appDescriptors) && null != supplier) {
           appDescriptors = supplier.get();
        }
        putAppDescriptor(appDescriptors);
        return appDescriptors;
    }

    /**
     * This will simply put a list of descriptor into memory.
     * @param appDescriptors
     */
    private void putAppDescriptor(final List<AppDescriptorMeta> appDescriptors) {
        cache.put(DESCRIPTORS_LIST_KEY, appDescriptors, DESCRIPTORS_CACHE_GROUP);
    }

    /**
     * Given a supplier delegate able to retrieve AppDescriptors this method will use it to populate cache.
     * Once passed that point the method takes whatever descriptors exist in cache and use the returned entries
     * to build Map like structure still in cache to track AppDescriptors by app-key.
     * If the supplier is null the function will get back the contents from cache and no population attempt will be performed.
     * @param supplier
     * @return
     */
    public Map<String, AppDescriptorMeta> getAppDescriptorsMap(final Supplier<List<AppDescriptorMeta>> supplier) {
        Map<String, AppDescriptorMeta> descriptorsByKey = (Map<String, AppDescriptorMeta>) cache.getNoThrow(
                DESCRIPTORS_MAPPED_BY_KEY, DESCRIPTORS_CACHE_GROUP);
        if (!UtilMethods.isSet(descriptorsByKey) && null != supplier) {
            synchronized (AppsCacheImpl.class) {
                descriptorsByKey = getAppDescriptorsMeta(supplier).stream().collect(
                        Collectors.toMap(serviceDescriptorMeta -> serviceDescriptorMeta
                                        .getAppDescriptor().getKey().toLowerCase(), Function.identity(),
                                (serviceDescriptor, serviceDescriptor2) -> serviceDescriptor));

                putDescriptorsByKey(descriptorsByKey);
            }
        }
        return descriptorsByKey;
    }

    /**
     * This simply allow putting in cache
     * A Map like structure, Where the key is the app-key and the entry is the AppDescriptor.
     * @param descriptorsByKey
     */
    private void putDescriptorsByKey(final Map<String, AppDescriptorMeta> descriptorsByKey){
        cache.put(DESCRIPTORS_MAPPED_BY_KEY, descriptorsByKey, DESCRIPTORS_CACHE_GROUP);
    }

    /**
     * Remove all descriptors from cache;
     */
    public void invalidateDescriptorsCache(){
        cache.flushGroup(DESCRIPTORS_CACHE_GROUP);
    }

    /**
     * In a similar fashion this takes a supplier delegate responsible for getting the secret keys to populate cache.
     * If the supplier is null the function will get back the contents from cache and no population attempt will be performed.
     * @param supplier string keys set delegate
     * @return
     * @throws DotCacheException
     */
    public Set<String> getKeysFromCache(final Supplier<Set<String>> supplier)
            throws DotCacheException {
        synchronized (AppsCacheImpl.class) {
            Set<String> keys = (Set<String>) cache.get(SECRETS_CACHE_KEY, SECRETS_CACHE_KEYS_GROUP);
            if (!UtilMethods.isSet(keys) && null != supplier) {
                keys = supplier.get();
                putKeys(keys);
            }
            return keys;
        }
    }

    /**
     * In this version of the method the supplier is null
     * so the function will get back the contents from cache and no population attempt will be performed.
     * @return
     * @throws DotCacheException
     */
    public Set<String> getKeysFromCache() throws DotCacheException {
       return getKeysFromCache(null);
    }

    /**
     * Given a supplier delegate able to retrieve an encrypted secret this method will use it to populate cache.
     * @param key Key
     * @param supplier encrypted secret supplier
     * @return encrypted secret from cache.
     */
    public char[] getFromCache(final String key, final Supplier<char[]> supplier) {
        synchronized (AppsCacheImpl.class) {
            char[] retVal = (char[]) cache.getNoThrow(key, SECRETS_CACHE_GROUP);
            if (retVal == null && null != supplier) {
                retVal = supplier.get();
                putSecret(key, retVal);
            }
            return retVal;
        }
    }

    /**
     * In this version of the method the supplier is null
     * so the function will get back the secrets from cache and no population attempt will be performed.
     * @param key secret key
     * @return encrypted secret from cache.
     */
    public char[] getFromCache(final String key){
       return getFromCache(key, null);
    }

    /**
     * Simply Puts a secret in cache.
     * @param key
     * @param chars
     */
    public void putSecret(final String key, final char[] chars){
        cache.put(key, chars, SECRETS_CACHE_GROUP);
    }

    /**
     * All Secrets flush.
     */
    public  void flushSecret() {
        synchronized (AppsCacheImpl.class) {
           cache.flushGroup(SECRETS_CACHE_GROUP);
           cache.flushGroup(SECRETS_CACHE_KEYS_GROUP);
        }
    }

    /**
     * Single flush by key.
     * @param key
     * @throws DotCacheException
     */
    public void flushSecret(final String key) throws DotCacheException {
        synchronized (AppsCacheImpl.class) {
            cache.remove(key, SECRETS_CACHE_GROUP);
            final Set<String> keys = (Set<String>)cache.get(SECRETS_CACHE_KEY, SECRETS_CACHE_KEYS_GROUP);
            if(UtilMethods.isSet(keys)){
                keys.remove(key);
                putKeys(keys);
            }
        }
    }

    /**
     * Simply put or replace the set of keys
     * @param keys
     */
    public void putKeys(final Set<String> keys){
        cache.put(SECRETS_CACHE_KEY, keys, SECRETS_CACHE_KEYS_GROUP);
    }

}
