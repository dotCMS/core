package com.dotcms.cache.transport;

import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;

public class HazelcastCacheTransportEmbedded extends AbstractHazelcastCacheTransport {

	@Override
    protected HazelcastInstanceType getHazelcastInstanceType() {
    	return HazelcastInstanceType.EMBEDDED;
    }
	
    @Override
    public boolean requiresAutowiring() {
       
        return true;
    }
}
