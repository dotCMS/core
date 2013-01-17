package com.dotcms.publisher.pusher.wrapper;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.links.model.Link;

public class LinkWrapper {
	private Identifier linkId;
	private Link link;
	private VersionInfo vi;
	
	public LinkWrapper(Identifier linkId, Link link) {
		this.linkId = linkId;
		this.link = link;
	}

	public Identifier getTemplateId() {
		return linkId;
	}

	public void setLinkId(Identifier linkId) {
		this.linkId = linkId;
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
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
