package com.dotmarketing.business.cache.provider.hazelcast;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

/**
 * Created by jasontesser on 3/14/17.
 */
public class HazelCastClientProvider extends CacheProvider {

    /**
   * 
   */
  private static final long serialVersionUID = 1L;
    protected HazelcastInstance hazel = null;
    protected Boolean initialized = false;

    @Override
    public void init() throws Exception {
        Logger.info(this, "Setting Up HazelCast Client Config");
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream("hazelcast-client.xml");
            XmlClientConfigBuilder builder = new XmlClientConfigBuilder(is);
            hazel = HazelcastClient.newHazelcastClient(builder.build());
            initialized = true;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Override
    public String getName() {
        return "HazelCast Client Provider";
    }

    @Override
    public String getKey() {
        return "HazelCastClientProvider";
    }

    @Override
    public boolean isInitialized() throws Exception {
        return initialized;
    }

    @Override
    public void put(String group, String key, Object content) {
        hazel.getMap(group).set(key, content);
    }

    @Override
    public Object get(String group, String key) {
        return hazel.getMap(group).get(key);
    }

    @Override
    public void remove(String group, String key) {
        hazel.getMap(group).remove(key);
    }

    @Override
    public void remove(String group) {
        hazel.getMap(group).clear();
    }

    @Override
    public void removeAll() {
        Collection<DistributedObject> distObjs = hazel.getDistributedObjects();
        for (DistributedObject distObj : distObjs) {
            if (distObj.getServiceName().contains("mapService")) {
                hazel.getMap(distObj.getName()).clear();
            }
        }
    }

    @Override
    public Set<String> getKeys(String group) {
        Set<String> keys = new HashSet<String>();
        for (Object key : hazel.getMap(group).keySet()) {
            keys.add(key.toString());
        }
        return keys;
    }

    @Override
    public Set<String> getGroups() {
        Set groups = new HashSet();

        Collection<DistributedObject> distObjs = hazel.getDistributedObjects();
        for (DistributedObject distObj : distObjs) {
            if (distObj.getServiceName().contains("mapService")) {
                groups.add(distObj.getName());
            }
        }
        return groups;
    }

    @Override
    public CacheProviderStats getStats() {
        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats,getName());
        Set<String> currentGroups = getGroups();

        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();

            stats.addStat("cache-region", group);
            stats.addStat("cache-local-memory-cost", hazel.getMap(group).getLocalMapStats().getOwnedEntryMemoryCost());
            stats.addStat("cache-local-heap-cost", hazel.getMap(group).getLocalMapStats().getHeapCost());
            stats.addStat("cache-requests", hazel.getMap(group).getLocalMapStats().getGetOperationCount());
            stats.addStat("cache-hits", hazel.getMap(group).getLocalMapStats().getHits());
            stats.addStat("cache-local-size", hazel.getMap(group).getLocalMapStats().getOwnedEntryCount());


            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {
        hazel.shutdown();
    }
}
