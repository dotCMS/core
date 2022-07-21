package com.dotcms.model.contenttype;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = GetContentTypesRequest.class)
public interface AbstractGetContentTypesRequest {
    String filter();
    int page();
    int perPage();
    String orderBy();
    String direction();
    String type();
    String host();

}
