package com.dotmarketing.cache;


public class DotEvictionAlgorithmConfig extends org.jboss.cache.eviction.LFUAlgorithmConfig {




	@Override
	public String getEvictionAlgorithmClassName() {

		return com.dotmarketing.cache.DotLFUAlgorithm.class.getName();
	}

	@Override
	public int getMinNodes() {

		// make the cache work as expected with max nodes
		// this will prune down to the max node setting
		if(super.getMinNodes() <1 && super.getMaxNodes() > 0){
			super.setMinNodes(super.getMaxNodes());
		}

		return super.getMinNodes();
	}

	@Override
	public String toString() {
		return getEvictionAlgorithmClassName() + " [maxNodes=" + maxNodes +
				 ", minTimeToLive=" + minTimeToLive + 
				", minNodes=" + getMinNodes()+ "]";
	}

	
}
