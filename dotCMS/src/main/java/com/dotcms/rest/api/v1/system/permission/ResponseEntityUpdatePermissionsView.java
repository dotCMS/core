package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Response entity view for the PUT /api/v1/permissions/{assetId} endpoint.
 *
 * <p>Response structure:
 * <pre>{@code
 * {
 *   "entity": {
 *     "message": "Permissions saved successfully",
 *     "permissionCount": 3,
 *     "inheritanceBroken": true,
 *     "asset": {
 *       "assetId": "asset-123",
 *       "assetType": "contentlet",
 *       "inheritanceMode": "INDIVIDUAL",
 *       "isParentPermissionable": false,
 *       "canEditPermissions": true,
 *       "canEdit": true,
 *       "parentAssetId": "parent-folder-123",
 *       "permissions": [...]
 *     }
 *   }
 * }
 * }</pre>
 *
 * @author dotCMS
 * @since 24.01
 */
public class ResponseEntityUpdatePermissionsView extends ResponseEntityView<Map<String, Object>> {

    /**
     * Creates a new ResponseEntityUpdatePermissionsView.
     *
     * @param entity Map containing message, permissionCount, inheritanceBroken, and asset data
     */
    public ResponseEntityUpdatePermissionsView(final Map<String, Object> entity) {
        super(entity);
    }
}
