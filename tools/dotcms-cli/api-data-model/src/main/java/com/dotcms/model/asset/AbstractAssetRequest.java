package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = AssetRequest.class)
public interface AbstractAssetRequest {

    String assetPath();

    String language();

    boolean live();

}
