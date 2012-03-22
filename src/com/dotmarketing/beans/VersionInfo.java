package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.Date;

import com.liferay.portal.model.User;

public class VersionInfo implements Serializable {
	private static final long serialVersionUID = 241933896664122728L;
	private String identifier;
	private String liveInode;
	private String workingInode;
	private String lockedBy;
	private Date lockedOn;
	private boolean deleted;
	
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getLiveInode() {
		return liveInode;
	}
	public void setLiveInode(String liveInode) {
		this.liveInode = liveInode;
	}
	public String getWorkingInode() {
		return workingInode;
	}
	public void setWorkingInode(String workingInode) {
		this.workingInode = workingInode;
	}
	public boolean isDeleted() {
		return deleted;
	}
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
    public String getLockedBy() {
        return lockedBy;
    }
    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }
    public Date getLockedOn() {
        return lockedOn;
    }
    public void setLockedOn(Date lockedOn) {
        this.lockedOn = lockedOn;
    }
    public boolean isLocked() {
        return lockedBy!=null;
    }
    public void setLocked(String userId) {
        lockedOn=new Date();
        lockedBy=userId;
    }
    public void unLock() {
        lockedBy=null;
        lockedOn=new Date();
    }
}
