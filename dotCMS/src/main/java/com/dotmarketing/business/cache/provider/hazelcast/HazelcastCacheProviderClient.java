package com.dotmarketing.business.cache.provider.hazelcast;

import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;

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
}
