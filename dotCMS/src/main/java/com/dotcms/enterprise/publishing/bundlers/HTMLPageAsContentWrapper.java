package com.dotcms.enterprise.publishing.bundlers;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

public class HTMLPageAsContentWrapper {

	
	ContentletVersionInfo info;
	IHTMLPage asset;
	Identifier id;
	
	public ContentletVersionInfo getInfo() {
		return info;
	}
	public void setInfo(ContentletVersionInfo info) {
		this.info = info;
	}

	public IHTMLPage getAsset() {
		return asset;
	}
	public void setAsset(IHTMLPage asset) {
		this.asset = asset;
	}
	public Identifier getId() {
		return id;
	}
	public void setId(Identifier id) {
		this.id = id;
	}
	
	
	
	
	
	
}
