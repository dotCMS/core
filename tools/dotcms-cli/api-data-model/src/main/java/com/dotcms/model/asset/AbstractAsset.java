package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Asset.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAsset extends SimpleWebAsset {

    String sha256();

    Long size();

    boolean live();

    String lang();
}
