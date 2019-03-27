package com.dotmarketing.portlets.dashboard.model;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class DashboardWorkStream implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
	private long id;
	
	private String inode;
	
	private String assetType;
	
	private String hostId;
	
	private String action;
	
	private String name;

	private String modUserId;
	
	private Date modDate;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getInode() {
		return inode;
	}

	public void setInode(String inode) {
		this.inode = inode;
	}

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModUserId() {
		return modUserId;
	}

	public void setModUserId(String modUserId) {
		this.modUserId = modUserId;
	}

	public Date getModDate() {
		return modDate;
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	
	public User getModUser() {
		User modUser = null;
		try {
			modUser = APILocator.getUserAPI().loadUserById(this.getModUserId(),APILocator.getUserAPI().getSystemUser(),false);
		} catch (NoSuchUserException e) {
			Logger.debug(this, e.getMessage());
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		return modUser;
	}
	
	public Host getHost() {
		Host host = null;
		try {
			host = APILocator.getHostAPI().find(this.getHostId(), APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		return host;
	}


}
