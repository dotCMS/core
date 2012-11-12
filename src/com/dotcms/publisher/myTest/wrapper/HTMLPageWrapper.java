package com.dotcms.publisher.myTest.wrapper;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

public class HTMLPageWrapper {
	private HTMLPage page;
	private Identifier pageId;

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
	
	
}
