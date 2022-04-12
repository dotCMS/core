package com.dotmarketing.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;

/**
 * An user can get login into the BE or FE (regardless their roles)
 * The class will returns the mode the user gets login (if was on FE or BE)
 * Unknown if not session, login or can to determinate
 * @author jsanca
 */
public enum LoginMode {

    UNKNOWN, BE, FE;

    /**
     * Gets the current Login mode based on the {@link HttpServletRequestThreadLocal} (if it is set)
     * @return LoginMode
     */
    public static LoginMode get() {

        final HttpServletRequest req = Try.of(()-> HttpServletRequestThreadLocal.INSTANCE.getRequest()).getOrNull();
        return null == req? UNKNOWN: get(req);

    }

    /**
     * Gets the current Login mode based on the {@link HttpServletRequest}
     * @param request  {@link HttpServletRequest}
     * @return LoginMode
     */
    public static LoginMode get(final HttpServletRequest request) {

        if (request.getSession(false) != null && PortalUtil.getUser(request) != null &&
                request.getSession(false).getAttribute(WebKeys.LOGIN_MODE_PARAMETER) != null) {

            final LoginMode loginMode = (LoginMode) request.getSession(false)
                    .getAttribute(WebKeys.LOGIN_MODE_PARAMETER);
            return loginMode;
        }

        return UNKNOWN;
    }

    /**
     * Sets the loginMode
     * pre: session should exists, otherwise won't set the loginMode
     * @param request  {@link HttpServletRequest}
     * @param loginMode {@link LoginMode}
     */
    public static void set(final HttpServletRequest request, final LoginMode loginMode) {

        if (request.getSession(false) != null) {

            request.getSession(false).setAttribute(WebKeys.LOGIN_MODE_PARAMETER, loginMode);
        }
    }
}