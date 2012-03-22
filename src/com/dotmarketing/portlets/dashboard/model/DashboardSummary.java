package com.dotmarketing.portlets.dashboard.model;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.dotmarketing.beans.Host;

public class DashboardSummary implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
	private long id;
	
	private DashboardSummaryPeriod summaryPeriod;
	
	private String hostId;
	
	private long visits;
	
	private long pageViews;
	
	private long uniqueVisits;
	
	private long newVisits;
	
	private Date avgTimeOnSite;
	
	private int bounceRate;
	
	private long directTraffic;
	
	private long referringSites;

	private long searchEngines;

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

	public long getVisits() {
		return visits;
	}

	public void setVisits(long visits) {
		this.visits = visits;
	}

	public long getPageViews() {
		return pageViews;
	}

	public void setPageViews(long pageViews) {
		this.pageViews = pageViews;
	}

	public long getUniqueVisits() {
		return uniqueVisits;
	}

	public void setUniqueVisits(long uniqueVisits) {
		this.uniqueVisits = uniqueVisits;
	}

	public long getNewVisits() {
		return newVisits;
	}

	public void setNewVisits(long newVisits) {
		this.newVisits = newVisits;
	}

	public Date getAvgTimeOnSite() {
		return avgTimeOnSite;
	}

	public void setAvgTimeOnSite(Date avgTimeOnSite) {
		this.avgTimeOnSite = avgTimeOnSite;
	}

	public int getBounceRate() {
		return bounceRate;
	}

	public void setBounceRate(int bounceRate) {
		this.bounceRate = bounceRate;
	}

	public long getDirectTraffic() {
		return directTraffic;
	}

	public void setDirectTraffic(long directTraffic) {
		this.directTraffic = directTraffic;
	}

	public long getReferringSites() {
		return referringSites;
	}

	public void setReferringSites(long referringSites) {
		this.referringSites = referringSites;
	}

	public long getSearchEngines() {
		return searchEngines;
	}

	public void setSearchEngines(long searchEngines) {
		this.searchEngines = searchEngines;
	}

	public boolean equals(Object object){
		boolean returnValue = false;
		if((object instanceof DashboardSummary)){
			DashboardSummary summary = (DashboardSummary) object;
			if(this.id == summary.getId() && 
					this.summaryPeriod.equals(summary.getSummaryPeriod())){
				returnValue = true;
			}
		}
		return returnValue;
	}
	
	public int hashCode(){
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	

	
}
