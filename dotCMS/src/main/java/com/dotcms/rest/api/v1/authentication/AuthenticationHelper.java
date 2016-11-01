package com.dotcms.rest.api.v1.authentication;

import com.dotcms.api.web.WebSessionContext;
import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LoginAsAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * {@link AuthenticationResource}'s helper
 */
class AuthenticationHelper {

    private final LoginAsAPI loginAsAPI;
    private final LoginService loginService;

    private static class SingletonHolder {
        private static final AuthenticationHelper INSTANCE = new AuthenticationHelper();
    }

    public static AuthenticationHelper getInstance() {
        return AuthenticationHelper.SingletonHolder.INSTANCE;
    }

    private AuthenticationHelper() {
        this( APILocator.getLoginAsAPI(), LoginServiceFactory.getInstance().getLoginService() );
    }

    @VisibleForTesting
    protected AuthenticationHelper(LoginAsAPI loginAsAPI, LoginService loginService) {
        this.loginAsAPI = loginAsAPI;
        this.loginService = loginService;
    }

    /** 
    * Return a map with the Principal and LoginAs user, the map content the follows keys: 
    * <ul> 
    *   <li>{@link AuthenticationResource#USER} for the principal user</li> 
    *   <li>{@link AuthenticationResource#LOGIN_AS_USER} for the login as user</li>
    *</ul>
    *
    * @param request 
    * @return
    * @throws DotDataException
    * @throws DotSecurityException
    */
    public Map<String, Map> getUsers(final HttpServletRequest request) throws DotDataException, DotSecurityException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        User principalUser = loginAsAPI.getPrincipalUser( WebSessionContext.getInstance( request ));
        User loginAsUser = null;

        if (principalUser == null){
            principalUser = this.loginService.getLogInUser( request );
        }else{
            loginAsUser = this.loginService.getLogInUser( request );
        }

        return map(AuthenticationResource.USER, principalUser != null ? principalUser.toMap() : null, AuthenticationResource.LOGIN_AS_USER,
                loginAsUser != null ? loginAsUser.toMap() : null);
    }
}
