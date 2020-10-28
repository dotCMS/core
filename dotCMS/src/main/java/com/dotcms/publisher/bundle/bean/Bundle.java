package com.dotcms.publisher.bundle.bean;

import java.io.File;
import java.util.Date;
import com.dotmarketing.util.ConfigUtils;
import io.vavr.control.Try;

public class Bundle {
	private String id;
	private String name;
	private Date publishDate;
	private Date expireDate;
	private String owner;
	private Integer operation;
	private boolean forcePush;
	private String filterKey;

	public Bundle() {}

	public Bundle(final String name,final Date publishDate,final Date expireDate,final String owner) {
		this.name = name;
		this.publishDate = publishDate;
		this.expireDate = expireDate;
		this.owner = owner;
	}

	public Bundle(final String name,final Date publishDate,final Date expireDate,final String owner,final boolean forcePush,final String filterKey) {
		this.name = name;
		this.publishDate = publishDate;
		this.expireDate = expireDate;
		this.owner = owner;
		this.forcePush = forcePush;
		this.filterKey = filterKey;
	}

	public String getId() {
		return id;
	}
	public void setId(final String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public Date getPublishDate() {
		return publishDate;
	}
	public void setPublishDate(final Date publishDate) {
		this.publishDate = publishDate;
	}
	public Date getExpireDate() {
		return expireDate;
	}
	public void setExpireDate(final Date expireDate) {
		this.expireDate = expireDate;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(final String owner) {
		this.owner = owner;
	}

	public Integer getOperation() {
		return operation;
	}

	public void setOperation(final Integer operation) {
		this.operation = operation;
	}

	public boolean isForcePush() {
		return forcePush;
	}

	public void setForcePush(final boolean forcePush) {
		this.forcePush = forcePush;
	}

	public String getFilterKey() {
		return filterKey;
	}
	public void setFilterKey(final String filterKey) {
		this.filterKey = filterKey;
	}

	/**
	 * Checks if the bundle was already generated based on the id: BUNDLE_ID.tar.gz
	 * @return boolean - true if the bundle exists.
	 */
	public boolean bundleTgzExists() {
	    
	    return Try.of(()->new File(  ConfigUtils.getBundlePath() + File.separator + id + ".tar.gz" ).exists()).getOrElse(false);
	    
	    
	}
	
	
	
}
