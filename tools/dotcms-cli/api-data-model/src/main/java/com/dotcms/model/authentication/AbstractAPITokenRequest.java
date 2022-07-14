package com.dotcms.model.authentication;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = APITokenRequest.Builder.class)
public interface AbstractAPITokenRequest {
    String user();
    String password();

    @Nullable
    Integer expirationDays();
    @Nullable
    String label();
}
