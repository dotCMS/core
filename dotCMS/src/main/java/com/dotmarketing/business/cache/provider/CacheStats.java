package com.dotmarketing.business.cache.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;
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
  /** Serialized bytes written since initialization/full flush, including replacements; not memory. */
  public final static String REGION_BYTES_WRITTEN="cache.stats.region.bytes.written";
  /** Write-weighted serialized size since initialization/full flush, not retained Java heap size. */
  public final static String REGION_AVG_SERIALIZED_SIZE="cache.stats.region.serialized.size.avg";
  public final static String REGION_AVG_LOAD_TIME="cache.stats.region.load.time.avg";
  public final static String REGION_EVICTIONS="cache.stats.region.evictions";

    Map<String, String> stats = new LinkedHashMap<>();

    // Native counters are metadata, not UI columns; REGION_LOAD has provider-specific meanings.
    private final Long hitCount;
    private final Long requestCount;

    public CacheStats() {
        hitCount = null;
        requestCount = null;
    }

    /** Creates stats with the provider's native hits and total requests (hits plus misses). */
    public CacheStats(final long hitCount, final long requestCount) {
        this.hitCount = hitCount;
        this.requestCount = requestCount;
    }

    /** Returns native hits, or empty when this provider does not report request counters. */
    public OptionalLong getHitCount() {
        return hitCount == null ? OptionalLong.empty() : OptionalLong.of(hitCount);
    }

    /** Returns native total requests, or empty when this provider does not report them. */
    public OptionalLong getRequestCount() {
        return requestCount == null ? OptionalLong.empty() : OptionalLong.of(requestCount);
    }

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
