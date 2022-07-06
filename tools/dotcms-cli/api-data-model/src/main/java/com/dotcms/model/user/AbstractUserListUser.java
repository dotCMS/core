package com.dotcms.model.user;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.user.UserListUser;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = UserListUser.class)
public interface AbstractUserListUser {
    String id();
    String type();
    String name();
    String emailaddress();
}
