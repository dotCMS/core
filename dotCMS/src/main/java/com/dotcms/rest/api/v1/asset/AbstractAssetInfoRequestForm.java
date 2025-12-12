package com.dotcms.rest.api.v1.asset;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetInfoRequestForm.class)
@JsonDeserialize(as = AssetInfoRequestForm.class)
@Schema(description = "Asset information request form")
public interface AbstractAssetInfoRequestForm {

    @JsonProperty("assetPath")
    @Schema(
        description = "Full path to the asset including site and folder structure.",
        example = "//demo.dotcms.com/my-new-folder/important-presentation.ppt",
        requiredMode = RequiredMode.REQUIRED
    )
    String assetPath();

}
