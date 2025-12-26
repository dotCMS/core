package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.Pagination;
import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for asset permissions endpoint.
 * Used by PermissionResource.getAssetPermissions() to return paginated
 * permission data organized by roles for a specific asset.
 *
 * @author hassandotcms
 * @since 24.01
 */
public class ResponseEntityAssetPermissionsView extends ResponseEntityView<AssetPermissionsView> {

    /**
     * Constructor for asset permissions response with pagination.
     * Places pagination at root level alongside entity.
     *
     * @param entity AssetPermissionsView containing asset metadata and paginated role permissions
     * @param pagination Pagination metadata (currentPage, perPage, totalEntries)
     */
    public ResponseEntityAssetPermissionsView(final AssetPermissionsView entity, final Pagination pagination) {
        super(entity, pagination);
    }
}
