package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderDetail.class)
@JsonDeserialize(as = FolderDetail.class)
@Schema(description = "Folder configuration details for creation and updates")
public interface AbstractFolderDetail {

    @Nullable
    @JsonProperty("title")
    @Schema(
        description = "Display title for the folder",
        example = "My Documents Folder",
        requiredMode = RequiredMode.REQUIRED
    )
    String title();

    @Nullable
    @JsonProperty("sortOrder")
    @Schema(
        description = "Sort order for the folder within its parent directory",
        example = "100",
        requiredMode = RequiredMode.NOT_REQUIRED
    )
    Integer sortOrder();

    @Nullable
    @JsonProperty("showOnMenu")
    @Schema(
        description = "Whether the folder should be visible in navigation menus",
        example = "true",
        requiredMode = RequiredMode.NOT_REQUIRED
    )
    Boolean showOnMenu();

    @Nullable
    @JsonProperty("fileMasks")
    @Schema(
        description = "List of file patterns that are allowed in this folder (e.g., *.jpg, *.pdf)",
        example = "[\"*.jpg\", \"*.png\", \"*.pdf\"]",
        requiredMode = RequiredMode.NOT_REQUIRED
    )
    List<String> fileMasks();

    @Nullable
    @JsonProperty("defaultAssetType")
    @Schema(
        description = "Default asset type for files uploaded to this folder",
        example = "FileAsset",
        requiredMode = RequiredMode.NOT_REQUIRED
    )
    String defaultAssetType();

}
