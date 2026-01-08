package com.dotcms.rest.api.v1.system.permission;

/**
 * Permission inheritance mode for assets in the REST API.
 * Indicates whether an asset inherits permissions from its parent
 * or has its own individual permission assignments.
 *
 * @author hassandotcms
 * @since 24.01
 */
public enum InheritanceMode {

    /**
     * Asset inherits permissions from its parent permissionable.
     * Changes to parent permissions will affect this asset.
     */
    INHERITED,

    /**
     * Asset has its own individual permission assignments.
     * Permissions are independent of parent permissionable.
     */
    INDIVIDUAL
}
