package com.dotcms.cms.login;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Intercepts performs the logout at the end of the call in order to give chance
 * to thirdparties implementation to patch the logout process, if needed
 */
public class LogoutWebInterceptor implements WebInterceptor{

    private static final String      API_CALL                = "/dotAdmin/logout";


    @Override
    public String[] getFilters() {
        return new String[] {
                API_CALL + "*"
        };
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        Logger.info(this.getClass(), "Starting Logout --> " + request.getRequestURI());

        try {

            Logger.info(this, ()-> "Doing the logout");
            final User user = PortalUtil.getUser(request);

            APILocator.getLoginServiceAPI().doActionLogout(request, response);

            if(null != user){
                SecurityLogger.logInfo(this.getClass(), "User " +
                        user.getFullName() + " (" + user.getUserId() + ") has logged out from IP: " + request.getRemoteAddr());
            }

            response.sendRedirect(Config.getStringProperty("logout.url", "/dotAdmin/#/public/logout"));
            Logger.info(this, ()-> "Logout DONE");
        }  catch (Exception e) {

            Logger.error(this,"Error doing the logout", e);
            try {
                if (!response.isCommitted()) {
                    response.sendError(500);
                }
            } catch (IOException ioException) {
                Logger.error(this,"Can not redirect to error", e);
            }

        }

        return Result.SKIP_NO_CHAIN;
    }

}
