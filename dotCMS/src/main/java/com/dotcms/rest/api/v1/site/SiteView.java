package com.dotcms.rest.api.v1.site;

import java.util.Date;

public class SiteView {

    private final String identifier;

    private final String inode;

    private final String aliases;

    private final String hostName;

    private final String tagStorage;

    private final String hostThumbnail;

    private final boolean runDashboard;

    private final String keywords;

    private final String description;

    private final String googleMap;

    private final String googleAnalytics;

    private final String addThis;

    private final String proxyUrlForEditMode;

    private final String embeddedDashboard;

    private final long   languageId;

    private final boolean isSystemHost;

    private final boolean isDefault;

    private final boolean isArchived;

    private final boolean isLive;

    private final boolean isLocked;

    private final boolean isWorking;

    private final Date modDate;

    private final  String modUser;

    public SiteView(String identifier, String inode, String aliases, String hostName, String tagStorage, String hostThumbnail,
                    boolean runDashboard, String keywords, String description, String googleMap, String googleAnalytics, String addThis,
                    String proxyUrlForEditMode, String embeddedDashboard, long languageId, boolean isSystemHost, boolean isDefault, boolean isArchived,
                    boolean isLive, boolean isLocked, boolean isWorking, Date modDate, String modUser) {

        this.identifier = identifier;
        this.inode = inode;
        this.aliases = aliases;
        this.hostName = hostName;
        this.tagStorage = tagStorage;
        this.hostThumbnail = hostThumbnail;
        this.runDashboard = runDashboard;
        this.keywords = keywords;
        this.description = description;
        this.googleMap = googleMap;
        this.googleAnalytics = googleAnalytics;
        this.addThis = addThis;
        this.proxyUrlForEditMode = proxyUrlForEditMode;
        this.embeddedDashboard = embeddedDashboard;
        this.languageId = languageId;
        this.isSystemHost = isSystemHost;
        this.isDefault = isDefault;
        this.isArchived = isArchived;
        this.isLive = isLive;
        this.isLocked = isLocked;
        this.isWorking = isWorking;
        this.modDate = modDate;
        this.modUser = modUser;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public String getAliases() {
        return aliases;
    }

    public String getHostName() {
        return hostName;
    }

    public String getTagStorage() {
        return tagStorage;
    }

    public String getHostThumbnail() {
        return hostThumbnail;
    }

    public boolean isRunDashboard() {
        return runDashboard;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getDescription() {
        return description;
    }

    public String getGoogleMap() {
        return googleMap;
    }

    public String getGoogleAnalytics() {
        return googleAnalytics;
    }

    public String getAddThis() {
        return addThis;
    }

    public String getProxyUrlForEditMode() {
        return proxyUrlForEditMode;
    }

    public String getEmbeddedDashboard() {
        return embeddedDashboard;
    }

    public long getLanguageId() {
        return languageId;
    }

    public boolean isSystemHost() {
        return isSystemHost;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public boolean isLive() {
        return isLive;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public Date getModDate() {
        return modDate;
    }

    public String getModUser() {
        return modUser;
    }
}
