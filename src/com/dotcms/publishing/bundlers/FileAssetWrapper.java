package com.dotcms.publishing.bundlers;

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
