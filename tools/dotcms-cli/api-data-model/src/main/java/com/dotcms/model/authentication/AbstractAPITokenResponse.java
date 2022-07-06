package com.dotcms.model.authentication;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.authentication.APITokenResponse;
import com.dotcms.model.authentication.TokenEntity;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.model.AbstractAPIResponse;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = APITokenResponse.class)
public interface AbstractAPITokenResponse extends AbstractAPIResponse<TokenEntity> {
}
