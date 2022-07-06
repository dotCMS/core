package com.dotcms.model;


import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.List;
;

@ValueType
@Value.Immutable
@JsonDeserialize(as = APIResponse.class)
public interface AbstractAPIResponse<T> {
    List<String> errors();
    T entity();
    List<String> messages();
    HashMap<String,String> i18nMessagesMap();
}
