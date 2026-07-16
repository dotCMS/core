package com.dotcms.rest.api.v1.asset;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Assets Request Form is a json representation of a request to get assets
 * The assetsPath is the uri to the asset, it can be a folder or a file.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetsRequestForm.class)
@JsonDeserialize(as = AssetsRequestForm.class)
@Schema(description = "Asset download request form with language and version options")
public interface AbstractAssetsRequestForm {
     @JsonProperty("assetPath")
     @Schema(
         description = "Full path to the asset (file or folder) including site and folder structure. Folders must end-up with `/`",
         example = "//demo.dotcms.com/application/containers/default/banner.vtl",
         requiredMode = RequiredMode.REQUIRED
     )
     String assetPath();

     @JsonProperty("language")
     @Value.Default
     @Schema(
         description = "Language identifier for the asset. Uses system default language if not specified",
         example = "en-US",
         defaultValue = "System default language"
     )
     default String language() {
         return APILocator.getLanguageAPI().getDefaultLanguage().toString();
     }

     @JsonProperty("live")
     @Value.Default
     @Schema(
         description = "Whether to retrieve the live version (true) or working version (false) of the asset",
         example = "false",
         defaultValue = "false"
     )
     default boolean live() { return false; }
}