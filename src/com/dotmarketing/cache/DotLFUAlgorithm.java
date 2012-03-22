package com.dotmarketing.cache;

import org.jboss.cache.NodeNotValidException;
import org.jboss.cache.config.EvictionAlgorithmConfig;
import org.jboss.cache.eviction.LFUAlgorithm;
import org.jboss.cache.eviction.NodeEntry;

import com.dotmarketing.util.Logger;

public class DotLFUAlgorithm extends LFUAlgorithm {

	@Override
	protected void evict(NodeEntry arg0) {

		super.evict(arg0);

	}

	@Override
	public Class<? extends EvictionAlgorithmConfig> getConfigurationClass() {

		return DotEvictionAlgorithmConfig.class;
	}

	@Override
	protected boolean shouldEvictNode(NodeEntry ne) {

		String fqn = ne.getFqn().toString();

		//
		// Is this a region, if so, never evict
		//
		if (fqn.indexOf("/") == fqn.lastIndexOf("/")) {
			this.getEvictionQueue().removeNodeEntry(ne);
			return false;
		}
		
		
		
		
		// check the minimum time to live and see if we should not evict the
		// node. This check will
		// ensure that, if configured, nodes are kept alive for at least a
		// minimum period of time.
		if (isYoungerThanMinimumTimeToLive(ne)) {
			return false;
		}
		DotEvictionAlgorithmConfig config = (DotEvictionAlgorithmConfig) evictionAlgorithmConfig;

		int size= 0;
		try{
			size = cache.getNode(regionFqn).getChildren().size();
		}catch (NodeNotValidException e) {
			Logger.debug(this, "Node doesn't exist : " + e.getMessage());
			return false;
		}catch (NullPointerException e) {
			Logger.debug(this, "Node doesn't exist : " + e.getMessage());
			return false;
		}
//		int size = this.getEvictionQueue().getNumberOfNodes();
		if (config.getMaxNodes() > -1 && size > config.getMaxNodes()) {
			return true;
		} else if (size > config.getMinNodes()) {
			return true;
		}
		return false;

	}

}
