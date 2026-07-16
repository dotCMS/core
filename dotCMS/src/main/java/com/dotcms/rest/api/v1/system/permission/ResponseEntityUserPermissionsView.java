package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.Pagination;
import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity wrapper for user permissions endpoint with pagination support.
 * Contains a user's individual role permissions organized by assets (hosts and folders).
 * Each asset includes detailed permission information such as the permission assignments,
 * whether the user can edit those permissions, and whether permissions are inherited.
 *
 * @author hassandotcms
 * @since 24.01
 * @see UserPermissionsView
 */
public class ResponseEntityUserPermissionsView extends ResponseEntityView<UserPermissionsView> {

    /**
     * Constructs a new response wrapper for user permissions with pagination.
     *
     * @param entity The user permissions data
     * @param pagination Pagination metadata
     */
    public ResponseEntityUserPermissionsView(final UserPermissionsView entity, final Pagination pagination) {
        super(entity, pagination);
    }
}
