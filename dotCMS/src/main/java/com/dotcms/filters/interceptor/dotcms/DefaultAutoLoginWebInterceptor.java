package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.util.CookieUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
    public Result intercept(final HttpServletRequest request,
            final HttpServletResponse response) {

        final HttpSession session = request.getSession(false);
        Result result = Result.NEXT;

        if ((null != session && session.getAttribute(WebKeys.CMS_USER) == null)
                || session == null) {

            final String jwtCookieValue = CookieUtil.get
                    (request.getCookies(), CookieKeys.JWT_ACCESS_TOKEN);
            if (null != jwtCookieValue) {

                try {
                    final User user = this.jsonWebTokenUtils.getUser(jwtCookieValue);
                    if (null != user) {

                        if (this.loginServiceAPI.
                                doCookieLogin(this.encryptor.encryptString(user.getUserId()),
                                        request,
                                        response)) {

                            // if this login was successfully, do not need to do any other.
                            result = Result.SKIP;
                        }
                    }
                } catch (Exception e) {
                    //Handling this invalid token exception
                    JsonWebTokenUtils.getInstance()
                            .handleInvalidTokenExceptions(this.getClass(), e, request, response);
                }
            }
        }

        return result;
    } // intercept.

} // E:O:F:DefaultAutoLoginWebInterceptor.