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
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * Created by jasontesser on 3/14/17.
 */
public class HazelCastCacheProvider extends CacheProvider {

  /**
  * 
  */
  private static final long serialVersionUID = 1L;
  protected HazelcastInstance hazel = null;
  protected Boolean initialized = false;
  private final String configFile;

  public HazelCastCacheProvider() {
    this("hazelcast-dotcms.xml");
  }

  public HazelCastCacheProvider(final String configFile) {
    this.configFile = configFile;
  }



  
  
  
  
  
  
  
  
  
  
  
  @Override
  public void init() throws Exception {
    Logger.info(this, "Setting Up HazelCast Cache Config");
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFile)) {
      if (is!=null) {
        XmlClientConfigBuilder builder = new XmlClientConfigBuilder(is);
        hazel = HazelcastClient.newHazelcastClient(builder.build());
        initialized = true;
      }
      else{
        hazel = Hazelcast.newHazelcastInstance();
        initialized = true;
      }
    }
  }

  
  private IMap<String, Object> map(String group){
    IMap<String, Object> map =hazel.getMap(group);

    return map;
  }
  
  
  
  
  @Override
  public String getName() {
    return "HazelCast Cache Provider";
  }

  @Override
  public String getKey() {
    return this.getClass().getTypeName();
  }

  @Override
  public boolean isInitialized() throws Exception {
    return initialized;
  }

  @Override
  public void put(String group, String key, Object content) {
    map(group).set(key, content);
  }

  @Override
  public Object get(String group, String key) {
    return map(group).get(key);
  }

  @Override
  public void remove(String group, String key) {
    map(group).remove(key);
  }

  @Override
  public void remove(String group) {
    map(group).clear();
  }

  @Override
  public void removeAll() {
    Collection<DistributedObject> distObjs = hazel.getDistributedObjects();
    for (DistributedObject distObj : distObjs) {
      if (distObj.getServiceName().contains("mapService")) {
        map(distObj.getName()).clear();
      }
    }
  }

  @Override
  public Set<String> getKeys(String group) {
    Set<String> keys = new HashSet<String>();
    for (Object key : map(group).keySet()) {
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
    CacheProviderStats ret = new CacheProviderStats(providerStats, getName());
    Set<String> currentGroups = getGroups();

    for (String group : currentGroups) {
      CacheStats stats = new CacheStats();

      stats.addStat("cache-region", group);
      stats.addStat("cache-local-memory-cost", UtilMethods.prettyByteify(map(group).getLocalMapStats().getOwnedEntryMemoryCost()));
      stats.addStat("cache-local-heap-cost", map(group).getLocalMapStats().getHeapCost());
      stats.addStat("cache-requests", map(group).getLocalMapStats().getGetOperationCount());
      stats.addStat("cache-hits", map(group).getLocalMapStats().getHits());
      stats.addStat("cache-local-size", map(group).getLocalMapStats().getOwnedEntryCount());


      ret.addStatRecord(stats);
    }

    return ret;
  }

  @Override
  public void shutdown() {
    hazel.shutdown();
  }
}
