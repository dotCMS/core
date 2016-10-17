package com.dotmarketing.portlets.chains.business;

import java.util.List;

import com.dotmarketing.business.Cachable;
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
public interface ChainCache extends Cachable{

	Chain putChain(Chain chain) throws DotCacheException;
	
	Chain getChain(long chainId) throws DotCacheException;

	Chain getChainByKey(String chainKey) throws DotCacheException;
	
	Chain removeChain(long chainId) throws DotCacheException;

	ChainLinkCode putChainLinkCode(ChainLinkCode chain) throws DotCacheException;
	
	ChainLinkCode getChainLinkCode(long chainLinkCodeId) throws DotCacheException;
	
	ChainLinkCode removeChainLinkCode(long chainLinkCodeId) throws DotCacheException;

	List<ChainState> putChainStates(long chainId, List<ChainState> states) throws DotCacheException;
	
	List<ChainState> getChainStates(long chainId) throws DotCacheException;
	
	List<ChainState> removeChainStates(long chainId) throws DotCacheException;

	List<ChainStateParameter> putChainStateParameters(long chainStateId, List<ChainStateParameter> parameters) throws DotCacheException;
	
	List<ChainStateParameter> getChainStateParameters(long chainStateId) throws DotCacheException;
	
	List<ChainStateParameter> removeChainStateParameters(long chainStateId) throws DotCacheException;
	
	void flushAllCaches();
	
	void flushChainCache();

	void flushChainLinkCodeCache();

	void flushChainStatesCache();

	void flushChainStateParametersCache();

}
