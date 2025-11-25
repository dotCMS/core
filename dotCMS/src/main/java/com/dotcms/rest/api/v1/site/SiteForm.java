package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Form to create a site
 * @author jsanca
 */
public class SiteForm {

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

    private final boolean isDefault;

    private final boolean forceExecution;

    private final List<SimpleSiteVariableForm> variables;

    @JsonCreator
    public SiteForm(@JsonProperty("aliases") final String aliases,
            @JsonProperty("siteName") final String siteName,
            @JsonProperty("tagStorage") final String tagStorage,
            @JsonProperty("siteThumbnail") final String siteThumbnail,
            @JsonProperty("runDashboard") final boolean runDashboard,
            @JsonProperty("keywords") final String keywords,
            @JsonProperty("description") final String description,
            @JsonProperty("googleMap") final String googleMap,
            @JsonProperty("googleAnalytics") final String googleAnalytics,
            @JsonProperty("addThis") final String addThis,
            @JsonProperty("proxyUrlForEditMode") final String proxyUrlForEditMode,
            @JsonProperty("embeddedDashboard") final String embeddedDashboard,
            @JsonProperty("languageId") final long languageId,
            @JsonProperty("identifier") final String identifier,
            @JsonProperty("inode") final String inode,
            @JsonProperty("default") final boolean isDefault,
            @JsonProperty("forceExecution") final boolean forceExecution,
            @JsonProperty("variables") List<SimpleSiteVariableForm> siteVariables) {

        this.aliases = aliases;
        this.siteName = siteName;
        this.tagStorage = tagStorage;
        this.siteThumbnail = siteThumbnail;
        this.runDashboard = runDashboard;
        this.keywords = keywords;
        this.description = description;
        this.googleMap = googleMap;
        this.googleAnalytics = googleAnalytics;
        this.addThis = addThis;
        this.proxyUrlForEditMode = proxyUrlForEditMode;
        this.embeddedDashboard = embeddedDashboard;
        this.languageId = languageId;
        this.identifier = identifier;
        this.inode = inode;
        this.isDefault = isDefault;
        this.forceExecution = forceExecution;
        this.variables = siteVariables;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public long getLanguageId() {
        return languageId;
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

    public String  getKeywords() {
        return keywords;
    }

    public String  getDescription() {
        return description;
    }

    public String  getGoogleMap() {
        return googleMap;
    }

    public String  getGoogleAnalytics() {
        return googleAnalytics;
    }

    public String  getAddThis() {
        return addThis;
    }

    public String  getProxyUrlForEditMode() {
        return proxyUrlForEditMode;
    }

    public String getEmbeddedDashboard() {
        return embeddedDashboard;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isForceExecution() {
        return forceExecution;
    }

    public List<SimpleSiteVariableForm> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "SiteForm{" +
                "aliases='" + aliases + '\'' +
                ", siteName='" + siteName + '\'' +
                ", tagStorage='" + tagStorage + '\'' +
                ", siteThumbnail='" + siteThumbnail + '\'' +
                ", runDashboard=" + runDashboard +
                ", keywords=" + keywords +
                ", description=" + description +
                ", googleMap=" + googleMap +
                ", googleAnalytics=" + googleAnalytics +
                ", addThis=" + addThis +
                ", proxyUrlForEditMode=" + proxyUrlForEditMode +
                ", default='" + isDefault + '\'' +
                ", embeddedDashboard='" + embeddedDashboard + '\'' +
                '}';
    }
}
