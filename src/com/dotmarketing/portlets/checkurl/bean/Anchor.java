package com.dotmarketing.portlets.checkurl.bean;


/**
 * Represent the anchor tag in HTML ('<a ...></a>')
 * 
 * @author	Graziano Aliberti
 * @date	24/02/2012
 */
public class Anchor {
	
	private URL externalLink;
	private String title;
	private String internalLink;
	private boolean isInternal;
	
	public URL getExternalLink() {
		return externalLink;
	}
	
	public void setExternalLink(URL href) {
		this.externalLink = href;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getInternalLink() {
		return internalLink;
	}

	public void setInternalLink(String internalLink) {
		this.internalLink = internalLink;
	}

	public boolean isInternal() {
		return isInternal;
	}

	public void setInternal(boolean isInternal) {
		this.isInternal = isInternal;
	}
	
}
