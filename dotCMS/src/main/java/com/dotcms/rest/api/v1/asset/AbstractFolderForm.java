package com.dotcms.rest.api.v1.asset;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(description = "Base folder form with asset path and folder details")
public interface AbstractFolderForm <T> {

    @JsonProperty("assetPath")
    @Schema(
        description = "Full path where the folder should be created or updated including site and folder structure. Must end with '/'",
        example = "//demo.dotcms.com/my-new-folder/",
        requiredMode = RequiredMode.REQUIRED
    )
    String assetPath();

    @JsonProperty("data")
    @Schema(description = "Folder configuration details", requiredMode = RequiredMode.REQUIRED)
    T data();
}
