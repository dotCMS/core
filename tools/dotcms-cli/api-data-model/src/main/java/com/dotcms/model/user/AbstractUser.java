package com.dotcms.model.user;

import com.dotcms.model.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.model.annotation.ValueType;

import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = User.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractUser {
    String userId();
    String givenName();
    String email();
    String surname();
    String roleId();
}
