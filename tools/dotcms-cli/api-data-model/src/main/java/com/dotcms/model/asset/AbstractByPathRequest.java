package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = ByPathRequest.class)
public interface AbstractByPathRequest {

    String assetPath();
}
