package com.dotcms.rest.api.v1.system.role;

import com.dotmarketing.business.Role;
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

    public RoleView(final Role role, final List<RoleView> roleChildren){
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
}
