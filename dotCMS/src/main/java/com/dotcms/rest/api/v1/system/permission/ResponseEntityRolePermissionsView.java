package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Response wrapper for role permissions endpoint.
 * Wraps the role permission data including roleId, roleName, and assets with permissions.
 *
 * @author dotCMS
 * @since 24.01
 */
public class ResponseEntityRolePermissionsView extends ResponseEntityView<Map<String, Object>> {

    /**
     * Creates a new response entity view for role permissions.
     *
     * @param entity Map containing roleId, roleName, and assets array
     */
    public ResponseEntityRolePermissionsView(final Map<String, Object> entity) {
        super(entity);
    }
}
