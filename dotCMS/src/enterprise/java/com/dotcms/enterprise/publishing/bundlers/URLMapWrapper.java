/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

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
