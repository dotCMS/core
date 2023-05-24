package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Assets Request Form is a json representation of a request to get assets
 * The assetsPath is the uri to the asset, it can be a folder or a file.
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetsRequestForm.class)
@JsonDeserialize(as = AssetsRequestForm.class)
public interface AbstractAssetsRequestForm {
     @JsonProperty("assetPath")
     String assetPath();
}
