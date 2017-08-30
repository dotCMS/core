package com.dotmarketing.business.cache.provider.hazelcast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.dotcms.cluster.business.HazelcastUtil;
import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

/**
 * Created by jasontesser on 3/14/17.
 */
public abstract class AbstractHazelcastCacheProvider extends CacheProvider {

	private static final long serialVersionUID = 1L;
    protected Boolean initialized = false;

    private final boolean ASYNC_PUT = Config.getBooleanProperty("HAZELCAST_ASYNC_PUT", true);
    protected abstract HazelcastInstanceType getHazelcastInstanceType();

    protected abstract CacheStats getStats(String group);


    protected HazelcastInstance getHazelcastInstance() {
    	return HazelcastUtil.getInstance().getHazel(getHazelcastInstanceType());
    }

    @Override
    public boolean isDistributed() {
    	return true;
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
        if(ASYNC_PUT){
            getHazelcastInstance().getMap(group).setAsync(key, content);
        }else{
            getHazelcastInstance().getMap(group).set(key, content);
        }
    }

    @Override
    public Object get(String group, String key) {
        return getHazelcastInstance().getMap(group).get(key);
    }

    @Override
    public void remove(String group, String key) {
        if(ASYNC_PUT){
            getHazelcastInstance().getMap(group).removeAsync(key);
        }
        else{
            getHazelcastInstance().getMap(group).remove(key);
        }
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

        for (String group : getGroups()) {

            ret.addStatRecord(getStats(group));
        }

        return ret;
    }

    @Override
    public void shutdown() {
    	getHazelcastInstance().shutdown();
    }
}
