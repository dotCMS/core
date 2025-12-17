package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.system.permission.PermissionSaveHelper;
import com.dotcms.rest.api.v1.system.permission.UserPermissionAssetView;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Paginator for user permission assets with in-memory pagination.
 * Similar to {@link SiteViewPaginator}, this pagination happens in-memory
 * since permission assets are not retrieved directly from a database query
 * with offset/limit support.
 *
 * @author hassandotcms
 * @since 24.01
 */
public class UserPermissionsPaginator implements Paginator<UserPermissionAssetView> {

    /**
     * Parameter name for the user's individual role.
     */
    public static final String ROLE_PARAM = "role";

    /**
     * Parameter name for the target user ID.
     */
    public static final String USER_ID_PARAM = "userId";

    private final PermissionSaveHelper permissionSaveHelper;

    public UserPermissionsPaginator() {
        this(new PermissionSaveHelper());
    }

    @VisibleForTesting
    public UserPermissionsPaginator(final PermissionSaveHelper permissionSaveHelper) {
        this.permissionSaveHelper = permissionSaveHelper;
    }

    /**
     * Returns a paginated list of permission assets for a user's role.
     * <p>
     * This implementation fetches all assets first and then applies pagination
     * in-memory using stream skip/limit, similar to {@link SiteViewPaginator}.
     *
     * @param user   The requesting user (for permission checks)
     * @param limit  Number of items to return
     * @param offset Starting offset
     * @param params Extra parameters containing:
     *               - {@link #ROLE_PARAM}: The user's individual {@link Role}
     *               - {@link #USER_ID_PARAM}: The target user's ID
     * @return Paginated list of permission assets
     * @throws PaginationException if pagination fails
     */
    @Override
    public PaginatedArrayList<UserPermissionAssetView> getItems(
            final User user,
            final int limit,
            final int offset,
            final Map<String, Object> params) throws PaginationException {

        try {
            final Role role = (Role) params.get(ROLE_PARAM);

            if (role == null) {
                throw new PaginationException(
                        new IllegalArgumentException("Role parameter is required"));
            }

            Logger.debug(this, () -> String.format(
                    "UserPermissionsPaginator: role=%s, offset=%d, limit=%d",
                    role.getId(), offset, limit));

            // Get all assets (in-memory pagination like SiteViewPaginator)
            final List<UserPermissionAssetView> allAssets =
                    permissionSaveHelper.getUserPermissionAssets(role, user);

            final long totalCount = allAssets.size();

            // In-memory pagination using stream skip/limit
            final List<UserPermissionAssetView> pagedAssets = allAssets.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

            final PaginatedArrayList<UserPermissionAssetView> result = new PaginatedArrayList<>();
            result.addAll(pagedAssets);
            result.setTotalResults(totalCount);

            Logger.debug(this, () -> String.format(
                    "UserPermissionsPaginator: returning %d of %d total assets",
                    pagedAssets.size(), totalCount));

            return result;

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting user permission assets for pagination", e);
            throw new DotRuntimeException("Error retrieving user permission assets", e);
        }
    }
}
