package com.dotmarketing.business.cache.provider.hazelcast;

import com.dotcms.repackage.com.google.common.cache.CacheStats;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Logger;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import java.util.*;

/**
 * Created by jasontesser on 3/14/17.
 */
public class HazelCastProvider extends CacheProvider{

    HazelcastInstance client = null;
    private Boolean isInitialized = false;

    @Override
    public void init() throws Exception {
        Logger.info(this,"Setting Up HazelCast Config");
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setGroupConfig(new GroupConfig("dev","dev-pass"));
        ClientNetworkConfig netConfig = clientConfig.getNetworkConfig();
        netConfig.addAddress("10.0.1.36:5701");
        client = HazelcastClient.newHazelcastClient( clientConfig );
        isInitialized=true;
        }

        @Override
        public String getName() {
            return "HazelCast Provider";
        }

        @Override
        public String getKey() {
            return "HazelCastProvider";
        }

        @Override
        public boolean isInitialized() throws Exception {
            return isInitialized;
        }

        @Override
        public void put(String group, String key, Object content) {
            client.getMap(group).set(key,content);
        }

        @Override
        public Object get(String group, String key) {
            return client.getMap(group).get(key);
        }

        @Override
        public void remove(String group, String key) {
            client.getMap(group).remove(key);
        }

        @Override
        public void remove(String group) {
            client.getMap(group).clear();
        }

        @Override
        public void removeAll() {
            Collection<DistributedObject> distObjs =  client.getDistributedObjects();
            for(DistributedObject distObj: distObjs) {
                if (distObj.getServiceName().contains("mapService")) {
                    client.getMap(distObj.getName()).clear();
                }
            }
        }

        @Override
        public Set<String> getKeys(String group) {
            Set<String> keys = new HashSet<String>();
            for (Object key :client.getMap(group).keySet()){
                keys.add(key.toString());
            }
            return keys;
        }

        @Override
        public Set<String> getGroups() {
            Set groups = new HashSet();

            Collection<DistributedObject> distObjs =  client.getDistributedObjects();
            for(DistributedObject distObj: distObjs) {
                if (distObj.getServiceName().contains("mapService")) {
                    groups.add(distObj.getName());
                }
            }
            return groups;
        }

        @Override
        public List<Map<String, Object>> getStats() {
            List<Map<String, Object>> list = new ArrayList<>();

            //Getting the list of groups
            Set<String> currentGroups = getGroups();

            for ( String group : currentGroups ) {
                Map<String, Object> stats = new HashMap<>();

                stats.put("name", getName());
                stats.put("key", getKey());
                stats.put("region", group);
                stats.put("toDisk", false);
                stats.put("isDefault", false);
                stats.put("memory", client.getMap(group).keySet().size() + "");
                stats.put("disk", "0");
                stats.put("configuredSize", new Integer(new Long(client.getMap(group).getLocalMapStats().getOwnedEntryMemoryCost()).intValue()));

                stats.put("CacheStats",new CacheStats(0,0,0,0,0,0));

                list.add(stats);
        }

        return list;
    }

@Override
    public void shutdown() {
        client.shutdown();
    }
}
