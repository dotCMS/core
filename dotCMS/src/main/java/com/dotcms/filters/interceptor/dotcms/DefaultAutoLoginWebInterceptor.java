package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Default Auto Login implementation
 * @author jsanca
 */
public class DefaultAutoLoginWebInterceptor implements WebInterceptor {

    @Override
    public Result intercept(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        final HttpSession session  = request.getSession(false);
        final String encryptedId = UtilMethods.getCookieValue
                (request.getCookies(), WebKeys.CMS_USER_ID_COOKIE);
        Result result = Result.NEXT;

        if (((session != null && session.getAttribute(WebKeys.CMS_USER) == null) || session == null) &&
                UtilMethods.isSet(encryptedId)) {

            Logger.debug(DefaultAutoLoginWebInterceptor.class, "Doing AutoLogin for " + encryptedId);
            if (LoginFactory.doCookieLogin(encryptedId, request, response)) {

                result = Result.SKIP;
            }
        }

        return result;
    } // intercept.

} // E:O:F:DefaultAutoLoginWebInterceptor.
