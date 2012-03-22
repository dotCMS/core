package com.dotmarketing.portlets.chains.business;

import java.util.List;

import com.dotmarketing.portlets.chains.model.Chain;

public class ChainsDependOnCodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6472466039460829928L;
	
	private List<Chain> dependentChains;
	public ChainsDependOnCodeException(List<Chain> chains) {
		setDependentChains(chains);
	}
	protected void setDependentChains(List<Chain> dependentChains) {
		this.dependentChains = dependentChains;
	}
	public List<Chain> getDependentChains() {
		return dependentChains;
	}

}
