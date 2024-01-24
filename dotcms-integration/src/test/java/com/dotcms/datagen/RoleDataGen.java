package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jonathan Gamba 2019-04-04
 */
public class RoleDataGen extends AbstractDataGen<Role> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "testName" + currentTime;
    private String key = "testKey" + currentTime;
    private String description = "testDescription" + currentTime;
    private String parent;
    private Boolean editUsers = Boolean.TRUE;
    private Boolean editPermissions = Boolean.TRUE;
    private Boolean editLayouts = Boolean.TRUE;
    private List<Layout> layouts = new ArrayList<>();

    @SuppressWarnings("unused")
    /**
     * @param parent Identifier of parent role
     */
    public RoleDataGen parent(final String parent) {
        this.parent = parent;
        return this;
    }

    @SuppressWarnings("unused")
    public RoleDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public RoleDataGen key(final String key) {
        this.key = key;
        return this;
    }

    @SuppressWarnings("unused")
    public RoleDataGen description(final String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public RoleDataGen editUsers(final Boolean editUsers) {
        this.editUsers = editUsers;
        return this;
    }

    @SuppressWarnings("unused")
    public RoleDataGen editPermissions(final Boolean editPermissions) {
        this.editPermissions = editPermissions;
        return this;
    }

    @SuppressWarnings("unused")
    public RoleDataGen editLayouts(final Boolean editLayouts) {
        this.editLayouts = editLayouts;
        return this;
    }

    @SuppressWarnings("unused")
    public RoleDataGen layout(final Layout... layouts) {
        this.layouts.addAll(Arrays.asList(layouts));
        return this;
    }

    @Override
    public Role next() {
        final Role role = new Role();
        role.setParent(parent);
        role.setName(name);
        role.setRoleKey(key);
        role.setDescription(description);
        role.setEditUsers(editUsers);
        role.setEditPermissions(editPermissions);
        role.setEditLayouts(editLayouts);

        return role;
    }

    @WrapInTransaction
    @Override
    public Role persist(final Role role) {
        try {
            final Role savedRole = APILocator.getRoleAPI().save(role);
             for(final Layout layout : layouts){
                APILocator.getRoleAPI().addLayoutToRole(layout, savedRole);
             }
            return savedRole;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist Role.", e);
        }
    }

    /**
     * Creates a new {@link Role} instance and persists it in DB
     *
     * @return A new Role instance persisted in DB
     */
    @Override
    public Role nextPersisted() {
        return persist(next());
    }

    public static void remove(final Role role) {
        remove(role, true);
    }

    @WrapInTransaction
    public static void remove(final Role role, final Boolean failSilently) {

        try {
            APILocator.getRoleAPI().delete(role);
        } catch (Exception e) {
            if (failSilently) {
                Logger.error(ContentTypeDataGen.class, "Unable to delete Role.", e);
            } else {
                throw new RuntimeException("Unable to delete Role.", e);
            }
        }
    }

}