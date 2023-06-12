package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@ValueType
@Value.Immutable
@JsonDeserialize(as = SearchByPathRequest.class)
public interface AbstractSearchByPathRequest {

    String assetPath();

    Optional<String> language();

    Optional<Boolean> live();
}
