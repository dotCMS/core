package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.Date;


public class Clickstream404 implements Serializable {
    private static final long serialVersionUID = 1L;
    private long clickstream404Id;
    private String requestURI;
    private String queryString;
    private String refererURI;
    private String hostId;
    private String userId;
    private Date timestamp;
   
    
	public long getClickstream404Id() {
		return clickstream404Id;
	}
	public void setClickstream404Id(long clickstream404Id) {
		this.clickstream404Id = clickstream404Id;
	}
	public String getRequestURI() {
		return requestURI;
	}
	public void setRequestURI(String requestURI) {
		 if(requestURI != null && requestURI.length() > 255){
			    requestURI.substring(0,254);
		        }
			this.requestURI = requestURI;
	}
	public String getRefererURI() {
		return refererURI;
	}
	public void setRefererURI(String refererURI) {
		if ((refererURI != null) && (255 < refererURI.length())) {
			refererURI = refererURI.substring(0,254);
		}
		this.refererURI = refererURI;
	}
	public String getHostId() {
		return hostId;
	}
	public void setHostId(String hostId) {
		this.hostId = hostId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public String getQueryString() {
		return queryString;
	}
	public void setQueryString(String queryString) {
		  if(queryString != null && queryString.length() > 1024){
	        	queryString = queryString.substring(0,1023);
	        }
		this.queryString = queryString;
	}
    
    
}