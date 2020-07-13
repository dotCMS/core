package com.dotmarketing.business.cache.provider.hazelcast;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.UtilMethods;
import com.hazelcast.map.LocalMapStats;


/**
 * Created by jasontesser on 3/14/17.
 */
public class HazelcastCacheProviderEmbedded extends AbstractHazelcastCacheProvider {
	private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return "Hazelcast Embedded Provider";
    }

    @Override
    public String getKey() {
        return "HazelcastCacheProviderEmbedded";
    }

    @Override
    protected HazelcastInstanceType getHazelcastInstanceType() {
    	return HazelcastInstanceType.EMBEDDED;
    }

    @Override
    protected CacheStats getStats(String group) {

    	LocalMapStats local = getHazelcastInstance().getMap(group).getLocalMapStats();
        NumberFormat nf = DecimalFormat.getInstance();

        long size = getHazelcastInstance().getMap(group).keySet().size();
        long mem = local.getOwnedEntryMemoryCost();
        long perObject = (size==0) ? 0 : mem/size;
        String x = UtilMethods.prettyMemory(mem);

        long totalTime = local.getTotalGetLatency();
        long opCount = local.getGetOperationCount();
        long avgTime = (opCount == 0) ? 0  :  totalTime / opCount ;

        CacheStats result = new CacheStats();
        result.addStat(CacheStats.REGION, group);
        result.addStat(CacheStats.REGION_SIZE, nf.format(size));
        result.addStat(CacheStats.REGION_MEM_PER_OBJECT, UtilMethods.prettyByteify(perObject ));
        result.addStat(CacheStats.REGION_HITS, local.getHits());
        result.addStat(CacheStats.REGION_AVG_LOAD_TIME, nf.format(avgTime/1000000) + " ms"); 
        result.addStat(CacheStats.REGION_MEM_TOTAL_PRETTY, x);

        return result;
    }

}
