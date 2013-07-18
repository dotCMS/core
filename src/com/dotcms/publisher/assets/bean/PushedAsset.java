package com.dotcms.publisher.assets.bean;

import java.util.Date;

public class PushedAsset {
	private String bundleId;
	private String assetId;
	private String assetType;
	private Date pushDate;
	private String environmentId;

	public PushedAsset() {}

	public PushedAsset(String bundleId, String assetId, String assetType, Date pushDate, String environmentId) {
		this.bundleId = bundleId;
		this.assetId = assetId;
		this.assetType = assetType;
		this.pushDate = pushDate;
		this.environmentId = environmentId;
	}

	public String getAssetId() {
		return assetId;
	}
	public void setAssetId(String id) {
		this.assetId = id;
	}
	public String getAssetType() {
		return assetType;
	}
	public void setAssetType(String name) {
		this.assetType = name;
	}
	public Date getPushDate() {
		return pushDate;
	}
	public void setPushDate(Date publishDate) {
		this.pushDate = publishDate;
	}

	public String getEnvironmentId() {
		return environmentId;
	}
	public void setEnvironmentId(String owner) {
		this.environmentId = owner;
	}

	public String getBundleId() {
		return bundleId;
	}

	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}



}
