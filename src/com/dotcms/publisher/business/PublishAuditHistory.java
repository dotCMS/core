package com.dotcms.publisher.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PublishAuditHistory implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private List<Map<String, Integer>> endpointsMap;
	private Date BundleStart;
	private Date BudleEnd;
	private Date PublishStart;
	private Date PublishEnd;
	private List<String> assets;
	
	public PublishAuditHistory() {
		assets = new ArrayList<String>();
		endpointsMap = new ArrayList<Map<String,Integer>>();
	}
	
	
	public List<Map<String, Integer>> getEndpointsMap() {
		return endpointsMap;
	}
	public void setEndpointsMap(List<Map<String, Integer>> endpointsMap) {
		this.endpointsMap = endpointsMap;
	}
	public Date getBundleStart() {
		return BundleStart;
	}
	public void setBundleStart(Date bundleStart) {
		BundleStart = bundleStart;
	}
	public Date getBudleEnd() {
		return BudleEnd;
	}
	public void setBudleEnd(Date budleEnd) {
		BudleEnd = budleEnd;
	}
	public Date getPublishStart() {
		return PublishStart;
	}
	public void setPublishStart(Date publishStart) {
		PublishStart = publishStart;
	}
	public Date getPublishEnd() {
		return PublishEnd;
	}
	public void setPublishEnd(Date publishEnd) {
		PublishEnd = publishEnd;
	}
	public List<String> getAssets() {
		return assets;
	}
	public void setAssets(List<String> assets) {
		this.assets = assets;
	}
}
