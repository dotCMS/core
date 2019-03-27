package com.dotmarketing.portlets.dashboard.model;

import java.io.Serializable;

public class DashboardSummaryContent implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
    private long id;
	
    private DashboardSummary summary;
	
	private String inode;
	
	private long hits;
	
	private String uri;
	
	private String title;
	
	public DashboardSummaryContent(){
		
	}
	
	public DashboardSummaryContent(String inode, long hits, String uri, String title){
		this.inode = inode;
		this.hits = hits;
		this.uri = uri;
		this.title = title;
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

	public long getHits() {
		return hits;
	}

	public void setHits(long hits) {
		this.hits = hits;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	

}
