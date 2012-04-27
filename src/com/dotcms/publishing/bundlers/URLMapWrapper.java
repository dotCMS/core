package com.dotcms.publishing.bundlers;

import com.dotmarketing.beans.Identifier;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;

public class URLMapWrapper {

	
	ContentletVersionInfo info;
	Contentlet content;
	Identifier id;
	String html;
	public ContentletVersionInfo getInfo() {
		return info;
	}
	public void setInfo(ContentletVersionInfo info) {
		this.info = info;
	}


	public Identifier getId() {
		return id;
	}
	public void setId(Identifier id) {
		this.id = id;
	}
	public Contentlet getContent() {
		return content;
	}
	public void setContent(Contentlet content) {
		this.content = content;
	}
	public String getHtml() {
		return html;
	}
	public void setHtml(String html) {
		this.html = html;
	}

	
}
