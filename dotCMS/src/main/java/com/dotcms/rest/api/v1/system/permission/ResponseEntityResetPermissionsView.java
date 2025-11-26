package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

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
public class ResponseEntityResetPermissionsView extends ResponseEntityView<Map<String, Object>> {

    /**
     * Creates a new ResponseEntityResetPermissionsView.
     *
     * @param entity Map containing message, assetId, and previousPermissionCount
     */
    public ResponseEntityResetPermissionsView(final Map<String, Object> entity) {
        super(entity);
    }
}
