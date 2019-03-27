package com.dotmarketing.portlets.dashboard.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class DashboardSummary404 implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
	private long id;
	
	private DashboardSummaryPeriod summaryPeriod;
	
	private String hostId;
	
	private String uri;
	
	private String refererUri;
	
	private boolean ignored;
	
	private Set userPreferences;

	public DashboardSummary404(){
		
	}
	
	public DashboardSummary404(long id, String hostId, String uri, String refererUri, Boolean ignored){
		this.id = id;
		this.hostId = hostId;
		this.uri = uri;
		this.refererUri = refererUri;
		this.ignored = ignored==null?false:ignored.booleanValue();
		
	}

		
	public boolean isIgnored() {
		return ignored;
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public DashboardSummaryPeriod getSummaryPeriod() {
		return summaryPeriod;
	}

	public void setSummaryPeriod(DashboardSummaryPeriod summaryPeriod) {
		this.summaryPeriod = summaryPeriod;
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getRefererUri() {
		return refererUri;
	}

	public void setRefererUri(String refererUri) {
		this.refererUri = refererUri;
	}

	public Set getUserPreferences() {
		return userPreferences;
	}

	public void setUserPreferences(Set userPreferences) {
		this.userPreferences = userPreferences;
	}
	
	
    
	
	
}
