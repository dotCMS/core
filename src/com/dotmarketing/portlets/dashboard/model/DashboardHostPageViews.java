package com.dotmarketing.portlets.dashboard.model;

public class DashboardHostPageViews {
	
	private String hostId;
	
	private long totalPageViews;
	
	private int diff;

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public long getTotalPageViews() {
		return totalPageViews;
	}

	public void setTotalPageViews(long totalPageViews) {
		this.totalPageViews = totalPageViews;
	}

	public int getDiff() {
		return diff;
	}

	public void setDiff(int diff) {
		this.diff = diff;
	}
	
	

}
