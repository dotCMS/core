package com.dotmarketing.business.cache.provider.hazelcast;

import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;

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
}
