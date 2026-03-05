package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for role permissions endpoint.
 * Wraps the role permission data including roleId, roleName, and assets with permissions.
 *
 * @author dotCMS
 * @since 24.01
 */
public class ResponseEntityRolePermissionsView extends ResponseEntityView<RolePermissionsView> {

    /**
     * Creates a new response entity view for role permissions.
     *
     * @param entity RolePermissionsView containing roleId, roleName, and assets
     */
    public ResponseEntityRolePermissionsView(final RolePermissionsView entity) {
        super(entity);
    }
}
