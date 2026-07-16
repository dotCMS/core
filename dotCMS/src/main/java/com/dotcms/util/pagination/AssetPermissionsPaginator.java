package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.system.permission.AssetPermissionHelper;
import com.dotcms.rest.api.v1.system.permission.RolePermissionView;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Paginator for asset permission roles with in-memory pagination.
 * Similar to {@link UserPermissionsPaginator}, this pagination happens in-memory
 * since permission data is not retrieved directly from a database query
 * with offset/limit support.
 *
 * @author hassandotcms
 * @since 24.01
 */
public class AssetPermissionsPaginator implements Paginator<RolePermissionView> {

    /**
     * Parameter name for the permissionable asset.
     */
    public static final String ASSET_PARAM = "asset";

    /**
     * Parameter name for the requesting user (for permission checks).
     */
    public static final String REQUESTING_USER_PARAM = "requestingUser";

    private final AssetPermissionHelper assetPermissionHelper;

    public AssetPermissionsPaginator() {
        this(new AssetPermissionHelper());
    }

    @VisibleForTesting
    public AssetPermissionsPaginator(final AssetPermissionHelper assetPermissionHelper) {
        this.assetPermissionHelper = assetPermissionHelper;
    }

    /**
     * Returns a paginated list of role permissions for an asset.
     * <p>
     * This implementation fetches all role permissions first and then applies pagination
     * in-memory using stream skip/limit, similar to {@link UserPermissionsPaginator}.
     *
     * @param user   The requesting user (for permission checks)
     * @param limit  Number of items to return
     * @param offset Starting offset
     * @param params Extra parameters containing:
     *               - {@link #ASSET_PARAM}: The {@link Permissionable} asset
     *               - {@link #REQUESTING_USER_PARAM}: The requesting {@link User}
     * @return Paginated list of role permissions
     * @throws PaginationException if pagination fails
     */
    @Override
    public PaginatedArrayList<RolePermissionView> getItems(
            final User user,
            final int limit,
            final int offset,
            final Map<String, Object> params) throws PaginationException {

        final Permissionable asset = (Permissionable) params.get(ASSET_PARAM);
        final User requestingUser = (User) params.get(REQUESTING_USER_PARAM);

        try {
            if (asset == null) {
                throw new PaginationException(
                        new IllegalArgumentException("Asset parameter is required"));
            }

            Logger.debug(this, () -> String.format(
                    "AssetPermissionsPaginator: assetId=%s, offset=%d, limit=%d",
                    asset.getPermissionId(), offset, limit));

            // Get all role permissions (in-memory pagination like UserPermissionsPaginator)
            final List<RolePermissionView> allRolePermissions =
                    assetPermissionHelper.buildRolePermissions(asset, requestingUser);

            final long totalCount = allRolePermissions.size();

            // In-memory pagination using stream skip/limit
            final List<RolePermissionView> pagedRoles = allRolePermissions.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

            final PaginatedArrayList<RolePermissionView> result = new PaginatedArrayList<>();
            result.addAll(pagedRoles);
            result.setTotalResults(totalCount);

            Logger.debug(this, () -> String.format(
                    "AssetPermissionsPaginator: returning %d of %d total roles",
                    pagedRoles.size(), totalCount));

            return result;

        } catch (DotDataException e) {
            Logger.error(this, String.format(
                    "Error getting asset permission roles for pagination, assetId=%s",
                    asset != null ? asset.getPermissionId() : "null"), e);
            throw new DotRuntimeException("Error retrieving asset permission roles", e);
        }
    }
}
