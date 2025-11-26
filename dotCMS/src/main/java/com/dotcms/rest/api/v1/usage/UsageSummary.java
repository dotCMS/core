package com.dotcms.rest.api.v1.usage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Summary of key business metrics for the usage dashboard
 */
public final class UsageSummary {

    @JsonProperty
    private final ContentMetrics contentMetrics;
    
    @JsonProperty
    private final SiteMetrics siteMetrics;
    
    @JsonProperty
    private final UserMetrics userMetrics;
    
    @JsonProperty
    private final SystemMetrics systemMetrics;
    
    @JsonProperty
    private final Instant lastUpdated;

    private UsageSummary(final Builder builder) {
        this.contentMetrics = builder.contentMetrics;
        this.siteMetrics = builder.siteMetrics;
        this.userMetrics = builder.userMetrics;
        this.systemMetrics = builder.systemMetrics;
        this.lastUpdated = builder.lastUpdated;
    }

    public ContentMetrics getContentMetrics() {
        return contentMetrics;
    }

    public SiteMetrics getSiteMetrics() {
        return siteMetrics;
    }

    public UserMetrics getUserMetrics() {
        return userMetrics;
    }

    public SystemMetrics getSystemMetrics() {
        return systemMetrics;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class ContentMetrics {
        @JsonProperty
        private final long totalContent;
        
        @JsonProperty
        private final long contentTypes;
        
        @JsonProperty
        private final long recentlyEdited;
        
        @JsonProperty
        private final long contentTypesWithWorkflows;
        
        @JsonProperty
        private final String lastContentEdited;

        public ContentMetrics(final long totalContent, final long contentTypes, final long recentlyEdited,
                             final long contentTypesWithWorkflows, final String lastContentEdited) {
            this.totalContent = totalContent;
            this.contentTypes = contentTypes;
            this.recentlyEdited = recentlyEdited;
            this.contentTypesWithWorkflows = contentTypesWithWorkflows;
            this.lastContentEdited = lastContentEdited;
        }

        public long getTotalContent() {
            return totalContent;
        }

        public long getContentTypes() {
            return contentTypes;
        }

        public long getRecentlyEdited() {
            return recentlyEdited;
        }

        public long getContentTypesWithWorkflows() {
            return contentTypesWithWorkflows;
        }

        public String getLastContentEdited() {
            return lastContentEdited;
        }
    }

    public static final class SiteMetrics {
        @JsonProperty
        private final long totalSites;
        
        @JsonProperty
        private final long activeSites;
        
        @JsonProperty
        private final long templates;
        
        @JsonProperty
        private final long siteAliases;

        public SiteMetrics(final long totalSites, final long activeSites, final long templates, final long siteAliases) {
            this.totalSites = totalSites;
            this.activeSites = activeSites;
            this.templates = templates;
            this.siteAliases = siteAliases;
        }

        public long getTotalSites() {
            return totalSites;
        }

        public long getActiveSites() {
            return activeSites;
        }

        public long getTemplates() {
            return templates;
        }

        public long getSiteAliases() {
            return siteAliases;
        }
    }

    public static final class UserMetrics {
        @JsonProperty
        private final long activeUsers;
        
        @JsonProperty
        private final long totalUsers;
        
        @JsonProperty
        private final long recentLogins;
        
        @JsonProperty
        private final String lastLogin;

        public UserMetrics(final long activeUsers, final long totalUsers, final long recentLogins, final String lastLogin) {
            this.activeUsers = activeUsers;
            this.totalUsers = totalUsers;
            this.recentLogins = recentLogins;
            this.lastLogin = lastLogin;
        }

        public long getActiveUsers() {
            return activeUsers;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getRecentLogins() {
            return recentLogins;
        }

        public String getLastLogin() {
            return lastLogin;
        }
    }

    public static final class SystemMetrics {
        @JsonProperty
        private final long languages;
        
        @JsonProperty
        private final long workflowSchemes;
        
        @JsonProperty
        private final long workflowSteps;
        
        @JsonProperty
        private final long liveContainers;
        
        @JsonProperty
        private final long builderTemplates;

        public SystemMetrics(final long languages, final long workflowSchemes, final long workflowSteps,
                            final long liveContainers, final long builderTemplates) {
            this.languages = languages;
            this.workflowSchemes = workflowSchemes;
            this.workflowSteps = workflowSteps;
            this.liveContainers = liveContainers;
            this.builderTemplates = builderTemplates;
        }

        public long getLanguages() {
            return languages;
        }

        public long getWorkflowSchemes() {
            return workflowSchemes;
        }

        public long getWorkflowSteps() {
            return workflowSteps;
        }

        public long getLiveContainers() {
            return liveContainers;
        }

        public long getBuilderTemplates() {
            return builderTemplates;
        }
    }

    public static final class Builder {
        private ContentMetrics contentMetrics;
        private SiteMetrics siteMetrics;
        private UserMetrics userMetrics;
        private SystemMetrics systemMetrics;
        private Instant lastUpdated;

        public Builder contentMetrics(final ContentMetrics contentMetrics) {
            this.contentMetrics = contentMetrics;
            return this;
        }

        public Builder siteMetrics(final SiteMetrics siteMetrics) {
            this.siteMetrics = siteMetrics;
            return this;
        }

        public Builder userMetrics(final UserMetrics userMetrics) {
            this.userMetrics = userMetrics;
            return this;
        }

        public Builder systemMetrics(final SystemMetrics systemMetrics) {
            this.systemMetrics = systemMetrics;
            return this;
        }

        public Builder lastUpdated(final Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public UsageSummary build() {
            return new UsageSummary(this);
        }
    }

}