package com.dotcms.cms.login;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Prevent the session fixation if the config property "PREVENT_SESSION_FIXATION_ON_LOGIN" is not in false.
 * @author jsanca
 */
public class PreventSessionFixationUtil {

    private PreventSessionFixationUtil() {
        // singleton
    }

    private static class SingletonHolder {
        private static final PreventSessionFixationUtil INSTANCE = new PreventSessionFixationUtil();
    }

    public static PreventSessionFixationUtil getInstance () {
        return PreventSessionFixationUtil.SingletonHolder.INSTANCE;
    }

    /**
     * Gets the current session (if exists) invalidate it and them created a new one with a
     * copy of the previous session attributes.
     * @param request {@link HttpServletRequest}
     * @param createSessionIfDoesNotExists {@link Boolean} if false and the session on the request.getSession(false) returns null (no session created) returns a null session,
     *                                                    if true will create a new session if does not exists
     * @return HttpSession
     */
    public HttpSession preventSessionFixation(final HttpServletRequest request, final boolean createSessionIfDoesNotExists) {

        HttpSession session = request.getSession(false);

        if(Config.getBooleanProperty("PREVENT_SESSION_FIXATION_ON_LOGIN", true)) {

            Logger.debug(this, ()-> "Preventing the session fixation");

            final Map<String, Object> sessionMap  = new HashMap<>();
            final HttpSession oldSession          = session;

            if (null != oldSession) {

                final Enumeration<String> keys = oldSession.getAttributeNames();

                while (keys.hasMoreElements()) {

                    final String key = keys.nextElement();
                    final Object value = oldSession.getAttribute(key);
                    sessionMap.put(key, value);
                }

                oldSession.invalidate();

                final HttpSession newSession = request.getSession();
                for (final Map.Entry<String, Object> entry : sessionMap.entrySet()) {
                    newSession.setAttribute(entry.getKey(), entry.getValue());
                }

                session = newSession;
            }
        }

        return null == session && createSessionIfDoesNotExists? request.getSession(): session;
    } // preventSessionFixation.
}
