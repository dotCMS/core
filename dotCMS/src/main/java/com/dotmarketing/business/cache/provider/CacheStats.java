package com.dotmarketing.business.cache.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by jasontesser on 3/22/17.
 */
public class CacheStats {

    Map<String, String> stats = new LinkedHashMap<>();

    public CacheStats(){}

    public void addStat(String statName, String value) {
        stats.put(statName, value);
    }
    public void addStat(String statName, Object value) {
      stats.put(statName, String.valueOf(value));
  }
    public Set<String> getStatColumns() {
        return stats.keySet();
    }

    public String getStatValue(String columnName) {
        return stats.get(columnName);
    }
    
    

}