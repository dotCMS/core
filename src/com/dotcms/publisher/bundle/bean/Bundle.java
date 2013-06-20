package com.dotcms.publisher.bundle.bean;

import java.util.Date;

public class Bundle {
	private String id;
	private String name;
	private Date publishDate;
	private Date expireDate;
	private String owner;

	public Bundle() {}

	public Bundle(String name, Date publishDate, Date expireDate, String owner) {
		this.name = name;
		this.publishDate = publishDate;
		this.expireDate = expireDate;
		this.owner = owner;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getPublishDate() {
		return publishDate;
	}
	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}
	public Date getExpireDate() {
		return expireDate;
	}
	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}

}
