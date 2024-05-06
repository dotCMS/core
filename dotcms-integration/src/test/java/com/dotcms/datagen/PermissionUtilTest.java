package com.dotcms.datagen;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

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
}
