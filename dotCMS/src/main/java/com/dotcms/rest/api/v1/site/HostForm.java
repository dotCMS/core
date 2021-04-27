package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HostForm {

    private final String aliases;

    private final String hostName;

    private final String tagStorage;

    private final String hostThumbnail;

    private final boolean runDashboard;

    private final boolean keywords;

    private final boolean description;

    private final boolean googleMap;

    private final boolean googleAnalytics;

    private final boolean addThis;

    private final boolean proxyUrlForEditMode;

    private final String embeddedDashboard;

    @JsonCreator
    public HostForm(@JsonProperty("aliases")    final String aliases,
                    @JsonProperty("hostName")   final String hostName,
                    @JsonProperty("tagStorage") final String tagStorage,
                    @JsonProperty("hostThumbnail") final String hostThumbnail,
                    @JsonProperty("runDashboard")  final boolean runDashboard,
                    @JsonProperty("keywords")      final boolean keywords,
                    @JsonProperty("description")   final boolean description,
                    @JsonProperty("googleMap")     final boolean googleMap,
                    @JsonProperty("googleAnalytics") final boolean googleAnalytics,
                    @JsonProperty("addThis")         final boolean addThis,
                    @JsonProperty("proxyUrlForEditMode") final boolean proxyUrlForEditMode,
                    @JsonProperty("embeddedDashboard")   final String embeddedDashboard) {

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

    public boolean isKeywords() {
        return keywords;
    }

    public boolean isDescription() {
        return description;
    }

    public boolean isGoogleMap() {
        return googleMap;
    }

    public boolean isGoogleAnalytics() {
        return googleAnalytics;
    }

    public boolean isAddThis() {
        return addThis;
    }

    public boolean isProxyUrlForEditMode() {
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
