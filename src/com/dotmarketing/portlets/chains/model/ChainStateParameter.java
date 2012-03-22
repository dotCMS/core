package com.dotmarketing.portlets.chains.model;

/**
 * 
 * @author davidtorresv
 *
 */
public class ChainStateParameter {

	private long id;
	
	private long chainStateId;
	
	private String name;
	
	private String value;
		
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getChainStateId() {
		return chainStateId;
	}

	public void setChainStateId(long chainLinkId) {
		this.chainStateId = chainLinkId;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String parameterName) {
		this.name = parameterName;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String parameterValue) {
		this.value = parameterValue;
	}

	public void setNameValue(String parameterName, String parameterValue) {
		this.name = parameterName;
		this.value = parameterValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (chainStateId ^ (chainStateId >>> 32));
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChainStateParameter other = (ChainStateParameter) obj;
		if (chainStateId != other.chainStateId)
			return false;
		if (id != other.id)
			return false;
		return true;
	}
	
		
}
