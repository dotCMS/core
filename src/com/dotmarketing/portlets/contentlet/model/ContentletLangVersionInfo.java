package com.dotmarketing.portlets.contentlet.model;

import java.io.Serializable;

public class ContentletLangVersionInfo implements Serializable {
    private static final long serialVersionUID = -6331989711318344224L;
    
    private String identifier;
    private long lang;
    private String workingInode;
    private String liveInode;
    
    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
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
    @Override
    public boolean equals(Object obj) {
        ContentletLangVersionInfo cc = (ContentletLangVersionInfo)obj;
        return cc.getIdentifier().equals(identifier) && cc.getLang()==lang;
    }
    @Override
    public int hashCode() {
        return identifier.hashCode()+(int)lang*17;
    }
}
