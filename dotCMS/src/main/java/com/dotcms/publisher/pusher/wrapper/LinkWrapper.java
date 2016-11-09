package com.dotcms.publisher.pusher.wrapper;

import java.util.List;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.links.model.Link;

public class LinkWrapper {
	private Identifier linkId;
	private List<Link> links;
	private VersionInfo vi;
	private Operation operation;
	
	public LinkWrapper(Identifier linkId, List<Link> link) {
		this.linkId = linkId;
		this.links = link;
	}

	public Identifier getLinkId() {
		return linkId;
	}

	public void setLinkId(Identifier linkId) {
		this.linkId = linkId;
	}

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

	/**
	 * @return the vi
	 */
	public VersionInfo getVi() {
		return vi;
	}

	/**
	 * @param vi the vi to set
	 */
	public void setVi(VersionInfo vi) {
		this.vi = vi;
	}
	
	/**
	 * @return the operation
	 */
	public Operation getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
