package com.dotmarketing.business;

import com.dotcms.system.AppContext;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import jnr.ffi.Runtime;

/**
 * Default implementation of {@link LoginAsAPI}
 */
public class LoginAsAPIImpl implements LoginAsAPI {

    private UserAPI userAPI;

    private LoginAsAPIImpl() {
        this(APILocator.getUserAPI());
    }

    @VisibleForTesting
    private LoginAsAPIImpl(UserAPI userAPI) {
        this.userAPI = userAPI;
    }

    private static class SingletonHolder {
        private static final LoginAsAPIImpl INSTANCE = new LoginAsAPIImpl();
    }

    public static LoginAsAPIImpl getInstance() {

        return LoginAsAPIImpl.SingletonHolder.INSTANCE;
    }

    /**
     * Return the LoginAs user
     *
     * @param appContext
     * @return if a user is LoginAs then return it, in otherwise return null
     * @throws  if one is thrown when the user is search
     */
    public User getPrincipalUser(AppContext appContext) {
        try {
            String principalUserId = appContext.getAttribute(WebKeys.PRINCIPAL_USER_ID);
            User loginAsUser = null;

            if (principalUserId != null) {
                loginAsUser = userAPI.loadUserById(principalUserId);
            }

            return loginAsUser;
        }catch(DotSecurityException|DotDataException e){
            throw new LoginAsRuntimeException(e);
        }
    }

    public boolean isLoginAsUser(AppContext appContext){
        return appContext.getAttribute(WebKeys.PRINCIPAL_USER_ID) != null;
    }
}
