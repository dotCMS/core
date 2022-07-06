package com.dotcms.model.user;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.user.UserFilter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = UserFilter.class)
public interface AbstractUserFilter {
    String query();
}
