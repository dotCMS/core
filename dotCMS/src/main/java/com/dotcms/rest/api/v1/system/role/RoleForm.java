package com.dotcms.rest.api.v1.system.role;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Form to save a new role
 * @author jsanca
 */
@JsonDeserialize(builder = RoleForm.Builder.class)
public class RoleForm extends Validated {

    @NotNull
    private final String roleName;

    private final String roleKey;

    private final String parentRoleId;
    private final boolean canEditUsers;

    private final boolean canEditPermissions;
    private final boolean canEditLayouts;

    private final String description;

    private RoleForm(final RoleForm.Builder builder) {
        super();
        roleName = builder.roleName;
        roleKey = builder.roleKey;
        parentRoleId = builder.parentRoleId;
        canEditUsers = builder.canEditUsers;
        canEditPermissions = builder.canEditPermissions;
        canEditLayouts = builder.canEditLayouts;
        description = builder.description;
        checkValid();
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public String getParentRoleId() {
        return parentRoleId;
    }

    public boolean isCanEditUsers() {
        return canEditUsers;
    }

    public boolean isCanEditPermissions() {
        return canEditPermissions;
    }

    public boolean isCanEditLayouts() {
        return canEditLayouts;
    }

    public String getDescription() {
        return description;
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String roleName;

        @JsonProperty()
        private String roleKey;

        @JsonProperty()
        private String parentRoleId;

        @JsonProperty()
        private boolean canEditUsers;

        @JsonProperty()
        private  boolean canEditPermissions;

        @JsonProperty()
        private  boolean canEditLayouts;

        @JsonProperty()
        private  String description;

        public RoleForm.Builder roleName(final String roleName) {
            this.roleName = roleName;
            return this;
        }

        public RoleForm.Builder roleKey(final String roleKey) {
            this.roleKey = roleKey;
            return this;
        }

        public RoleForm.Builder parentRoleId(final String parentRoleId) {
            this.parentRoleId = parentRoleId;
            return this;
        }

        public RoleForm.Builder canEditUsers(final boolean canEditUsers) {
            this.canEditUsers = canEditUsers;
            return this;
        }

        public RoleForm.Builder canEditPermissions(final boolean canEditPermissions) {
            this.canEditPermissions = canEditPermissions;
            return this;
        }

        public RoleForm.Builder canEditLayouts(final boolean canEditLayouts) {
            this.canEditLayouts = canEditLayouts;
            return this;
        }

        public RoleForm.Builder description(final String description) {
            this.description = description;
            return this;
        }
        public RoleForm build() {
            return new RoleForm(this);
        }
    }
}

