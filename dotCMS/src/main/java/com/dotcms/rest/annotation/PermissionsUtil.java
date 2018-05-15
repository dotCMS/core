package com.dotcms.rest.annotation;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;

public class PermissionsUtil {

    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    private static class SingletonHolder {
        private static final PermissionsUtil INSTANCE = new PermissionsUtil();
    }

    public static PermissionsUtil getInstance() {
        return PermissionsUtil.SingletonHolder.INSTANCE;
    }

    private final static String [] FULL_PERMISSIONS =
            new String[] { PermissionLevel.READ.name(), PermissionLevel.WRITE.name(), PermissionLevel.PUBLISH.name(),PermissionLevel.EDIT_PERMISSIONS.name()};

    private final static String [] PUBLISH_PERMISSIONS =
            new String[] { PermissionLevel.READ.name(), PermissionLevel.WRITE.name(), PermissionLevel.PUBLISH.name()};

    private final static String [] WRITE_PERMISSIONS =
            new String[] { PermissionLevel.READ.name(), PermissionLevel.WRITE.name()};

    private final static String [] READ_PERMISSIONS =
            new String[] { PermissionLevel.READ.name()};

    /**
     * Get an string array representations (json) of the basic permissions for an user over permissionable, for instance full permission will:
     *
     * [READ,WRITE,PUBLISH,EDIT_PERMISSIONS]
     *
     * @param permissionable
     * @param user
     * @return json array representation of the permissions for an user over permissionable
     * @throws DotDataException
     */
    public String[] getPermissionsArray (final Permissionable permissionable, final User user) throws DotDataException {

        if (this.permissionAPI.doesUserHavePermission
                (permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {

            return FULL_PERMISSIONS;
        } else {

            if (this.permissionAPI.doesUserHavePermission
                    (permissionable, PermissionAPI.PERMISSION_PUBLISH, user)) {

                return PUBLISH_PERMISSIONS;
            } else {

                if (this.permissionAPI.doesUserHavePermission
                        (permissionable, PermissionAPI.PERMISSION_WRITE, user)) {

                    return WRITE_PERMISSIONS;
                } else if (this.permissionAPI.doesUserHavePermission
                        (permissionable, PermissionAPI.PERMISSION_READ, user)) {

                    return READ_PERMISSIONS;
                }
            }
        }

        return new String[] {PermissionLevel.NONE.name()};
    }
}
