package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.action.ActionMessage;
import com.dotcms.repackage.org.apache.struts.action.ActionMessages;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * This interceptor basically checks the user status login on the FRONT-END
 * @author jsanca
 */
public class DefaultFrontEndLoginRequiredWebInterceptor implements WebInterceptor {

    @Override
    public Result intercept(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        final HttpSession session = request.getSession(false);
        Result result = Result.NEXT;

        if (null != session) {
            final boolean isAdminMode = PageMode.get(request).isAdmin;

            // if we are not logged in and you are not admin mode. go to login page
            if (session.getAttribute(WebKeys.CMS_USER) == null
                    && !isAdminMode) {

                Logger.warn(this.getClass(),
                        "Doing LoginRequiredFilter for RequestURI: " +
                                request.getRequestURI() + "?" + request.getQueryString());

                //if we don't have a redirect yet
                session.setAttribute(WebKeys.REDIRECT_AFTER_LOGIN,
                        request.getRequestURI() + "?" + request.getQueryString());

                final ActionMessages ams = new ActionMessages();
                ams.add(Globals.MESSAGE_KEY, new ActionMessage("message.login.required"));
                session.setAttribute(Globals.MESSAGE_KEY, ams);
                response.sendError(401);
                result = Result.SKIP_NO_CHAIN; // needs to stop the filter chain.
            }
        }

        return result; // if it is log in, continue!
    } // intercept.

    @Override
    public String[] getFilters() {
        // this filter is just for intranet.
        return new String[] { "/intranet" };
    }
} // E:O:F:DefaultFrontEndLoginRequiredWebInterceptor.
