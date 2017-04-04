package com.dotmarketing.business.cache.provider.hazelcast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.hazelcast.core.MapStore;



public class MapDbMapMapStore implements MapStore<String, Object> {

  static ConcurrentMap<String,Object> _map;

  MapDbMapMapStore() {
    if(_map==null){
    DB db = DBMaker
        .fileDB("/tmp/dotMmap")
        .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
        .fileMmapPreclearDisable()
        .closeOnJvmShutdown()
        .cleanerHackEnable()
        .make();
    _map = (ConcurrentMap<String,Object>) db.hashMap("map").createOrOpen();
    }
  }

  Map<String, Object> map(){
    
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
