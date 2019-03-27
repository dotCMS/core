package com.dotmarketing.portlets.dashboard.model;

import java.io.Serializable;
import java.util.Date;

public class DashboardSummaryVisits implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
	private Long id;
	
	private DashboardSummaryPeriod summaryPeriod;
	
	private String hostId;
	
	private Date visitTime;
	
	private Long visits;
	
	private String formattedTime;

	public DashboardSummaryVisits(){
		
	}
	
	public DashboardSummaryVisits(Long visits, Date visitTime){
		this.visits = visits;
		this.visitTime = visitTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public Date getVisitTime() {
		return visitTime;
	}

	public void setVisitTime(Date visitTime) {
		this.visitTime = visitTime;
	}

	public Long getVisits() {
		return visits;
	}

	public void setVisits(Long visits) {
		this.visits = visits;
	}

	public String getFormattedTime() {
		return formattedTime;
	}

	public void setFormattedTime(String formattedTime) {
		this.formattedTime = formattedTime;
	}
	
	

}
