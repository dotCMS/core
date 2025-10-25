package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.api.v1.system.role.RoleView;
import com.dotmarketing.business.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Map;

public class UserView {

    private final User user;
    private final Role role;

    public UserView(final User user, final Role role) {
        this.user = user;
        this.role = role;
    }


    @JsonProperty("userID")
    public String getUserId() {
        return user.getUserId();
    }

    @JsonProperty("user")
    public Map<String, Object> getUserMap() {
        return Try.of(()->user.toMap()).getOrElse(new HashMap<>());
    }

    @JsonProperty("role")
    public RoleView getRole() {
        return new RoleView(role, null);
    }
}
