package com.dotmarketing.business.cache.provider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.cache.transport.CacheTransport;

public class MockCacheAdministrator implements DotCacheAdministrator {

    Map<String, Map<String, Object>> mockCache = new ConcurrentHashMap<>();



    @Override
    public void initProviders() {


    }

    @Override
    public Set<String> getGroups() {
        return mockCache.keySet();
    }

    @Override
    public void flushAll() {
        mockCache.clear();

    }

    @Override
    public void flushGroup(String group) {
        mockCache.getOrDefault(group, new ConcurrentHashMap<>()).clear();

    }

    @Override
    public void flushAlLocalOnly(boolean ignoreDistributed) {
        mockCache.clear();

    }

    @Override
    public void flushGroupLocalOnly(String group, boolean ignoreDistributed) {
        mockCache.getOrDefault(group, new ConcurrentHashMap<>()).clear();

    }

    @Override
    public Object get(String key, String group) throws DotCacheException {

        return mockCache.getOrDefault(group, new ConcurrentHashMap<>()).get(key);
    }

    @Override
    public void put(String key, Object content, String group) {
        mockCache.getOrDefault(group, new ConcurrentHashMap<>()).put(key, content);

    }

    @Override
    public void remove(String key, String group) {
        mockCache.getOrDefault(group, new ConcurrentHashMap<>()).remove(key);

    }

    @Override
    public void removeLocalOnly(String key, String group, boolean ignoreDistributed) {
        mockCache.getOrDefault(group, new ConcurrentHashMap<>()).remove(key);

    }

    @Override
    public void shutdown() {
        mockCache.clear();

    }

    @Override
    public List<CacheProviderStats> getCacheStatsList() {
        return List.of();

    }

    @Override
    public Class getImplementationClass() {

        return this.getClass();
    }

    @Override
    public DotCacheAdministrator getImplementationObject() {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public void invalidateCacheMesageFromCluster(String message) {


    }

    @Override
    public CacheTransport getTransport() {
        // TODO Auto-generated method stub
        return null;
    }

}
