package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.Date;


/**
 * A small class that captures the most important info from the HttpServletRequest for each "click".
 * See the documentation for HttpServletRequest for more info about each method here.
 *
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody</a>
 */
public class ClickstreamRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private long clickstreamRequestId;
    private long clickstreamId;
    private String protocol;
    private String serverName;
    private int serverPort;
    private String requestURI;
    private String queryString;
    private String remoteUser;
    private long languageId;
    private int requestOrder;
    private Date timestamp;
    private String hostId;
    private String associatedIdentifier;
    
    
    public ClickstreamRequest(){
    	
    }
    
/*    public ClickstreamRequest(HttpServletRequest request, Date timestamp) {
        protocol = request.getProtocol();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        requestURI = request.getRequestURI();
        queryString = request.getQueryString();
        remoteUser = request.getRemoteUser();
        this.timestamp = timestamp;
    }
*/
    
    public String getProtocol() {
        return protocol;
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public String getHostId(){
    	return hostId;
    }

    /**
     * Returns a string representation of the HTTP request being tracked.
     * Example: <b>www.opensymphony.com/some/path.jsp?arg1=foo&arg2=bar</b>
     *
     * @return  a string representation of the HTTP request being tracked.
     */
    public String toString() {
        return serverName +
                (serverPort != 80 ? ":" + serverPort : "") +
                requestURI +
                (queryString != null ? "?" + queryString : "");
    }
	/**
	 * @return Returns the id.
	 */
	public long getClickstreamRequestId() {
		return clickstreamRequestId;
	}
	/**
	 * @param id The id to set.
	 */
	public void setClickstreamRequestId(long clickstreamRequestId) {
		this.clickstreamRequestId = clickstreamRequestId;
	}
	/**
	 * @param protocol The protocol to set.
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	/**
	 * @param queryString The queryString to set.
	 */
	public void setQueryString(String queryString) {
	        if(queryString != null && queryString.length() > 1024){
	        	queryString = queryString.substring(0,1023);
	        }
		this.queryString = queryString;
	}
	/**
	 * @param remoteUser The remoteUser to set.
	 */
	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}
	/**
	 * @param requestURI The requestURI to set.
	 */
	public void setRequestURI(String requestURI) {
	        if(requestURI != null && requestURI.length() > 255){
		    requestURI.substring(0,254);
	        }
		this.requestURI = requestURI;
	}
	/**
	 * @param serverName The serverName to set.
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	/**
	 * @param serverPort The serverPort to set.
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	/**
	 * @param timestamp The timestamp to set.
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * @return Returns the clickstreamId.
	 */
	public long getClickstreamId() {
		return clickstreamId;
	}
	/**
	 * @param clickstreamId The clickstreamId to set.
	 */
	public void setClickstreamId(long clickstreamId) {
		this.clickstreamId = clickstreamId;
	}
	
	public int getRequestOrder(){
		return requestOrder;
	}
	
	public void setRequestOrder(int requestOrder) {
		this.requestOrder = requestOrder;
	}

	/**
	 * @return Returns the languageId.
	 */
	public long getLanguageId() {
		return languageId;
	}
	
    public void setHostId(String hostInode){
    	this.hostId = hostInode;
    }
	/**
	 * @param languageId The languageId to set.
	 */
	public void setLanguageId(long languageId) {
		this.languageId = languageId;
	}

	public String getAssociatedIdentifier() {
		return associatedIdentifier;
	}

	public void setAssociatedIdentifier(String associatedIdentifier) {
		this.associatedIdentifier = associatedIdentifier;
	}
}