package com.dotcms.model.authentication;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = APITokenRequest.class)
public interface AbstractAPITokenRequest {
    String user();
    String password();
    int expirationDays();
}
