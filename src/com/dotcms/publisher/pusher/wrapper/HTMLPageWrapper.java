package com.dotcms.publisher.pusher.wrapper;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

public class HTMLPageWrapper {
	private HTMLPage page;
	private Identifier pageId;
	private VersionInfo vi;

	public HTMLPageWrapper(HTMLPage page, Identifier pageId) {
		this.page = page;
		this.pageId = pageId;
	}

	public HTMLPage getPage() {
		return page;
	}

	public void setPage(HTMLPage page) {
		this.page = page;
	}

	public Identifier getPageId() {
		return pageId;
	}

	public void setPageId(Identifier pageId) {
		this.pageId = pageId;
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
