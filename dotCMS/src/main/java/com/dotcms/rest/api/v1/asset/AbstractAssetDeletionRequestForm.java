package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetDeletionRequestForm.class)
@JsonDeserialize(as = AssetDeletionRequestForm.class)
@Schema(description = "Request form for deleting an asset permanently from the system")
public interface AbstractAssetDeletionRequestForm extends AbstractAssetInfoRequestForm {

    @Override
    @JsonProperty("assetPath")
    @Schema(
        description = "Full path to the asset (file) to be deleted permanently",
        example = "//demo.dotcms.com/uploads/old-document.pdf",
        requiredMode = RequiredMode.REQUIRED
    )
    String assetPath();
}