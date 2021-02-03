package com.dotmarketing.business.helper;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for operations that involves the use of the {@link PermissionAPI}
 */
public class PermissionHelper {

    private final PermissionAPI permissionAPI;
    private final List<Integer> DEFAULT_PERMISSIONS = CollectionsUtils
            .list(PermissionAPI.PERMISSION_WRITE, PermissionAPI.PERMISSION_READ);

    private final List<PermissionableType> DEFAULT_PERMISSIONABLE_TYPES = Arrays
            .asList(PermissionableType.values());

    private PermissionHelper() {
        this.permissionAPI = APILocator.getPermissionAPI();
    }

    private static class PermissionSingletonHelper {

        private static final PermissionHelper INSTANCE = new PermissionHelper();
    }

    public static PermissionHelper getInstance() {
        return PermissionHelper.PermissionSingletonHelper.INSTANCE;
    }

    /**
     * Given a user and optional permissions and permissionableTypes, returns a map with the
     * permissions allowed by permission type
     *
     * @param user User who requests the permissions info
     * @param permissions Optional list with the permissions to be required (view {@link
     * PermissionAPI#PERMISSION_READ} and {@link PermissionAPI#PERMISSION_WRITE}). If not set,
     * {@link PermissionAPI#PERMISSION_READ} and {@link PermissionAPI#PERMISSION_WRITE} will be
     * returned
     * @param permissionableTypes Optional list of PermissionableType's names to query (view allowed
     * values in {@link PermissionableType} enum). If not set, all values in the {@link
     * PermissionableType} enum will be returned
     * @return Map with the permissions allowed by permission type: Map<PermissionableType's name,
     * Map<`canRead/canWrite`, Boolean>>
     */
    public Map<String, Map<String, Boolean>> getPermissionsByPermissionType(final User user,
            final List<Integer> permissions, final List<String> permissionableTypes)
            throws DotDataException {

        if (null == user) {
            throw new DotDataValidationException("User cannot be null");
        }

        final Map<String, Map<String, Boolean>> result = new HashMap<>();

        final List<Integer> permissionsToReturn = UtilMethods.isSet(permissions) ? permissions
                : DEFAULT_PERMISSIONS;

        final List<PermissionableType> permissionTypesToReturn =
                UtilMethods.isSet(permissionableTypes) ? permissionableTypes.stream()
                        .map(type -> PermissionableType.valueOf(type)).collect(
                                Collectors.toList()) : DEFAULT_PERMISSIONABLE_TYPES;

        for (final PermissionableType type : permissionTypesToReturn) {
            final Map<String, Boolean> allowedPermissions = new HashMap<>();
            boolean hasWritePermissions = false;
            if (permissionsToReturn.contains(PermissionAPI.PERMISSION_WRITE)) {
                hasWritePermissions = permissionAPI
                        .doesUserHavePermissions(type, PermissionAPI.PERMISSION_WRITE, user);
                allowedPermissions.put("canWrite", hasWritePermissions);
            }

            if (permissionsToReturn.contains(PermissionAPI.PERMISSION_READ)) {
                if (hasWritePermissions) {
                    allowedPermissions.put("canRead", Boolean.TRUE);
                } else {
                    allowedPermissions.put("canRead", permissionAPI
                            .doesUserHavePermissions(type, PermissionAPI.PERMISSION_READ, user));
                }
            }

            result.put(type.name(), Collections.unmodifiableMap(allowedPermissions));
        }

        return Collections.unmodifiableMap(result);
    }
}
