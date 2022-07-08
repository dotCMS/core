package com.dotcms.model.authentication;

import com.dotcms.model.AbstractResponseEntityView;
import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = TokenResponse.class)
public interface AbstractTokenResponse extends AbstractResponseEntityView<TokenEntity> {
}
