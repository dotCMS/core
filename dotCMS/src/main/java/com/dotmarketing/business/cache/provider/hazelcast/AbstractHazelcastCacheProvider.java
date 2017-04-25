package com.dotmarketing.business.cache.provider.hazelcast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.dotcms.cluster.business.HazelcastUtil;
import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.NearCacheStats;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by jasontesser on 3/14/17.
 */
public abstract class AbstractHazelcastCacheProvider extends CacheProvider {

	private static final long serialVersionUID = 1L;
    protected Boolean initialized = false;

    protected abstract HazelcastInstanceType getHazelcastInstanceType();

    protected HazelcastInstance getHazelcastInstance() {
    	return new HazelcastUtil().getHazel(getHazelcastInstanceType());
    }

    @Override
    public void init()  {
        Logger.debug(this,"Calling HazelUtil to ensure Hazelcast member is up");
        getHazelcastInstance();
        initialized = true;
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
        NumberFormat nf = DecimalFormat.getInstance();
        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();

            LocalMapStats local = getHazelcastInstance().getMap(group).getLocalMapStats();
            NearCacheStats near = local.getNearCacheStats();
            long size = getHazelcastInstance().getMap(group).keySet().size();
            long mem = local.getOwnedEntryMemoryCost();
            long perObject = (size==0) ? 0 : mem/size;
            String x = UtilMethods.prettyMemory(mem);
            long totalTime = local.getTotalGetLatency();
            long avgTime = (local.getGetOperationCount() ==0) ? 0  :  local.getTotalGetLatency() / local.getGetOperationCount() ;
            stats.addStat(CacheStats.REGION, group);
            stats.addStat(CacheStats.REGION_SIZE, nf.format(size));
            stats.addStat(CacheStats.REGION_MEM_PER_OBJECT, UtilMethods.prettyByteify(perObject ));
            stats.addStat(CacheStats.REGION_HITS, local.getHits());
            stats.addStat(CacheStats.REGION_AVG_LOAD_TIME, nf.format(avgTime/1000000) + " ms"); 
            stats.addStat(CacheStats.REGION_MEM_TOTAL_PRETTY, x);

            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {
    	getHazelcastInstance().shutdown();
    }
}
