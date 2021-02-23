package com.dotmarketing.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * 
 * @author David Torres
 */
public class UserProxyAPIImpl implements UserProxyAPI {

    private final PermissionAPI permissionAPI;

    public UserProxyAPIImpl() {
        permissionAPI = APILocator.getPermissionAPI();
    }

    @CloseDBIfOpened
    public UserProxy getUserProxy(String userId, User user, boolean respectFrontEndRoles)
                    throws DotRuntimeException, DotSecurityException, DotDataException {
        if (userId == null) {
            return null;
        }

        UserProxy up = new UserProxy(userId);

        if (permissionAPI.doesUserHavePermission(up, PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)) {
            return up;
        } else {
            throw new DotSecurityException("User doesn't have permissions to retrieve UserProxy");
        }

    }

    public UserProxy getUserProxy(User userToGetProxyFor, User user, boolean respectFrontEndRoles)
                    throws DotRuntimeException, DotSecurityException, DotDataException {
        return getUserProxy(userToGetProxyFor.getUserId(), user, respectFrontEndRoles);
    }


    @WrapInTransaction
    public void saveUserProxy(UserProxy userProxy, User user, boolean respectFrontEndRoles)
                    throws DotRuntimeException, DotDataException, DotSecurityException {

    }


}
