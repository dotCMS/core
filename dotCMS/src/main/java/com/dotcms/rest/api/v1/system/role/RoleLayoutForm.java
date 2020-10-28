package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Set;

@JsonDeserialize(builder = RoleLayoutForm.Builder.class)
public class RoleLayoutForm extends Validated {

    private final String roleId;
    private final Set<String> layoutIds;

    private RoleLayoutForm(final RoleLayoutForm.Builder builder) {
        super();
        roleId    = builder.roleId;
        layoutIds = builder.layoutIds;
        checkValid();
    }

    public String getRoleId() {
        return this.roleId;
    }

    public Set<String> getLayoutIds() {
        return this.layoutIds;
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String roleId;
        @JsonProperty(required = true)
        private Set<String> layoutIds;


        public RoleLayoutForm.Builder roleId(final String roleId) {
            this.roleId = roleId;
            return this;
        }

        public RoleLayoutForm.Builder layoutIds(final Set<String> layoutIds) {
            this.layoutIds = layoutIds;
            return this;
        }

        public RoleLayoutForm build() {
            return new RoleLayoutForm(this);
        }
    }
}

