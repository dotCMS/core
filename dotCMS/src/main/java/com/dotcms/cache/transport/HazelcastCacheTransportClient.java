package com.dotcms.cache.transport;

import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;

public class HazelcastCacheTransportClient extends AbstractHazelcastCacheTransport {

	@Override
    protected HazelcastInstanceType getHazelcastInstanceType() {
		return HazelcastInstanceType.CLIENT;
    }
}
