package com.dotmarketing.beans;

import com.dotmarketing.business.Permissionable;

public final class Identifier implements Permissionable {
    public String getPermissionType() { return "Identifier"; }
}
