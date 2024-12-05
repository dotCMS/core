package com.dotmarketing.business.cache.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by jasontesser on 3/22/17.
 */
public class CacheStats {

  public final static String REGION="cache.stats.region";
  public final static String REGION_DEFAULT="cache.stats.region.default";
  public final static String REGION_CONFIGURED_SIZE="cache.stats.region.configured.size";
  public final static String REGION_SIZE="cache.stats.region.size";
  public final static String REGION_LOAD="cache.stats.region.load";
  public final static String REGION_HITS="cache.stats.region.hits";
  public final static String REGION_HIT_RATE="cache.stats.region.hit.rate";
  public final static String REGION_MEM_TOTAL="cache.stats.region.mem.total";
  public final static String REGION_MEM_TOTAL_PRETTY="cache.stats.region.mem.total.pretty";
  public final static String REGION_MEM_PER_OBJECT="cache.stats.region.mem.per.object";
  public final static String REGION_MEM_PER_OBJECT_PRETTY="cache.stats.region.mem.per.object";
  public final static String REGION_AVG_LOAD_TIME="cache.stats.region.load.time.avg";
  public final static String REGION_EVICTIONS="cache.stats.region.evictions";

    Map<String, String> stats = new LinkedHashMap<>();

    public CacheStats(){}

    public CacheStats addStat(String statName, String value) {
        stats.put(statName, value);
        return this;
    }

    public CacheStats addStat(String statName, long value) {
      stats.put(statName, value+"");
      return this;
    }
    
    public Set<String> getStatColumns() {
        return stats.keySet();
    }

    public String getStatValue(String columnName) {
        return stats.get(columnName);
    }
    
    

}
