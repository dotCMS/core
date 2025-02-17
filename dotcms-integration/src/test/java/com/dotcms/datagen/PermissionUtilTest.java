package com.dotcms.datagen;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.Arrays;

public final class PermissionUtilTest {

    private PermissionUtilTest(){}

    public static void addAnonymousUser(final Permissionable permissionable) throws DotDataException, DotSecurityException {
         final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();

        final Permission permission = new Permission(
                permissionable.getPermissionId(),
                anonymousRole.getId(),
                PermissionAPI.PERMISSION_READ);
        APILocator.getPermissionAPI().save(permission, permissionable, APILocator.systemUser(), false);
    }

    public static void addPermission(final Permissionable permissionable,
                                     final User user, final String permissionType, final int... permissions) throws DotDataException {

        final int permission = Arrays.stream(permissions).sum();

        final Permission permissionObject = new Permission(permissionType,
                permissionable.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId(),
                permission, true);

        try {
            APILocator.getPermissionAPI().save(permissionObject, permissionable,
                    APILocator.systemUser(), false);
        } catch (DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
