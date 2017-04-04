package com.dotmarketing.business.cache.provider.hazelcast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hazelcast.core.MapStore;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

public class FSTMapStore implements MapStore<String, Object> {

  ChronicleMap<String, Object> _map;


  FSTMapStore() {
    ChronicleMapBuilder<String, Object> builder = ChronicleMapBuilder.of(String.class, Object.class).entries(1000);
    _map = builder.create();

  }

  ChronicleMap<String, Object> map(){
    return _map;
  }
  @Override
  public Object load(String key) {
    return map().get(key);
  }

  @Override
  public Map<String, Object> loadAll(Collection<String> keys) {
    
    HashMap<String, Object> maps = new HashMap<String, Object>();
    for(String x : keys){
      maps.put(x, load(x));
    }
    
   return maps;
  }

  @Override
  public Iterable<String> loadAllKeys() {
    return map().keySet();
  }

  @Override
  public void store(String key, Object value) {
    map().put(key, value);

  }

  @Override
  public void storeAll(Map<String, Object> m) {
    map().putAll(m);

  }

  @Override
  public void delete(String key) {
    map().remove(key);

  }

  @Override
  public void deleteAll(Collection<String> keys) {
    for(String x : keys){
      delete(x);
    }
  }

}
