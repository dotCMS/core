package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.Date;

/**
 * This bean manage the trackbackping object
 * @author Oswaldo Gallango
 * @version 1.0
 * @since 1.5
 */
public class Trackback implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private long id;
	private String assetIdentifier;
	private String title;
	private String excerpt;
	private String url;
	private String blogName;
	private Date trackDate = new Date();
	
	/**
	 * Get Asset Inode
	 * @return String
	 */
	public String getAssetIdentifier() {
		return assetIdentifier;
	}
	/**
	 * Set the asset inode
	 * @param assetIdentifier
	 */
	public void setAssetIdentifier(String assetIdentifier) {
		this.assetIdentifier = assetIdentifier;
	}
	/**
	 * Get Blog Name
	 * @return String
	 */
	public String getBlogName() {
		return blogName;
	}
	/**
	 * Set Blog Name
	 * @param blogName
	 */
	public void setBlogName(String blogName) {
		this.blogName = blogName;
	}
	/**
	 * Get Excerpt
	 * @return String
	 */
	public String getExcerpt() {
		return excerpt;
	}
	/**
	 * Set Excerpt
	 * @param excerpt
	 */
	public void setExcerpt(String excerpt) {
		this.excerpt = excerpt;
	}
	/**
	 * Get Id
	 * @return long
	 */
	public long getId() {
		return id;
	}
	/**
	 * Set id
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * Get Title
	 * @return String
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * Set title
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * Get Track Date
	 * @return Date
	 */
	public Date getTrackDate() {
		return trackDate;
	}
	/**
	 * Set Track Date
	 * @param trackDate
	 */
	public void setTrackDate(Date trackDate) {
		this.trackDate = trackDate;
	}
	/**
	 * Get url
	 * @return String
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * Set url
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	


}
