package com.dotmarketing.portlets.chains.model;

import java.io.Serializable;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.ChainLink;
import com.dotmarketing.portlets.chains.business.ChainAPI;
import com.dotmarketing.portlets.chains.business.ChainLinkCodeCompilationException;

/**
 * 
 * @author davidtorresv
 *
 */
public class ChainState implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8468753308406750514L;

	private long id;
	
	private long chainId;
	
	private long linkCodeId;
	
	private long order;
	
	private List<ChainStateParameter> parameters;

	public ChainState() {
		
	}

	public ChainState(long linkCodeId) {
		this.linkCodeId = linkCodeId;
	}
	
	public ChainState(ChainLinkCode linkCode) {
		this.linkCodeId = linkCode.getId();
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getChainId() {
		return chainId;
	}
	public void setChainId(long chainId) {
		this.chainId = chainId;
	}
	public long getLinkCodeId() {
		return linkCodeId;
	}
	public void setLinkCodeId(long linkId) {
		this.linkCodeId = linkId;
	}
	public long getOrder() {
		return order;
	}
	public void setOrder(long order) {
		this.order = order;
	}
	
	public void setParameters(List<ChainStateParameter> parameters) {
		this.parameters = parameters;
	}
	public List<ChainStateParameter> getParameters() {
		if(parameters == null) {
			ChainAPI api = APILocator.getChainAPI();

			try {
				parameters = api.loadChainStateParameters(this);
			} catch (DotDataException e) {
				throw new DotRuntimeException(e.getMessage(), e);
			} catch (DotCacheException e) {
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
			
		return parameters;
	}
	
	public ChainLink getChainLink () throws DotRuntimeException, DotDataException, DotCacheException, ChainLinkCodeCompilationException {
		ChainAPI api = APILocator.getChainAPI();
		if(linkCodeId > 0)
			return api.instanciateChainLink(linkCodeId);
		else 
			return null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (chainId ^ (chainId >>> 32));
		result = prime * result + (int) (linkCodeId ^ (linkCodeId >>> 32));
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
		ChainState other = (ChainState) obj;
		if (chainId != other.chainId)
			return false;
		if (linkCodeId != other.linkCodeId)
			return false;
		return true;
	}

		
	
}
