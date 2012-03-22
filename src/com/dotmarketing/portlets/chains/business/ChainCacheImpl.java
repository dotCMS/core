package com.dotmarketing.portlets.chains.business;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainLinkCode;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;

/**
 * 
 * @author davidtorresv
 *
 */
public class ChainCacheImpl implements ChainCache {

	private DotCacheAdministrator cache;
	
	private String chainGroup = "ChainCache";
	private String chainByKeyGroup = "chainByKeyGroup";
	private String chainCodeGroup = "ChainCodeCache";
	private String stateGroup = "StateCache";
	private String chainStateParametersGroup = "chainStateParametersCache";
	
	private String[] groups = { chainGroup, chainByKeyGroup, chainCodeGroup, stateGroup };
 
	public ChainCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}
	
	public Chain getChain(long chainId) throws DotCacheException {
		return (Chain) cache.get(String.valueOf(chainId), chainGroup);
	}
	
	public Chain getChainByKey(String chainKey) throws DotCacheException {
		return (Chain) cache.get(chainKey, chainByKeyGroup);
	}

	public ChainLinkCode getChainLinkCode(long chainLinkCodeId) throws DotCacheException {
		return (ChainLinkCode) cache.get(String.valueOf(chainLinkCodeId), chainCodeGroup);
	}

	@SuppressWarnings("unchecked")
	public List<ChainState> getChainStates(long chainId) throws DotCacheException {
		return (List<ChainState>) cache.get(String.valueOf(chainId), stateGroup);
	}

	public Chain putChain(Chain chain) throws DotCacheException {
		
		Chain oldChain = getChain(chain.getId());
		
		cache.put(String.valueOf(chain.getId()), chain, chainGroup);
		cache.put(chain.getKey(), chain, chainByKeyGroup);
		
		return oldChain;
		
	}

	public ChainLinkCode putChainLinkCode(ChainLinkCode chainLinkCode) throws DotCacheException {
		
		ChainLinkCode oldChainLinkCode = getChainLinkCode(chainLinkCode.getId());
		
		cache.put(String.valueOf(chainLinkCode.getId()), chainLinkCode, chainCodeGroup);
		
		return oldChainLinkCode;
		
	}

	public List<ChainState> putChainStates(long chainId,
			List<ChainState> states) throws DotCacheException {
		
		List<ChainState> oldChainStates = getChainStates(chainId);
		
		cache.put(String.valueOf(chainId), states, stateGroup);
		
		return oldChainStates;
	}

	public Chain removeChain(long chainId) throws DotCacheException {
		Chain oldChain = getChain(chainId);
		
		if(oldChain != null) {
			cache.remove(String.valueOf(chainId), chainGroup);
			cache.remove(oldChain.getKey(), chainByKeyGroup);
		}
		
		return oldChain;
	}

	public ChainLinkCode removeChainLinkCode(long chainLinkCodeId) throws DotCacheException {
		ChainLinkCode oldChainLinkCode = getChainLinkCode(chainLinkCodeId);
		
		cache.remove(String.valueOf(chainLinkCodeId), chainCodeGroup);
		
		return oldChainLinkCode;
	}

	public List<ChainState> removeChainStates(long chainId) throws DotCacheException {
		List<ChainState> oldChainState = getChainStates(chainId);
		cache.remove(String.valueOf(chainId), stateGroup);
		return oldChainState;
	}

	@SuppressWarnings("unchecked")
	public List<ChainStateParameter> getChainStateParameters(long chainStateId)
			throws DotCacheException {
		return (List<ChainStateParameter>) cache.get(String.valueOf(chainStateId), chainStateParametersGroup);
	}

	public List<ChainStateParameter> putChainStateParameters(long chainStateId,
			List<ChainStateParameter> parameters) throws DotCacheException {
		List<ChainStateParameter> oldChainStateParameters = getChainStateParameters(chainStateId);
		cache.put(String.valueOf(chainStateId), parameters, chainStateParametersGroup);
		return oldChainStateParameters;
	}

	public List<ChainStateParameter> removeChainStateParameters(long chainStateId)
			throws DotCacheException {
		List<ChainStateParameter> oldChainStateParameters = getChainStateParameters(chainStateId);
		cache.remove(String.valueOf(chainStateId), chainStateParametersGroup);
		return oldChainStateParameters;
	}
	
	public void flushAllCaches() {
		for(String group: groups) 
			cache.flushGroup(group);
		
	}

	public void flushChainCache() {
		cache.flushGroup(chainGroup);		
	}

	public void flushChainLinkCodeCache() {
		cache.flushGroup(chainCodeGroup);		
	}

	public void flushChainStatesCache() {
		cache.flushGroup(stateGroup);		
	}

	public void flushChainStateParametersCache() {
		cache.flushGroup(chainStateParametersGroup);		
	}

	public String getPrimaryGroup() {
		return chainGroup;
	}

	public String[] getGroups() {
		return groups;
	}

	public void clearCache() {
		flushAllCaches();
	}


}
