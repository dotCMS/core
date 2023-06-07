package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileUploadDetail {

    @JsonCreator
    public FileUploadDetail(@JsonProperty("assetPath") final String assetPath,
            @JsonProperty("language") final String language,
            @JsonProperty("status") final Boolean live,
            @JsonProperty("createNewVersion") final Boolean createNewVersion) {
        this.assetPath = assetPath;
        this.language = language;
        this.live = live;
        this.createNewVersion = createNewVersion;
    }

    @JsonProperty("assetPath")
    private String assetPath;

    @JsonProperty("language")
    private String language;

    @JsonProperty("live")
    private Boolean live;

    @JsonProperty("createNewVersion")
    private Boolean createNewVersion;

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getCreateNewVersion() {
        return createNewVersion;
    }

    public void setCreateNewVersion(Boolean createNewVersion) {
        this.createNewVersion = createNewVersion;
    }

    public Boolean getLive() {
        return live;
    }

    public void setLive(Boolean live) {
        this.live = live;
    }
}
