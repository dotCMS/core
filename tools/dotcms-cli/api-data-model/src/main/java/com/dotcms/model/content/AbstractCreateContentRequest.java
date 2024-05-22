package com.dotcms.model.content;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = CreateContentRequest.class)
public interface AbstractCreateContentRequest {

    Contentlet contentlet();

}
