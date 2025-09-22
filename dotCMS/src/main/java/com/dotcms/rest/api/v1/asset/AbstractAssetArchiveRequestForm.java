package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetArchiveRequestForm.class)
@JsonDeserialize(as = AssetArchiveRequestForm.class)
@Schema(description = "Request form for archiving an asset to make it inactive while preserving it")
public interface AbstractAssetArchiveRequestForm extends AbstractAssetInfoRequestForm {

    @Override
    @JsonProperty("assetPath")
    @Schema(
        description = "Full path to the asset (file) to be archived",
        example = "//demo.dotcms.com/temp/temp-presentation.pptx",
        requiredMode = RequiredMode.REQUIRED
    )
    String assetPath();
}