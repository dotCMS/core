package com.dotmarketing.portlets.contentlet.model;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.beans.VersionInfo;
import com.liferay.portal.model.User;

public class ContentletVersionInfo implements Serializable {
    private String identifier;
    private boolean deleted;
    private String lockedBy;
    private Date lockedOn;
    
    public String getLockedBy() {
        return lockedBy;
    }
    public void setLockedBy(String userId) {
        this.lockedBy = userId;
    }
    public Date getLockedOn() {
        return lockedOn;
    }
    public void setLockedOn(Date lockedOn) {
        this.lockedOn = lockedOn;
    }
    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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
