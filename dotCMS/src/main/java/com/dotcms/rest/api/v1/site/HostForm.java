package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form to create a host
 * @author jsanca
 */
public class HostForm {

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

    @JsonCreator
    public HostForm(@JsonProperty("aliases")    final String aliases,
                    @JsonProperty("hostName")   final String hostName,
                    @JsonProperty("tagStorage") final String tagStorage,
                    @JsonProperty("hostThumbnail") final String hostThumbnail,
                    @JsonProperty("runDashboard")  final boolean runDashboard,
                    @JsonProperty("keywords")      final String keywords,
                    @JsonProperty("description")   final String description,
                    @JsonProperty("googleMap")     final String googleMap,
                    @JsonProperty("googleAnalytics") final String googleAnalytics,
                    @JsonProperty("addThis")         final String addThis,
                    @JsonProperty("proxyUrlForEditMode") final String proxyUrlForEditMode,
                    @JsonProperty("embeddedDashboard")   final String embeddedDashboard,
                    @JsonProperty("languageId")          final long   languageId) {

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
        this.languageId        = languageId;
    }


    public long getLanguageId() {
        return languageId;
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

    @Override
    public String toString() {
        return "HostForm{" +
                "aliases='" + aliases + '\'' +
                ", hostName='" + hostName + '\'' +
                ", tagStorage='" + tagStorage + '\'' +
                ", hostThumbnail='" + hostThumbnail + '\'' +
                ", runDashboard=" + runDashboard +
                ", keywords=" + keywords +
                ", description=" + description +
                ", googleMap=" + googleMap +
                ", googleAnalytics=" + googleAnalytics +
                ", addThis=" + addThis +
                ", proxyUrlForEditMode=" + proxyUrlForEditMode +
                ", embeddedDashboard='" + embeddedDashboard + '\'' +
                '}';
    }
}
