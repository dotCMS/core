package com.dotmarketing.business;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.beans.Host;

public class TimeMachineSessionBean implements Serializable {

	private static final long serialVersionUID = -4258219080608591393L;

	public static final String SESSION_KEY = TimeMachineSessionBean.class.getName();
	
	private boolean active;

	private Host host;
	
	private Date date;
	
	private boolean notFoundGoOnMainSite;
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public boolean isNotFoundGoOnMainSite() {
		return notFoundGoOnMainSite;
	}

	public void setNotFoundGoOnMainSite(boolean notFoundGoOnMainSite) {
		this.notFoundGoOnMainSite = notFoundGoOnMainSite;
	}
	
}
