package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

/**
 * Typed response wrapper for the {@code DELETE /v1/roles/{roleId}/users} endpoint
 * (issue #36938).
 *
 * @author hassandotcms
 * @since Aug 2026
 */
public class ResponseEntityRoleUsersRemovalView extends ResponseEntityView<RoleUsersRemovalView> {

    public ResponseEntityRoleUsersRemovalView(final RoleUsersRemovalView entity) {
        super(entity);
    }
}
