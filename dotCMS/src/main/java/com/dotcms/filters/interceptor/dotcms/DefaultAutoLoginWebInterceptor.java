package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.CookieUtil;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Default Auto Login implementation
 *
 * @author jsanca
 */
public class DefaultAutoLoginWebInterceptor implements WebInterceptor {

    private final JsonWebTokenUtils jsonWebTokenUtils;
    private final LoginServiceAPI loginServiceAPI;
    private Encryptor encryptor;

    public DefaultAutoLoginWebInterceptor() {
        this(JsonWebTokenUtils.getInstance(), APILocator.getLoginServiceAPI(),
                EncryptorFactory.getInstance().getEncryptor());
    }

    public DefaultAutoLoginWebInterceptor(final JsonWebTokenUtils jsonWebTokenUtils,
            final LoginServiceAPI loginServiceAPI,
            final Encryptor encryptor) {

        this.jsonWebTokenUtils = jsonWebTokenUtils;
        this.loginServiceAPI = loginServiceAPI;
        this.encryptor = encryptor;
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {


        Result result = Result.NEXT;

        User user = Try.of(()-> PortalUtil.getUser(request)).getOrNull();
        if (user==null) {

            final String jwtCookieValue = CookieUtil.get(request.getCookies(), CookieKeys.JWT_ACCESS_TOKEN);
            if (null != jwtCookieValue) {
                user = this.jsonWebTokenUtils.getUser(jwtCookieValue, request.getRemoteAddr());
                if(user==null) {
                    // user is null because token is expired
                    CookieUtil.deleteCookie(request, response, CookieKeys.JWT_ACCESS_TOKEN);
                }else {
                    if (this.loginServiceAPI.doCookieLogin(this.encryptor.encryptString(user.getUserId()), request, response)) {
                        // if this login was successfully, do not need to do any other.
                        result = Result.SKIP;
                    }
                }

            }
        }

        return result;
    } // intercept.


} // E:O:F:DefaultAutoLoginWebInterceptor.