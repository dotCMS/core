package com.dotcms.publisher.pusher.wrapper;

import java.util.List;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.links.model.Link;

public class LinkWrapper {
	private Identifier linkId;
	private List<Link> links;
	private VersionInfo vi;
	
	public LinkWrapper(Identifier linkId, List<Link> link) {
		this.linkId = linkId;
		this.links = link;
	}

	public Identifier getTemplateId() {
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
}
