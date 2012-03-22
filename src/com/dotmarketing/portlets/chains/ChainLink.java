package com.dotmarketing.portlets.chains;

import com.dotmarketing.portlets.chains.model.ChainLinkCode;

/**
 * 
 * This is the class that every Link implementation should extend
 * @author davidtorresv
 *
 */
public abstract class ChainLink {

	private ChainControl chainData;
	private ChainLinkCode code;

	
	/**
	 * Returns a short title that names the link
	 * @return
	 */
	public abstract String getTitle ();
	
	/**
	 * Returns a full description of what the link does and how
	 * behaves
	 * @return
	 */
	public abstract String getDescription ();
	
	/**
	 * 
	 * @return
	 */
	public abstract ChainLinkParameter[] getParameters ();
	
	/**
	 * This method will return a list of fully class qualified names of
	 * the required links that should be executed before this link
	 * @return
	 */
	public abstract String[] getRequiredLinks();
	
	/**
	 * This is the method invoked 
	 * @return
	 */
	public final boolean execute(ChainControl data) {
		this.chainData = data;
		return run();
	}
	
	/**
	 * This is the method that will implement the link functionality
	 */
	public abstract boolean run ();
	
	/**
	 * Returns the current chaindata during the execution of the pipe
	 * @return
	 */
	public final ChainControl getChainData() {
		return chainData;
	}

	public void setCode(ChainLinkCode code) {
		this.code = code;
	}

	public ChainLinkCode getCode() {
		return code;
	}


	
}
