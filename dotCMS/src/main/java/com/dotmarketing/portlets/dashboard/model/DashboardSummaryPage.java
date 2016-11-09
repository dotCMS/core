package com.dotmarketing.portlets.dashboard.model;

import java.io.Serializable;

public class DashboardSummaryPage implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
	private long id;
	
	private DashboardSummary summary;
	
	private String inode;
	
	private String uri;
	
	private long hits;
	
	public DashboardSummaryPage(){
		
	}
	
	public DashboardSummaryPage(String inode, String uri, long hits){
		this.inode = inode;
		this.uri = uri;
		this.hits = hits;
	}
	
	public DashboardSummaryPage(String uri, long hits){
		this.uri = uri;
		this.hits = hits;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public DashboardSummary getSummary() {
		return summary;
	}

	public void setSummary(DashboardSummary summary) {
		this.summary = summary;
	}

	public String getInode() {
		return inode;
	}

	public void setInode(String inode) {
		this.inode = inode;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public long getHits() {
		return hits;
	}

	public void setHits(long hits) {
		this.hits = hits;
	}
	
	

}
