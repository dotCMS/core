package com.dotcms.rest.api.v1.asset;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetInfoRequestForm.class)
@JsonDeserialize(as = AssetInfoRequestForm.class)
public interface AbstractAssetInfoRequestForm {

    @JsonProperty("assetPath")
    String assetPath();

}
