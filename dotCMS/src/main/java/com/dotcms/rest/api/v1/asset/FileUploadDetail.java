package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileUploadDetail {

    @JsonCreator
    public FileUploadDetail(@JsonProperty("assetPath") final String assetPath,
            @JsonProperty("language") final String language,
            @JsonProperty("status") final String status,
            @JsonProperty("createNewVersion") final Boolean createNewVersion) {
        this.assetPath = assetPath;
        this.language = language;
        this.status = status;
        this.createNewVersion = createNewVersion;
    }

    @JsonProperty("assetPath")
    private String assetPath;

    @JsonProperty("language")
    private String language;

    @JsonProperty("status")
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getCreateNewVersion() {
        return createNewVersion;
    }

    public void setCreateNewVersion(Boolean createNewVersion) {
        this.createNewVersion = createNewVersion;
    }
}
