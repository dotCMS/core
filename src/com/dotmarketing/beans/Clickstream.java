package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Clickstream implements Serializable {
    
    private static final long serialVersionUID = 1L;
 	@SuppressWarnings("unchecked")
	private List<ClickstreamRequest> clickstreamRequests = Collections.synchronizedList(new ArrayList<ClickstreamRequest> ());
 	private List<Clickstream404> clickstream404s = Collections.synchronizedList(new ArrayList<Clickstream404> ());
    private long clickstreamId;
    private Map attributes = new HashMap();
    private String hostname;
    private String userId;
    private String cookieId;
    private String remoteAddress;
    private String remoteHostname;
    private String initialReferrer;
    private String userAgent;
    private Date start = new Date();
    private Date lastRequest = new Date();
    private boolean bot = false;
    private Date lastSaved = new Date();
    private String hostId;
    private String firstPageId ;
    private String lastPageId ;
    private String operatingSystem ;
    private String browserName ;
    private String browserVersion;
    private boolean mobileDevice;

    
    private int numberOfRequests = 0;
    
    public int getNumberOfRequests() {
		return numberOfRequests;
	}

	public void setNumberOfRequests(int numberOfRequests) {
		this.numberOfRequests = numberOfRequests;
	}

	public Clickstream() {
    	
    }

    /**
     * Gets an attribute for this clickstream.
     *
     * @param name
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Gets the attribute names for this clickstream.
     */
    public Set getAttributeNames() {
        return attributes.keySet();
    }

    /**
     * Sets an attribute for this clickstream.
     *
     * @param name
     * @param value
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Returns the host name that this clickstream relates to.
     *
     * @return the host name that the user clicked through
     */
    public String getHostname() {
      return hostname;
    }

    /**
     * Returns the bot status.
     *
     * @return true if the client is bot or spider
     */
    public boolean isBot() {
        return bot;
    }

     /**
     * The URL of the initial referer. This is useful for determining
     * how the user entered the site.
     *
     * @return the URL of the initial referer
     */
    public String getInitialReferrer() {
        return initialReferrer;
    }

    /**
     * Returns the Date when the clickstream began.
     *
     * @return the Date when the clickstream began
     */
    public Date getStart() {
        return start;
    }

    /**
     * Returns the last Date that the clickstream was modified.
     *
     * @return the last Date that the clickstream was modified
     */
    public Date getLastRequest() {
        return lastRequest;
    }

    /**
     * Returns the actual List of ClickstreamRequest objects.
     *
     * @return the actual List of ClickstreamRequest objects
     */


    /**
	 * @return Returns the cookie.
	 */
	public String getCookieId() {
		return cookieId;
	}
	/**
	 * @param cookie The cookie to set.
	 */
	public void setCookieId(String cookieId) {
		this.cookieId = cookieId;
	}
	/**
	 * @return Returns the id.
	 */
	public long getClickstreamId() {
		return clickstreamId;
	}
	/**
	 * @param id The id to set.
	 */
	public void setClickstreamId(long clickstreamId) {
		this.clickstreamId = clickstreamId;
	}
	/**
	 * @return Returns the userId.
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId The userId to set.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @param bot The bot to set.
	 */
	public void setBot(boolean bot) {
		this.bot = bot;
	}
	/**
	 * @param clickstreamRequests The clickstreamRequests to set.
	 */
	public void setClickstreamRequests(List<ClickstreamRequest>  clickstreamRequests) {
		this.clickstreamRequests = clickstreamRequests;
	}
	/**
	 * @param clickstreamRequests The clickstreamRequests to add.
	 */
	public void addClickstreamRequest(ClickstreamRequest clickstreamRequest) {
		clickstreamRequests.add(clickstreamRequest);
	}
	/**
	 * @param hostname The hostname to set.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	/**
	 * @param initialReferrer The initialReferrer to set.
	 */
	public void setInitialReferrer(String initialReferrer) {
		if ((initialReferrer != null) && (255 < initialReferrer.length())) {
			initialReferrer = initialReferrer.substring(0,254);
		}
		this.initialReferrer = initialReferrer;
	}
	/**
	 * @param lastRequest The lastRequest to set.
	 */
	public void setLastRequest(Date lastRequest) {
		this.lastRequest = lastRequest;
	}
	/**
	 * @param start The start to set.
	 */
	public void setStart(Date start) {
		this.start = start;
	}
	
	/**
	 * @return Returns the clickstreamRequests.
	 */
	public List<ClickstreamRequest> getClickstreamRequests() {
		return clickstreamRequests;
	}




	/**
	 * @return Returns the userAgent.
	 */
	public String getUserAgent() {
		return userAgent;
	}
	/**
	 * @param userAgent The userAgent to set.
	 */
	public void setUserAgent(String userAgent) {
		if(userAgent!=null && userAgent.length()>255){
			userAgent = userAgent.substring(0,254);
		}
		this.userAgent = userAgent;
	}
	/**
	 * @return Returns the remoteAddress.
	 */
	public String getRemoteAddress() {
		return remoteAddress;
	}
	/**
	 * @param remoteAddress The remoteAddress to set.
	 */
	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	/**
	 * Retrive the last date when this clickstream was saved in the database (this is only store in memory while the clickstream is in session)
	 * @return
	 */
	public Date getLastSaved() {
		return lastSaved;
	}

	/**
	 * Set the last date when this clickstream was saved in the database (this is only store in memory while the clickstream is in session)
	 * @return
	 */
	public void setLastSaved(Date lastSave) {
		this.lastSaved = lastSave;
	}

	public Map getAttributes() {
		return attributes;
	}

	public void setAttributes(Map attributes) {
		this.attributes = attributes;
	}



	public String getFirstPageId() {
		return firstPageId;
	}

	public void setFirstPageId(String firstPageId) {
		this.firstPageId = firstPageId;
	}

	public String getLastPageId() {
		return lastPageId;
	}

	public void setLastPageId(String lastPageId) {
		this.lastPageId = lastPageId;
	}

	public String getOperatingSystem() {
		return operatingSystem;
	}

	public void setOperatingSystem(String operatingSystem) {
		this.operatingSystem = operatingSystem;
	}

	public String getBrowserName() {
		return browserName;
	}

	public void setBrowserName(String browserName) {
		this.browserName = browserName;
	}

	public String getBrowserVersion() {
		return browserVersion;
	}

	public void setBrowserVersion(String browserVersion) {
		this.browserVersion = browserVersion;
	}

	public boolean isMobileDevice() {
		return mobileDevice;
	}

	public void setMobileDevice(boolean mobileDevice) {
		this.mobileDevice = mobileDevice;
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public String getRemoteHostname() {
		return remoteHostname;
	}

	public void setRemoteHostname(String remoteHostname) {
		this.remoteHostname = remoteHostname;
	}

	public List<Clickstream404> getClickstream404s() {
		return clickstream404s;
	}

	public void setClickstream404s(List<Clickstream404> clickstream404s) {
		this.clickstream404s = clickstream404s;
	}
	
	public void addClickstream404(Clickstream404 clickstream404) {
		clickstream404s.add(clickstream404);
	}
	
	
	
}


