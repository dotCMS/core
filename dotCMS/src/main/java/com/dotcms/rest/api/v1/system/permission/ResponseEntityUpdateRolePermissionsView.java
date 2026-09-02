package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity view for PUT /api/v1/permissions/role/{roleId}/asset/{assetId} endpoint.
 *
 * <p>Response structure:
 * <pre>{@code
 * {
 *   "entity": {
 *     "roleId": "role-abc-123",
 *     "roleName": "Content Editors",
 *     "asset": {
 *       "id": "folder-xyz-456",
 *       "type": "FOLDER",
 *       "name": "News",
 *       "path": "/demo.dotcms.com/content/news",
 *       "hostId": "host-demo-123",
 *       "canEditPermissions": true,
 *       "inheritsPermissions": false,
 *       "permissions": {
 *         "INDIVIDUAL": ["READ", "WRITE", "PUBLISH"],
 *         "FOLDER": ["READ", "WRITE"],
 *         "CONTENT": ["READ", "WRITE", "PUBLISH"],
 *         "PAGE": ["READ", "WRITE", "PUBLISH"]
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * @author dotCMS
 * @since 24.01
 */
public class ResponseEntityUpdateRolePermissionsView extends ResponseEntityView<UpdateRolePermissionsView> {

    /**
     * Creates a new ResponseEntityUpdateRolePermissionsView.
     *
     * @param entity UpdateRolePermissionsView containing roleId, roleName, and asset data with updated permissions
     */
    public ResponseEntityUpdateRolePermissionsView(final UpdateRolePermissionsView entity) {
        super(entity);
    }
}
