package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.api.v1.user.RestUser;
import com.dotmarketing.business.PermissionAPI;

import java.util.List;

public class PermissionView {

    /** persistent field */
    private long id;

    /** persistent field */
    private String inode;

    /** persistent field */
    private String roleId;

    /** persistent field */
    private PermissionAPI.Type permission;

    private boolean isBitPermission = false;

    private String type;

    public PermissionView(final long id,
                          final String inode,
                          final String roleId,
                          final PermissionAPI.Type permission,
                          final boolean isBitPermission,
                          final String type) {

        this.id = id;
        this.inode = inode;
        this.roleId = roleId;
        this.permission = permission;
        this.isBitPermission = isBitPermission;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public String getInode() {
        return inode;
    }

    public String getRoleId() {
        return roleId;
    }

    public PermissionAPI.Type getPermission() {
        return permission;
    }

    public boolean isBitPermission() {
        return isBitPermission;
    }

    public String getType() {
        return type;
    }

}
