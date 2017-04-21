package com.dotcms.cache.transport;

import com.dotcms.cluster.business.HazelcastUtil;
import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastCacheTransportClient extends AbstractHazelcastCacheTransport {

	@Override
    protected HazelcastInstance getHazelcastInstance() {
    	return new HazelcastUtil().getHazel(HazelcastInstanceType.CLIENT);
    }
}
