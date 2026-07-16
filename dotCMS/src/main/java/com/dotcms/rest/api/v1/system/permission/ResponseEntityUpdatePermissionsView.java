package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

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
public class ResponseEntityUpdatePermissionsView extends ResponseEntityView<UpdateAssetPermissionsView> {

    /**
     * Creates a new ResponseEntityUpdatePermissionsView.
     *
     * @param entity UpdateAssetPermissionsView containing message, permissionCount,
     *               inheritanceBroken, and asset data
     */
    public ResponseEntityUpdatePermissionsView(final UpdateAssetPermissionsView entity) {
        super(entity);
    }
}
