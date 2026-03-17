package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity view for the PUT /api/v1/permissions/{assetId}/_reset endpoint.
 *
 * <p>Response structure:
 * <pre>{@code
 * {
 *   "entity": {
 *     "message": "Individual permissions removed. Asset now inherits from parent.",
 *     "assetId": "asset-123",
 *     "previousPermissionCount": 5
 *   }
 * }
 * }</pre>
 *
 * @author dotCMS
 * @since 24.01
 */
public class ResponseEntityResetPermissionsView extends ResponseEntityView<ResetAssetPermissionsView> {

    /**
     * Creates a new ResponseEntityResetPermissionsView.
     *
     * @param entity ResetAssetPermissionsView containing message, assetId, and previousPermissionCount
     */
    public ResponseEntityResetPermissionsView(final ResetAssetPermissionsView entity) {
        super(entity);
    }
}
