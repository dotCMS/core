package com.dotmarketing.portlets.chains.business;

import java.util.List;

import com.dotmarketing.portlets.chains.model.ChainStateParameter;

public class DuplicateChainStateParametersException extends Exception {

	private List<ChainStateParameter> parameters;
	
	public DuplicateChainStateParametersException(String message, List<ChainStateParameter> pl) {
		super(message);
		setParameters(pl);
	}


	public void setParameters(List<ChainStateParameter> parameters) {
		this.parameters = parameters;
	}


	public List<ChainStateParameter> getParameters() {
		return parameters;
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 2997380724829540762L;

}
