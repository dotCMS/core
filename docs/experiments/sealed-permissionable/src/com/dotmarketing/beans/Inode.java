package com.dotmarketing.beans;

import com.dotmarketing.business.Permissionable;

/**
 * Gives up and reopens the branch — Category, Field, WebAsset and UserComment all extend the real
 * one. Note this does NOT break exhaustiveness downstream: every subclass of Inode is still an
 * Inode, so a single `case Inode` covers all of them.
 */
public non-sealed class Inode implements Permissionable {

    public String getPermissionType() {
        return "Inode";
    }
}
