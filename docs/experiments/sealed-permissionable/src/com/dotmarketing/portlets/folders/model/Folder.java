package com.dotmarketing.portlets.folders.model;

import com.dotmarketing.business.Permissionable;

public final class Folder implements Permissionable {
    public String getPermissionType() { return "Folder"; }
}
