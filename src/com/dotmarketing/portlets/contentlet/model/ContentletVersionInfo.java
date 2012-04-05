package com.dotmarketing.portlets.contentlet.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import com.dotmarketing.util.UtilMethods;

public class ContentletVersionInfo implements Serializable {
    private static final long serialVersionUID = 8952464908349482530L;
    private String identifier;
    private boolean deleted;
    private String lockedBy;
    private Timestamp lockedOn;
    private long lang;
    private String workingInode;
    private String liveInode;

    public long getLang() {
        return lang;
    }
    public void setLang(long lang) {
        this.lang = lang;
    }
    public String getWorkingInode() {
        return workingInode;
    }
    public void setWorkingInode(String workingInode) {
        this.workingInode = workingInode;
    }
    public String getLiveInode() {
        return liveInode;
    }
    public void setLiveInode(String liveInode) {
        this.liveInode = liveInode;
    }
    public String getLockedBy() {
        return lockedBy;
    }
    public void setLockedBy(String userId) {
        this.lockedBy = userId;
    }
    public Date getLockedOn() {
        return lockedOn;
    }
    public void setLockedOn(Timestamp lockedOn) {
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
        lockedOn=new Timestamp(System.currentTimeMillis());
        lockedBy=userId;
    }
    public void unLock() {
        lockedBy=null;
        lockedOn=new Timestamp(System.currentTimeMillis());
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ContentletVersionInfo) {
            ContentletVersionInfo vinfo=(ContentletVersionInfo)obj;
            return UtilMethods.isSet(this.identifier) && UtilMethods.isSet(vinfo.getIdentifier())
                    && this.identifier.equals(vinfo.getIdentifier()) && lang==vinfo.getLang();
        }
        else
            return false;
    }

    @Override
    public int hashCode() {
        int langx=(int)lang;
        return identifier.hashCode()+17*(langx+1);
    }
}
