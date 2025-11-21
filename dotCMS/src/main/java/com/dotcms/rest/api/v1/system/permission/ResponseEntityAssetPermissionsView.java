package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.Pagination;
import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Response wrapper for asset permissions endpoint.
 * Used by PermissionResource.getAssetPermissions() to return paginated
 * permission data organized by roles for a specific asset.
 *
 * @author Hassan
 * @since 24.01
 */
public class ResponseEntityAssetPermissionsView extends ResponseEntityView<Map<String, Object>> {

    /**
     * Constructor for asset permissions response with pagination.
     * Places pagination at root level alongside entity.
     *
     * @param entity Map containing asset metadata and paginated permissions array
     * @param pagination Pagination metadata (currentPage, perPage, totalEntries)
     */
    public ResponseEntityAssetPermissionsView(final Map<String, Object> entity, final Pagination pagination) {
        super(entity, pagination);
    }
}
