package com.dotcms.publisher.assets.bean;

import java.io.Serializable;
import java.util.Date;

public class PushedAsset implements Serializable {
	private String bundleId;
	private String assetId;
	private String assetType;
	private Date pushDate;
	private String environmentId;
	private String endpointIds;
	private String publisher;

	public PushedAsset() {}

	public PushedAsset(String bundleId, String assetId, String assetType, Date pushDate, String environmentId, String endpointIds, String publisher) {
		this.bundleId = bundleId;
		this.assetId = assetId;
		this.assetType = assetType;
		this.pushDate = pushDate;
		this.environmentId = environmentId;
		this.endpointIds = endpointIds;
		this.publisher = publisher;
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
	
	public void setEndpointId(String endpointIds) {
		this.endpointIds = endpointIds;
	}
		 
	public String getEndpointIds() {
		return endpointIds;
	}
		 
	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}



}
