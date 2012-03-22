package com.dotmarketing.portlets.chains.business;

import java.util.List;

import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.ChainLink;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;
import com.dotmarketing.portlets.chains.model.ChainLinkCode.Language;

/**
 * 
 * @author davidtorresv
 *
 */
public interface ChainAPI {

	/**
	 * Save a chain with all it states configuration, it makes sure that all the state have correct order set 
	 * if not it tries to reorganize them based on the order of the given list
	 * @param chain
	 * @param states
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 * @throws DuplicateChainStateParametersException 
	 * @throws ChainAlreadyExistsException 
	 */
	public void saveChain(Chain chain, List<ChainState> states) throws DotDataException, DotCacheException, DuplicateChainStateParametersException, ChainAlreadyExistsException;
	
	/**
	 * Retrieve a list of states associated to a chain in the order they are setup on the chain
	 * @param chain
	 * @return
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 */
	public List<ChainState> getChainStates (Chain chain) throws DotDataException, DotCacheException;
	
	/**
	 * Look for a chain object based on it's id, returns null if doesn't find it
	 * @param chainId
	 * @return
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 */
	public Chain loadChain(long chainId) throws DotDataException, DotCacheException;
	
	/**
	 * Returns a list of all chain setup in db
	 * @return
	 * @throws DotDataException 
	 */
	public List<Chain> findAllChains() throws DotDataException;

	/**
	 * This method as the logic to instantiate a chain link class taking the code
	 * from the db, compiling it and giving to you an instance of it, if the code
	 * is already compiled and the implementation hasn't change in db, it will take it
	 * from disk and not re-compile.
	 * @param linkId
	 * @return
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 * @throws DotRuntimeException 
	 * @throws ChainLinkCodeCompilationException 
	 */
	public ChainLink instanciateChainLink (long linkId) throws DotRuntimeException, DotDataException, DotCacheException, ChainLinkCodeCompilationException;
	
	/**
	 * Returns a list of all chain links setup in db and instantiate them
	 * @return
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 * @throws DotRuntimeException 
	 */
	public List<ChainLink> findAllChainLinks() throws DotDataException, DotRuntimeException, DotCacheException;
	
	/**
	 * Finds a list of chain links which properties (title, usage, class name) contains the given keyword 
	 * Used on the UI to search for specific chain links by it code class name
	 * This is a heavy method use it carefully because it needs to compile and instanciate
	 * all configured chain links to be able to search on its title, description, etc
	 * @return
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 * @throws DotRuntimeException 
	 */
	public List<ChainLink> findChainLinksByKeyword(String keyword) throws DotRuntimeException, DotDataException, DotCacheException;
	
	
	/**
	 * Searches a chain link code object based on its fully qualified class name
	 * @param fullyQualifiedClassName
	 * @return
	 * @throws DotDataException 
	 * @throws DotCacheException 
	 * @throws ChainLinkCodeCompilationException 
	 * @throws DotRuntimeException 
	 */
	public ChainLink findChainLinkByClassName(String fullyQualifiedClassName) throws DotDataException, DotRuntimeException, ChainLinkCodeCompilationException, DotCacheException;

	/**
	 * Retrieves all the parameters associated to the given state
	 * @param chainState
	 * @return
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 */
	public List<ChainStateParameter> loadChainStateParameters(ChainState chainState) throws DotDataException, DotCacheException;

	/**
	 * Retrieves a chain by its given key
	 * @param chainName
	 * @return
	 * @throws DotCacheException 
	 * @throws DotDataException 
	 */
	public Chain loadChainByKey(String chainKey) throws DotDataException, DotCacheException;
	
	/**
	 * Adds a new chain link to the database
	 * @param className The class name in the code
	 * @param code The code of the chain link
	 * @param lang
	 * @return
	 * @throws DotDataException
	 * @throws DotRuntimeException
	 * @throws ChainLinkCodeCompilationException
	 * @throws DotCacheException
	 */
	public ChainLink addChainLink(String className, String code, Language lang) throws DuplicatedChainLinkException, DotDataException, DotRuntimeException, ChainLinkCodeCompilationException, DotCacheException;
	
	/**
	 * Updates the code on a chain link 
	 * @param chainLinkId the id of the chain link to update if 0 is passed then it will create a new chain link
	 * @param className The class name to instantiate in the passed code
	 * @param code The code to compile
	 * @param lang The language of the passed code
	 * @return The instantiated chain link 
	 * @throws ChainLinkCodeCompilationException
	 * @throws DotDataException
	 * @throws DotRuntimeException
	 * @throws DotCacheException
	 */
	public ChainLink saveChainLink(long chainLinkId, String className, String code, Language lang) throws ChainLinkCodeCompilationException, DotDataException, DotRuntimeException, DotCacheException;

	
	/**
	 * Updates the code on a chain link 
	 * @param chainLinkId the id of the chain link to update if 0 is passed then it will create a new chain link
	 * @param className The class name to instantiate in the passed code
	 * @param code The code to compile
	 * @param lang The language of the passed code
	 * @return The instantiated chain link 
	 * @throws ChainsDependOnCodeException 
	 * @throws DotDataException 
	 * @throws ChainLinkCodeCompilationException
	 * @throws DotDataException
	 * @throws DotCacheException 
	 * @throws DotRuntimeException
	 * @throws DotCacheException
	 */
	public void deleteChainLink(ChainLink chainLink) throws ChainsDependOnCodeException, DotDataException, DotCacheException;

	/**
	 * Updates the code on a chain link 
	 * @param chainLinkId the id of the chain link to update if 0 is passed then it will create a new chain link
	 * @param className The class name to instantiate in the passed code
	 * @param code The code to compile
	 * @param lang The language of the passed code
	 * @return The instantiated chain link 
	 * @throws DotDataException 
	 * @throws ChainLinkCodeCompilationException
	 * @throws DotDataException
	 * @throws DotCacheException 
	 * @throws DotRuntimeException
	 * @throws DotCacheException
	 */
	public void deleteChain(Chain chain) throws DotDataException, DotCacheException;

}
