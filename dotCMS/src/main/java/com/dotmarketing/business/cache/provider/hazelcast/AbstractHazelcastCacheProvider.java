package com.dotmarketing.business.cache.provider.hazelcast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.dotcms.cluster.business.HazelcastUtil;
import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;

/**
 * Created by jasontesser on 3/14/17.
 */
public abstract class AbstractHazelcastCacheProvider extends CacheProvider {

	private static final long serialVersionUID = 1L;
    private boolean initialized = false;
    private boolean recovering =false;

    private final boolean ASYNC_PUT = Config.getBooleanProperty("HAZELCAST_ASYNC_PUT", true);

    protected abstract HazelcastInstanceType getHazelcastInstanceType();
    protected abstract CacheStats getStats(String group);

    protected HazelcastInstance getHazelcastInstance() {
    	return HazelcastUtil.getInstance().getHazel(getHazelcastInstanceType());
    }

    protected void reInitialize(){
        if(recovering){
            return;
        }
        synchronized (this) {
            if(!recovering){
                setRecovering(true);
                Runnable hazelThread = new ReinitializeHazelThread(this, getHazelcastInstanceType());
                hazelThread.run();
            }
        }
    }

    @Override
    public boolean isDistributed() {
    	return true;
    }

    @Override
    public void init()  {
    	if(!LicenseManager.getInstance().isEnterprise()){
    		return;
    	}
        try {Thread.sleep(1000);}catch (Exception e){}
        if(isRecovering()){
            return;
        }
        Logger.debug(this,"Calling HazelUtil to ensure Hazelcast member is up");
        getHazelcastInstance();
        setInitialized(true);
    }

    public boolean isRecovering() {
        return recovering;
    }

    public void setRecovering(boolean recovering){
        this.recovering = recovering;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized){
        this.initialized=initialized;
    }

    @Override
    public void put(String group, String key, Object content) {
        if(isRecovering()){
            return;
        }
        try{
            if(ASYNC_PUT){
                getHazelcastInstance().getMap(group).setAsync(key, content);
            }else{
                getHazelcastInstance().getMap(group).set(key, content);
            }
        } catch (HazelcastInstanceNotActiveException hce){
            reInitialize();
        }
    }

    @Override
    public Object get(String group, String key) {
        if(isRecovering()){
            return null;
        }
        try {
            return getHazelcastInstance().getMap(group).get(key);
        } catch (HazelcastInstanceNotActiveException hce){
            reInitialize();
            return null;
        }
    }

    @Override
    public void remove(String group, String key) {
        if(isRecovering()){
            return;
        }
        try{
            if(ASYNC_PUT){
                getHazelcastInstance().getMap(group).removeAsync(key);
            } else{
                getHazelcastInstance().getMap(group).remove(key);
            }
        } catch (HazelcastInstanceNotActiveException hce){
            reInitialize();
        }
    }

    @Override
    public void remove(String group) {
        if(isRecovering()){
            return;
        }
        try{
            getHazelcastInstance().getMap(group).clear();
        } catch (HazelcastInstanceNotActiveException hce){
            reInitialize();
        }
    }

    @Override
    public void removeAll() {
        if(isRecovering()){
            return;
        }
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
        if(isRecovering()){
            return keys;
        }
        for (Object key : getHazelcastInstance().getMap(group).keySet()) {
            keys.add(key.toString());
        }
        return keys;
    }

    @Override
    public Set<String> getGroups() {
        Set groups = new HashSet();
        if(isRecovering()){
            return groups;
        }
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
        Set groups = new HashSet();
        if(isRecovering()){
            return ret;
        }
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
