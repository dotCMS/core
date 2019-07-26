package com.dotcms.publisher.bundle.bean;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;

public class Bundle implements Permissionable, Serializable {
	private String id = new UUIDGenerator().uuid();
	private String name;
	private Date publishDate;
	private Date expireDate;
	private String owner;
	private Integer operation;
	private boolean forcePush;

	private static final List<PermissionSummary> acceptedPermissions = ImmutableList.of(new PermissionSummary("use","use-permission-description", PermissionAPI.PERMISSION_USE));
	public Bundle() {
	  
	  
	  
	}

	public Bundle(String name, Date publishDate, Date expireDate, String owner) {
		this(name, publishDate, expireDate, owner, false);
	}

	public Bundle(String name, Date publishDate, Date expireDate, String owner, boolean forcePush) {
		this.name = name;
		this.publishDate = publishDate;
		this.expireDate = expireDate;
		this.owner = owner;
		this.forcePush = forcePush;
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

	public Integer getOperation() {
		return operation;
	}

	public void setOperation(Integer operation) {
		this.operation = operation;
	}

	public boolean isForcePush() {
		return forcePush;
	}

	public void setForcePush(boolean forcePush) {
		this.forcePush = forcePush;
	}

  @Override
  public String getPermissionId() {
    return this.id;
  }

  @Override
  @JsonIgnore
  public Permissionable getParentPermissionable() throws DotDataException {
    return null;
  }
  @Override
  @JsonIgnore
  public String getPermissionType() {
    return this.getClass().getCanonicalName();
  }
  @Override
  @JsonIgnore
  public boolean isParentPermissionable() {
    return false;
  }
  @Override
  @JsonIgnore
  public List<PermissionSummary> acceptedPermissions() {

    return acceptedPermissions;
  }
}
