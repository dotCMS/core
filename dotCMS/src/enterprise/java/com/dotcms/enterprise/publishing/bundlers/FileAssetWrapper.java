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
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;

public class FileAssetWrapper {

	
	ContentletVersionInfo info;
	FileAsset asset;
	Identifier id;
	
	public ContentletVersionInfo getInfo() {
		return info;
	}
	public void setInfo(ContentletVersionInfo info) {
		this.info = info;
	}

	public FileAsset getAsset() {
		return asset;
	}
	public void setAsset(FileAsset asset) {
		this.asset = asset;
	}
	public Identifier getId() {
		return id;
	}
	public void setId(Identifier id) {
		this.id = id;
	}
	
	
	
	
	
	
}
