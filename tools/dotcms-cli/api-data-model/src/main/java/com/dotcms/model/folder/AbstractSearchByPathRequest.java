package com.dotcms.model.folder;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = SearchByPathRequest.class)
public interface AbstractSearchByPathRequest {

    String path();

}
