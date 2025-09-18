package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetLookupRequestForm.class)
@JsonDeserialize(as = AssetLookupRequestForm.class)
@Schema(description = "Request form for looking up asset information and metadata")
public interface AbstractAssetLookupRequestForm extends AbstractAssetInfoRequestForm {

    @Override
    @JsonProperty("assetPath")
    @Schema(
        description = "Full path to the asset (file or folder) to retrieve information for",
        example = "//demo.dotcms.com/documents/annual-report.pdf",
        requiredMode = RequiredMode.REQUIRED
    )
    String assetPath();
}