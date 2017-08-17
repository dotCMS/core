package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.util.CookieKeys;
import com.liferay.util.CookieUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Default Auto Login implementation
 * @author jsanca
 */
public class DefaultAutoLoginWebInterceptor implements WebInterceptor {

    private final JsonWebTokenUtils jsonWebTokenUtils;

    public DefaultAutoLoginWebInterceptor() {
        this(JsonWebTokenUtils.getInstance());
    }

    public DefaultAutoLoginWebInterceptor(final JsonWebTokenUtils jsonWebTokenUtils) {

        this.jsonWebTokenUtils = jsonWebTokenUtils;
    }

    @Override
    public Result intercept(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        final HttpSession session  = request.getSession(false);
        final String jwtCookieValue = CookieUtil.get
                (request.getCookies(), CookieKeys.JWT_ACCESS_TOKEN);
        final String encryptedId = this.jsonWebTokenUtils.getEncryptedUserId(jwtCookieValue);
        Result result = Result.NEXT;

        if (((session != null && session.getAttribute(WebKeys.CMS_USER) == null) || session == null) &&
                UtilMethods.isSet(encryptedId)) {

            Logger.debug(DefaultAutoLoginWebInterceptor.class, "Doing AutoLogin for " + encryptedId);
            if (APILocator.getLoginServiceAPI().doCookieLogin(encryptedId, request, response)) {

                result = Result.SKIP;
            }
        }

        return result;
    } // intercept.

} // E:O:F:DefaultAutoLoginWebInterceptor.
