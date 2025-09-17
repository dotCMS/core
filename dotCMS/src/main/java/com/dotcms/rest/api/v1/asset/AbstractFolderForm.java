package com.dotcms.rest.api.v1.asset;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface AbstractFolderForm <T> {

    @JsonProperty("assetPath")
    String assetPath();

    @JsonProperty("data")
    T data();
}
