package com.dotcms.filters.interceptor.cas;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Cas auto login implementation
 * @author jsanca
 */
public class CasAutoLoginWebInterceptor implements WebInterceptor {

    private final LoginServiceAPI loginServiceAPI;
    final boolean useCasFilter =
                    Config.getBooleanProperty("FRONTEND_CAS_FILTER_ON", false);
    public CasAutoLoginWebInterceptor() {
        this(APILocator.getLoginServiceAPI());
    }

    public CasAutoLoginWebInterceptor(final LoginServiceAPI loginServiceAPI) {
        this.loginServiceAPI = loginServiceAPI;
    }

    @Override
    public Result intercept(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        final HttpSession session  = request.getSession(false);

        Result result = Result.NEXT;

        if (useCasFilter && null != session) {

            final String userID = (String) session.getAttribute("edu.yale.its.tp.cas.client.filter.user");
            Logger.debug(CasAutoLoginWebInterceptor.class, "Doing CasAutoLogin Filter for: " + userID);

            if (UtilMethods.isSet(userID)) {

                if (this.loginServiceAPI.doCookieLogin(PublicEncryptionFactory.encryptString
                        (userID), request, response)) {

                    result = Result.SKIP;
                }
            }
        }

        return result;
    } // intercept.

} // E:O:F:CasAutoLoginWebInterceptor.
