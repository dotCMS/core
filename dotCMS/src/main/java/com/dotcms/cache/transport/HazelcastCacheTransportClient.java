package com.dotcms.cache.transport;

import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;

public class HazelcastCacheTransportClient extends AbstractHazelcastCacheTransport {

	@Override
    protected HazelcastInstanceType getHazelcastInstanceType() {
		return HazelcastInstanceType.CLIENT;
    }

    @Override
    public boolean shouldReinit() {
	    return false;
    }
    
    @Override
    public boolean requiresAutowiring() {
       
        return false;
    }
}
