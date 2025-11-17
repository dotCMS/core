package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.Pagination;

/**
 * Response entity wrapper for user permissions endpoint with pagination support.
 * Contains a user's individual role permissions organized by assets (hosts and folders).
 * Each asset includes detailed permission information such as the permission assignments,
 * whether the user can edit those permissions, and whether permissions are inherited.
 *
 * @see UserResource#getUserPermissions(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, String)
 * @see UserPermissions
 */
public class ResponseEntityUserPermissionsView extends ResponseEntityView<UserPermissions> {

    /**
     * Constructs a new response wrapper for user permissions with pagination.
     *
     * @param entity The user permissions data
     * @param pagination Pagination metadata
     */
    public ResponseEntityUserPermissionsView(final UserPermissions entity, final Pagination pagination) {
        super(entity, pagination);
    }
}