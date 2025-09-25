package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderDeletionRequestForm.class)
@JsonDeserialize(as = FolderDeletionRequestForm.class)
@Schema(description = "Request form for deleting a folder and all its contents permanently")
public interface AbstractFolderDeletionRequestForm extends AbstractAssetInfoRequestForm {

    @Override
    @JsonProperty("assetPath")
    @Schema(
        description = "Full path to the folder to be deleted permanently (must end with /)",
        example = "//demo.dotcms.com/old-projects/",
        requiredMode = RequiredMode.REQUIRED
    )
    String assetPath();
}