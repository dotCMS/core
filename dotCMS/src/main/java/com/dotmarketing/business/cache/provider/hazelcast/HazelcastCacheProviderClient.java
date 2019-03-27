package com.dotmarketing.business.cache.provider.hazelcast;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.dotmarketing.business.cache.provider.CacheStats;

public class HazelcastCacheProviderClient extends AbstractHazelcastCacheProvider {
	private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return "Hazelcast Client Provider";
    }

    @Override
    public String getKey() {
        return "HazelcastCacheProviderClient";
    }

    @Override
    protected HazelcastInstanceType getHazelcastInstanceType() {
    	return HazelcastInstanceType.CLIENT;
    }

    @Override

    protected CacheStats getStats(String group) {

        NumberFormat nf = DecimalFormat.getInstance();

        long size = getHazelcastInstance().getMap(group).keySet().size();

        CacheStats result = new CacheStats();
        result.addStat(CacheStats.REGION, group);
        result.addStat(CacheStats.REGION_SIZE, nf.format(size));

        return result;
    }
    
}
