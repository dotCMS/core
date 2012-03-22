package com.dotmarketing.portlets.chains.model;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.business.ChainAPI;

/**
 * 
 * @author davidtorresv
 *
 */
public class Chain {

	private long id;

	private String name;
	
	private String key;
	
 	private String successValue;
	
	private String failureValue;
	
	public Chain () {
		
	}
	
	public Chain (String name, String key, String successValue, String failureValue) {
		this.name = name;
		this.key = key;
		this.successValue = successValue;
		this.failureValue = failureValue;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		if(key == null) {
			setKey(name.replaceAll("\\s", "_").replaceAll("[^A-Za-z0-9_]", "").toLowerCase());
		}
			
	}

	public String getSuccessValue() {
		return successValue;
	}

	public void setSuccessValue(String returnUrl) {
		this.successValue = returnUrl;
	}

	public String getFailureValue() {
		return failureValue;
	}

	public void setFailureValue(String failureUrl) {
		this.failureValue = failureUrl;
	}

	private void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public List<ChainState> getStates() {
		ChainAPI api = APILocator.getChainAPI();
		try {
			return api.getChainStates(this);
		} catch (DotDataException e) {
			throw new DotRuntimeException("Unable to retrieve the given chain state, chain key = " + this.key);
		} catch (DotCacheException e) {
			throw new DotRuntimeException("Unable to retrieve the given chain state, chain key = " + this.key);
		}
	}
	
}
