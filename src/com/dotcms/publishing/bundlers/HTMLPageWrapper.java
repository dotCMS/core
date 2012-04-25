package com.dotcms.publishing.bundlers;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

public class HTMLPageWrapper {

	private Identifier identifier;
	private VersionInfo versionInfo;
	private HTMLPage page;
	
	/**
	 * @return the identifier
	 */
	public Identifier getIdentifier() {
		return identifier;
	}
	/**
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}
	/**
	 * @return the versionInfo
	 */
	public VersionInfo getVersionInfo() {
		return versionInfo;
	}
	/**
	 * @param versionInfo the versionInfo to set
	 */
	public void setVersionInfo(VersionInfo versionInfo) {
		this.versionInfo = versionInfo;
	}
	/**
	 * @return the page
	 */
	public HTMLPage getPage() {
		return page;
	}
	/**
	 * @param page the page to set
	 */
	public void setPage(HTMLPage page) {
		this.page = page;
	}
	
}
