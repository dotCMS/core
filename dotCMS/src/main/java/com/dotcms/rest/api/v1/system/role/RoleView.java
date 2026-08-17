package com.dotcms.rest.api.v1.system.role;

import com.dotmarketing.business.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * This class holds a view with all attributes of com.dotmarketing.business.Role
 * plus a list to hold the children roles
 *
 *
 */
public class RoleView {

    private final String id;
    private final String name;
    private final String description;
    private final String roleKey;
    private final String DBFQN;
    private final String FQN;
    private final String parent;
    private final boolean editPermissions;
    private final boolean editUsers;
    private final boolean editLayouts;
    private final boolean locked;
    private final boolean system;
    private final List<RoleView> roleChildren;
    private final int childCount;
    private final int userCount;

    public RoleView(final Role role, final List<RoleView> roleChildren, final int userCount){
        this.id = role.getId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.roleKey = role.getRoleKey();
        this.FQN = role.getFQN();
        this.DBFQN = role.getDBFQN();
        this.parent = role.getParent();
        this.editPermissions = role.isEditPermissions();
        this.editUsers = role.isEditUsers();
        this.editLayouts = role.isEditLayouts();
        this.locked = role.isLocked();
        this.system = role.isSystem();
        this.roleChildren = roleChildren;
        this.childCount = null != role.getRoleChildren() ? role.getRoleChildren().size() : 0;
        this.userCount = userCount;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public String getDBFQN() {
        return DBFQN;
    }

    public boolean isEditLayouts() {
        return editLayouts;
    }

    public String getParent() {
        return parent;
    }

    public String getFQN() {
        return FQN;
    }

    public boolean isEditPermissions() {
        return editPermissions;
    }

    public boolean isEditUsers() {
        return editUsers;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isSystem() {
        return system;
    }

    public List<RoleView> getRoleChildren() {
        return roleChildren;
    }

    @Schema(description = "Number of direct child roles, independent of children hydration",
            example = "3", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    public int getChildCount() {
        return childCount;
    }

    @Schema(description = "Number of users directly granted this role; grants inherited "
            + "through the role hierarchy are not included",
            example = "12", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    public int getUserCount() {
        return userCount;
    }
}
