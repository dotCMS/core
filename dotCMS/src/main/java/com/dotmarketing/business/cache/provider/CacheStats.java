package com.dotmarketing.business.cache.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Created by jasontesser on 3/22/17. */
public class CacheStats {

  public static final String REGION = "cache.stats.region";
  public static final String REGION_DEFAULT = "cache.stats.region.default";
  public static final String REGION_CONFIGURED_SIZE = "cache.stats.region.configured.size";
  public static final String REGION_SIZE = "cache.stats.region.size";
  public static final String REGION_LOAD = "cache.stats.region.load";
  public static final String REGION_HITS = "cache.stats.region.hits";
  public static final String REGION_HIT_RATE = "cache.stats.region.hit.rate";
  public static final String REGION_MEM_TOTAL = "cache.stats.region.mem.total";
  public static final String REGION_MEM_TOTAL_PRETTY = "cache.stats.region.mem.total.pretty";
  public static final String REGION_MEM_PER_OBJECT = "cache.stats.region.mem.per.object";
  public static final String REGION_MEM_PER_OBJECT_PRETTY = "cache.stats.region.mem.per.object";
  public static final String REGION_AVG_LOAD_TIME = "cache.stats.region.load.time.avg";
  public static final String REGION_EVICTIONS = "cache.stats.region.evictions";

  Map<String, String> stats = new LinkedHashMap<>();

  public CacheStats() {}

  public void addStat(String statName, String value) {
    stats.put(statName, value);
  }

  public void addStat(String statName, long value) {
    stats.put(statName, value + "");
  }

  public Set<String> getStatColumns() {
    return stats.keySet();
  }

  public String getStatValue(String columnName) {
    return stats.get(columnName);
  }
}
