package com.dotmarketing.business.cache.provider.hazelcast;

import com.dotcms.cluster.business.HazelcastUtil;
import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import java.util.*;


/**
 * Created by jasontesser on 3/14/17.
 */
public class HazelCastEmbeddedProvider extends CacheProvider {

  private static final long serialVersionUID = 1L;
    protected Boolean initialized = false;

    @Override
    public void init()  {
        Logger.debug(this,"Calling HazelUtil to ensure Hazelcast member is up");
        getHazelcastInstance();
        initialized = true;
    }

    @Override
    public String getName() {
        return "Hazelcast Embedded Provider";
    }

    @Override
    public String getKey() {
        return "HazelCastEmbeddedProvider";
    }

    @Override
    public boolean isInitialized() throws Exception {
        return initialized;
    }

    @Override
    public void put(String group, String key, Object content) {
        getHazelcastInstance().getMap(group).set(key, content);
    }

    @Override
    public Object get(String group, String key) {
        return getHazelcastInstance().getMap(group).get(key);
    }

    @Override
    public void remove(String group, String key) {
        getHazelcastInstance().getMap(group).remove(key);
    }

    @Override
    public void remove(String group) {
        getHazelcastInstance().getMap(group).clear();
    }

    @Override
    public void removeAll() {
        Collection<DistributedObject> distObjs = getHazelcastInstance().getDistributedObjects();
        for (DistributedObject distObj : distObjs) {
            if (distObj.getServiceName().contains("mapService")) {
                getHazelcastInstance().getMap(distObj.getName()).clear();
            }
        }
    }

    @Override
    public Set<String> getKeys(String group) {
        Set<String> keys = new HashSet<String>();
        for (Object key : getHazelcastInstance().getMap(group).keySet()) {
            keys.add(key.toString());
        }
        return keys;
    }

    @Override
    public Set<String> getGroups() {
        Set groups = new HashSet();

        Collection<DistributedObject> distObjs = getHazelcastInstance().getDistributedObjects();
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
            stats.addStat("cache-local-memory-cost", UtilMethods.prettyByteify(getHazelcastInstance().getMap(group).getLocalMapStats().getOwnedEntryMemoryCost()));
            stats.addStat("cache-local-heap-cost", getHazelcastInstance().getMap(group).getLocalMapStats().getHeapCost());
            stats.addStat("cache-requests", getHazelcastInstance().getMap(group).getLocalMapStats().getGetOperationCount());
            stats.addStat("cache-hits", getHazelcastInstance().getMap(group).getLocalMapStats().getHits());
            stats.addStat("cache-local-size", getHazelcastInstance().getMap(group).getLocalMapStats().getOwnedEntryCount());


            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {
        getHazelcastInstance().shutdown();
    }

    protected HazelcastInstance getHazelcastInstance() {
    	return new HazelcastUtil().getHazel(HazelcastInstanceType.EMBEDDED);
    }
}


