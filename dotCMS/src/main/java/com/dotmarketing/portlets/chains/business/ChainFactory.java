package com.dotmarketing.portlets.chains.business;

import java.util.List;

import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainLinkCode;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;


/**
 * 
 * @author davidtorresv
 *
 */
public interface ChainFactory {

	//Lookup methods
	
	/**
	 * Tries to look for the chain on cache if doesn't find it,
	 * it uses @see findChain
	 * returns null if doesn't find it
	 * @param chainId
	 * @return
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 */
	Chain loadChain(long chainId) throws DotDataException, DotCacheException;
	
	/**
	 * Tries to look for the chain on cache if doesn't find it,
	 * it uses @see findChainByKey
	 * @param chainKey The key of the chain to look for
	 * @return The found chain, null if the chain doesn't exist
	 */
	Chain loadChainByKey(String chainKey) throws DotDataException, DotCacheException;
	
	/**
	 * Tries to look for the chain object directly in db
	 * returns null if doesn't find it
	 * @param chainId
	 * @return
	 * @throws DotDataException 
	 */
	Chain findChain(long chainId) throws DotDataException;
	
	/**
	 * Tries to look for the chain object directly in db
	 * returns null if doesn't find it
	 * @param chainKey
	 * @return
	 * @throws DotDataException 
	 */
	Chain findChainByKey(String chainKey) throws DotDataException;	
	
	
	/**
	 * Tries to look for the chain object directly in db by its id
	 * @param stateId
	 * @return
	 * @throws DotDataException
	 */
	ChainState findChainState(long stateId) throws DotDataException;
	
	/**
	 * 
	 * @param chain
	 * @return
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 */
	List<ChainState> loadChainStates(Chain chain) throws DotCacheException, DotDataException;

	/**
	 * 
	 * @param chain
	 * @return  A list of chain link state where you can grab the link, the chain and the position that link is on the chain
	 * @throws DotDataException 
	 */
	List<ChainState> findChainStates(Chain chain) throws DotDataException;
	
	/**
	 * Return the list of states that are using the given ChainLinkCode, this method goes to DB directly
	 * @param link
	 * @return  
	 * @throws DotHibernateException 
	 */
	List<ChainState> findChainLinkCodeStates(ChainLinkCode link) throws DotHibernateException;
	
	
	/**
	 * This method returns the parameters associated to the given state on the chain,
	 * this method tries to load from cache first if not in cache uses the @see loadChainLinkParameters
	 * @param state
	 * @return
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 */
	List<ChainStateParameter> loadChainStateParameters(ChainState state) throws DotDataException, DotCacheException;

	/**
	 * This method performs a database lookup for the parameters associated to a given state
	 * of the chain.
	 * @param state
	 * @return
	 * @throws DotDataException 
	 */
	List<ChainStateParameter> findChainStateParameters(ChainState state) throws DotDataException;

	
	/**
	 * This method returns the chain link code given the chain link code id,
	 * this method tries to load from cache first if not in cache uses the @see findChainLinkCode
	 * @param chainLinkCodeId
	 * @return
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 */
	ChainLinkCode loadChainLinkCode(long chainLinkCodeId) throws DotDataException, DotCacheException;

	/**
	 * This method performs a database lookup for the chain link code given the chain link code id.
	 * @param chainLinkCodeId
	 * @return
	 * @throws DotDataException 
	 */
	ChainLinkCode findChainLinkCode(long chainLinkCodeId) throws DotDataException;
	
	/**
	 * This method performs a database lookup for the chain link code given the chain link code id.
	 * @param chainLinkCodeId
	 * @return
	 * @throws DotDataException 
	 */
	List<Chain> findDependentChains(ChainLinkCode chainLinkCode) throws DotDataException;
	
	//Save/Update/Delete Methods
	
	/**
	 * Saves/update a chain in the database
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 */
	void saveChain(Chain chain) throws DotDataException, DotCacheException;
	
	/**
	 * Removes a chain and all its related states from the database
	 * @param chain
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 */
	void deleteChain(Chain chain) throws DotDataException, DotCacheException;
	
	/**
	 * Saves/updates a chain link from the database
	 * @param link
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 */
	void saveChainLinkCode(ChainLinkCode link) throws DotDataException, DotCacheException;
	
	/**
	 * Deletes a link from the database an also look for all the possible states that reference the link
	 * and also delete them from the db as well
	 * @param link
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 * @throws DotCacheException 
	 */
	void deleteChainLinkCode(ChainLinkCode link) throws DotDataException, DotCacheException;
	
	/**
	 * This method save and chain state into the database and also takes care of reordering other states
	 * so orders are not overlapped
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 */
	void saveChainState(ChainState state) throws DotDataException, DotCacheException;
		
	/**
	 * This method deletes a chain state from the db and takes cares of reorganizing other states
	 * to compact the order and not leave holes
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 * @throws DotCacheException 
	 */
	void deleteChainState(ChainState state) throws DotDataException, DotCacheException;
	
	
	/**
	 * Saves the given chain state parameter in db
	 * @param p
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 */
	void saveChainStateParameter(ChainStateParameter p) throws DotCacheException, DotDataException;
	
	/**
	 * Deletes the given state parameter from db
	 * @param p
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 */
	void deleteChainStateParameter(ChainStateParameter p) throws DotDataException, DotCacheException;

	//General find methods

	/**
	 * It queries the db and retrieve all the configured chain links codes
	 * @return
	 * @throws DotDataException 
	 */
	List<ChainLinkCode> findAllChainLinkCodes() throws DotDataException;

	/**
	 * It queries the db and retrieve all the configured chains
	 * @return
	 * @throws DotDataException 
	 * @throws DotDataException 
	 */
	List<Chain> findAllChains() throws DotDataException;

	/**
	 * Queries searching for chain link code by its fullyquelified class name
	 * Only one class (fully qualified) can leave on the links class loader
	 * @param fullyQualifiedClassName
	 * @return
	 * @throws DotDataException 
	 */
	ChainLinkCode findChainLinkCodeByClassName(String fullyQualifiedClassName) throws DotDataException;



	

}
