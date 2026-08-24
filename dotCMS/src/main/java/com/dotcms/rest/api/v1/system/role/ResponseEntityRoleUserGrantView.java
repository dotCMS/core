package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

/**
 * Typed response wrapper for the {@code POST /v1/roles/{roleId}/users/{userId}} endpoint
 * (issue #36937).
 *
 * @author hassandotcms
 * @since Aug 2026
 */
public class ResponseEntityRoleUserGrantView extends ResponseEntityView<RoleUserGrantView> {

    public ResponseEntityRoleUserGrantView(final RoleUserGrantView entity) {
        super(entity);
    }
}
