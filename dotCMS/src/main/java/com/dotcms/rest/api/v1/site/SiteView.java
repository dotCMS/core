package com.dotcms.rest.api.v1.site;

import java.util.Date;
import java.util.List;

public class SiteView {

    private final String identifier;

    private final String inode;

    private final String aliases;

    private final String siteName;

    private final String tagStorage;

    private final String siteThumbnail;

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

    private final List<SimpleSiteVarView> variables;

    private SiteView(Builder builder) {
        identifier = builder.identifier;
        inode = builder.inode;
        aliases = builder.aliases;
        siteName = builder.siteName;
        tagStorage = builder.tagStorage;
        siteThumbnail = builder.siteThumbnail;
        runDashboard = builder.runDashboard;
        keywords = builder.keywords;
        description = builder.description;
        googleMap = builder.googleMap;
        googleAnalytics = builder.googleAnalytics;
        addThis = builder.addThis;
        proxyUrlForEditMode = builder.proxyUrlForEditMode;
        embeddedDashboard = builder.embeddedDashboard;
        languageId = builder.languageId;
        isSystemHost = builder.isSystemHost;
        isDefault = builder.isDefault;
        isArchived = builder.isArchived;
        isLive = builder.isLive;
        isLocked = builder.isLocked;
        isWorking = builder.isWorking;
        modDate = builder.modDate;
        modUser = builder.modUser;
        variables = builder.variables;
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

    public String getSiteName() {
        return siteName;
    }

    public String getTagStorage() {
        return tagStorage;
    }

    public String getSiteThumbnail() {
        return siteThumbnail;
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

    public List<SimpleSiteVarView> getVariables() {
        return variables;
    }

    public static final class Builder {
        private String identifier;
        private String inode;
        private String aliases;
        private String siteName;
        private String tagStorage;
        private String siteThumbnail;
        private boolean runDashboard;
        private String keywords;
        private String description;
        private String googleMap;
        private String googleAnalytics;
        private String addThis;
        private String proxyUrlForEditMode;
        private String embeddedDashboard;
        private long languageId;
        private boolean isSystemHost;
        private boolean isDefault;
        private boolean isArchived;
        private boolean isLive;
        private boolean isLocked;
        private boolean isWorking;
        private Date modDate;
        private String modUser;
        private List<SimpleSiteVarView> variables;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withIdentifier(String val) {
            identifier = val;
            return this;
        }

        public Builder withInode(String val) {
            inode = val;
            return this;
        }

        public Builder withAliases(String val) {
            aliases = val;
            return this;
        }

        public Builder withSiteName(String val) {
            siteName = val;
            return this;
        }

        public Builder withTagStorage(String val) {
            tagStorage = val;
            return this;
        }

        public Builder withSiteThumbnail(String val) {
            siteThumbnail = val;
            return this;
        }

        public Builder withRunDashboard(boolean val) {
            runDashboard = val;
            return this;
        }

        public Builder withKeywords(String val) {
            keywords = val;
            return this;
        }

        public Builder withDescription(String val) {
            description = val;
            return this;
        }

        public Builder withGoogleMap(String val) {
            googleMap = val;
            return this;
        }

        public Builder withGoogleAnalytics(String val) {
            googleAnalytics = val;
            return this;
        }

        public Builder withAddThis(String val) {
            addThis = val;
            return this;
        }

        public Builder withProxyUrlForEditMode(String val) {
            proxyUrlForEditMode = val;
            return this;
        }

        public Builder withEmbeddedDashboard(String val) {
            embeddedDashboard = val;
            return this;
        }

        public Builder withLanguageId(long val) {
            languageId = val;
            return this;
        }

        public Builder withIsSystemHost(boolean val) {
            isSystemHost = val;
            return this;
        }

        public Builder withIsDefault(boolean val) {
            isDefault = val;
            return this;
        }

        public Builder withIsArchived(boolean val) {
            isArchived = val;
            return this;
        }

        public Builder withIsLive(boolean val) {
            isLive = val;
            return this;
        }

        public Builder withIsLocked(boolean val) {
            isLocked = val;
            return this;
        }

        public Builder withIsWorking(boolean val) {
            isWorking = val;
            return this;
        }

        public Builder withModDate(Date val) {
            modDate = val;
            return this;
        }

        public Builder withModUser(String val) {
            modUser = val;
            return this;
        }

        public Builder withVariables(List<SimpleSiteVarView> val) {
            variables = val;
            return this;
        }

        public SiteView build() {
            return new SiteView(this);
        }
    }
}
