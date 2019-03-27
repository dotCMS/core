package com.dotmarketing.portlets.common.bean;

public class CrumbTrailEntry {
	private String title;
	private String link;
	
	public CrumbTrailEntry(String title, String link) {
		this.title = title;
		this.link = link;
	}
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}
	/**
	 * @param link the link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}
}