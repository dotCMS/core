package com.dotmarketing.portlets.chains.business;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainLinkCode;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author davidtorresv
 * 
 */
public class ChainFactoryImpl implements ChainFactory {

	public void deleteChain(Chain chain) throws DotDataException, DotCacheException {

		ChainCache cache = CacheLocator.getChainCache();
		chain = findChain(chain.getId());
		HibernateUtil.delete(chain);
		try {
			cache.removeChain(chain.getId());
		} catch (DotCacheException e) {
			Logger.warn(this, "deleteChain: Couldn't remove the key from cache", e);
		}

	}

	public void deleteChainState(ChainState state) throws DotDataException, DotCacheException {

		ChainCache cache = CacheLocator.getChainCache();
		Chain chain = findChain(state.getChainId());
		state = findChainState(state.getId());
		HibernateUtil.delete(state);

		// Making sure the remaining states got the right order
		// Removing gaps that could happen on deleting the state
		List<ChainState> states = findChainStates(chain);
		int order = 0;
		for (ChainState stateIt : states) {
			if (stateIt.getOrder() != order) {
				stateIt.setOrder(order);
				saveChainState(stateIt);
			}
		}

		// Getting rid of the cache entries forcing to lazily load later
		try {
			cache.removeChainStates(chain.getId());
		} catch (DotCacheException e) {
			Logger.warn(this, "deleteChain: Couldn't remove the key from cache", e);
		}

	}

	public void deleteChainLinkCode(ChainLinkCode link) throws DotDataException, DotCacheException {

		ChainCache cache = CacheLocator.getChainCache();
		List<ChainState> states = findChainLinkCodeStates(link);

		/**
		 * Deleting all the states related to this link
		 */
		for (ChainState state : states) {
			deleteChainState(state);
		}

		HibernateUtil.delete(link);

		// Getting rid of the cache entries forcing to lazily load them later
		try {
			cache.removeChainLinkCode(link.getId());
		} catch (DotCacheException e) {
			Logger.warn(this, "deleteChain: Couldn't remove the key from cache", e);
		}

	}

	public Chain findChain(long chainId) throws DotDataException {
		return (Chain) HibernateUtil.load(Chain.class, chainId);
	}

	public ChainState findChainState(long stateId) throws DotHibernateException {
		return (ChainState) HibernateUtil.load(ChainState.class, stateId);
	}

	@SuppressWarnings("unchecked")
	public List<Chain> findAllChains() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Chain.class);
		String query = "from chain in " + Chain.class;
		hu.setQuery(query);
		return hu.list();
	}

	@SuppressWarnings("unchecked")
	public List<ChainState> findChainStates(Chain chain) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(ChainState.class);
		String query = "from chainstate in " + ChainState.class + " where chainstate.chainId = ?";
		hu.setQuery(query);
		hu.setParam(chain.getId());
		return hu.list();
	}

	@SuppressWarnings("unchecked")
	public List<ChainStateParameter> findChainStateParameters(ChainState state) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(ChainStateParameter.class);
		String query = "from params in " + ChainStateParameter.class + " where params.chainStateId = ?";
		hu.setQuery(query);
		hu.setParam(state.getId());
		return hu.list();
	}

	@SuppressWarnings("unchecked")
	public List<ChainState> findChainLinkCodeStates(ChainLinkCode link) throws DotHibernateException {
		HibernateUtil hu = new HibernateUtil(ChainStateParameter.class);
		String query = "from state in " + ChainState.class + " where state.linkCodeId = ?";
		hu.setQuery(query);
		hu.setParam(link.getId());
		return hu.list();
	}

	public ChainLinkCode findChainLinkCode(long chainLinkCodeId) throws DotDataException {
		return (ChainLinkCode) HibernateUtil.load(ChainLinkCode.class, chainLinkCodeId);
	}

	public ChainLinkCode findChainLinkCodeByClassName(String fullyQualifiedClassName) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(ChainLinkCode.class);
		String query = "from code in " + ChainLinkCode.class + " where code.className = ?";
		hu.setQuery(query);
		hu.setParam(fullyQualifiedClassName);
		return (ChainLinkCode) hu.load();
	}

	@SuppressWarnings("unchecked")
	public List<ChainLinkCode> findAllChainLinkCodes() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(ChainLinkCode.class);
		String query = "from codes in " + ChainLinkCode.class;
		hu.setQuery(query);
		return hu.list();
	}

	public Chain loadChain(long chainId) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();

		Chain ch = cache.getChain(chainId);

		// Not in cache?
		if (ch == null) {
			ch = findChain(chainId);
			cache.putChain(ch);
		}

		return ch;
	}

	public List<ChainState> loadChainStates(Chain chain) throws DotCacheException, DotDataException {
		ChainCache cache = CacheLocator.getChainCache();

		List<ChainState> states = cache.getChainStates(chain.getId());

		// Not in cache?
		if (states == null) {
			states = findChainStates(chain);
			cache.putChainStates(chain.getId(), states);
		}

		return states;
	}

	public List<ChainStateParameter> loadChainStateParameters(ChainState state) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();

		List<ChainStateParameter> parameters = cache.getChainStateParameters(state.getId());

		// Not in cache?
		if (parameters == null) {
			parameters = findChainStateParameters(state);
			cache.putChainStateParameters(state.getId(), parameters);
		}

		return parameters;
	}

	public ChainLinkCode loadChainLinkCode(long chainLinkCodeId) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();

		ChainLinkCode linkCode = cache.getChainLinkCode(chainLinkCodeId);

		// Not in cache?
		if (linkCode == null) {
			linkCode = findChainLinkCode(chainLinkCodeId);
			cache.putChainLinkCode(linkCode);
		}

		return linkCode;
	}

	public void saveChain(Chain chain) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();
		HibernateUtil.save(chain);
		cache.removeChain(chain.getId());
		cache.putChain(chain);
	}

	public void saveChainState(ChainState state) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();
		HibernateUtil.save(state);
		cache.removeChainStates(state.getChainId());
	}

	public void saveChainLinkCode(ChainLinkCode link) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();
		HibernateUtil.save(link);
		cache.removeChainLinkCode(link.getId());
		cache.putChainLinkCode(link);
	}

	public void deleteChainStateParameter(ChainStateParameter p) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();
		HibernateUtil.delete(p);
		cache.removeChainStateParameters(p.getChainStateId());

	}

	public void saveChainStateParameter(ChainStateParameter p) throws DotCacheException, DotDataException {
		ChainCache cache = CacheLocator.getChainCache();
		HibernateUtil.save(p);
		cache.removeChainStateParameters(p.getChainStateId());

	}

	public Chain findChainByKey(String chainKey) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Chain.class);
		String query = "from chain in " + Chain.class + " where key_name = ?";
		hu.setQuery(query);
		hu.setParam(chainKey);
		return (Chain) hu.load();
	}

	public Chain loadChainByKey(String chainKey) throws DotDataException, DotCacheException {
		ChainCache cache = CacheLocator.getChainCache();

		Chain ch = cache.getChainByKey(chainKey);

		// Not in cache?
		if (ch == null) {
			ch = findChainByKey(chainKey);
			if(ch != null)
				cache.putChain(ch);
		}

		return ch;
	}

	@SuppressWarnings("unchecked")
	public List<Chain> findDependentChains(ChainLinkCode chainLinkCode) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Chain.class);
		String query = "from chain in " + Chain.class + ", chain_state in " + ChainState.class + " where chain_state.linkCodeId = ? " +
				"and chain_state.chainId = chain.id";
		hu.setQuery(query);
		hu.setParam(chainLinkCode.getId());
		return hu.list();
	}

}
