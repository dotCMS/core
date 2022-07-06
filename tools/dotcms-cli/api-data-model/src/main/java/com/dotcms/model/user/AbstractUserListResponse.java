package com.dotcms.model.user;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.user.UserListResponse;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@ValueType
@Value.Immutable
@JsonDeserialize(as = UserListResponse.class)
public interface AbstractUserListResponse {
    List<AbstractUserListUser> data();
    int total();
}
