package com.dotmarketing.portlets.dashboard.model;

import java.io.Serializable;
import java.util.Date;

public class DashboardUserPreferences implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
	private long id;
	
	private DashboardSummary404 summary404;
	
	private boolean ignored;
	
	private String userId;
	
	private Date modDate;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public DashboardSummary404 getSummary404() {
		return summary404;
	}

	public void setSummary404(DashboardSummary404 summary404) {
		this.summary404 = summary404;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getModDate() {
		return modDate;
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	
	


}
