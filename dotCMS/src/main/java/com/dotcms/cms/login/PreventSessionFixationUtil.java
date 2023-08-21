package com.dotcms.cms.login;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Allows dotCMS to prevent security problems related to Session Fixation. This behavior is enabled by default, but can
 * be disabled via the following configuration property: {@code PREVENT_SESSION_FIXATION_ON_LOGIN}.
 * <p>This approach helps prevent situations in which a specific session ID is injected to a request, bypassing the
 * expected authentication mechanisms. So, dotCMS can force the generation of a new session object after a successful
 * login, which causes its ID to change.</p>
 *
 * @author jsanca
 * @since May 29th, 2019
 */
public class PreventSessionFixationUtil {

    private static final Lazy<Boolean> PREVENT_SESSION_FIXATION_ON_LOGIN = Lazy.of(() -> Config.getBooleanProperty(
            "PREVENT_SESSION_FIXATION_ON_LOGIN", true));

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
     * Takes the current {@link HttpSession} from the HTTP Request (if it exists), invalidates it, and then returns a
     * new session containing all the attributes from the original one.
     *
     * @param request                      The current {@link HttpServletRequest} instance.
     * @param createSessionIfDoesNotExists If set to {@code true} and the current session is {@code null}, forces the
     *                                     {@link HttpServletRequest} to create a new Session. Otherwise, a null session
     *                                     is returned.
     *
     * @return HttpSession The brand-new session, or {@code null} if depending on the value of the
     * {@code createSessionIfDoesNotExists} parameter.
     */
    public HttpSession preventSessionFixation(final HttpServletRequest request, final boolean createSessionIfDoesNotExists) {

        HttpSession session = request.getSession(false);

        if (PREVENT_SESSION_FIXATION_ON_LOGIN.get()) {

            Logger.debug(this, ()-> "Preventing the session fixation");

            final Map<String, Object> oldSessionMap  = new HashMap<>();
            final HttpSession oldSession          = session;

            if (null != oldSession) {

                final Enumeration<String> keys = oldSession.getAttributeNames();

                while (keys.hasMoreElements()) {

                    final String key = keys.nextElement();
                    final Object value = oldSession.getAttribute(key);
                    oldSessionMap.put(new String(key.toCharArray()), value);
                }
                final Map<String, Object> newSessionMap = Map.copyOf(oldSessionMap);
                oldSession.invalidate();

                final HttpSession newSession = request.getSession();
                for (final Map.Entry<String, Object> entry : newSessionMap.entrySet()) {
                    newSession.setAttribute(entry.getKey(), entry.getValue());
                }

                session = newSession;
            }
        }

        return null == session && createSessionIfDoesNotExists? request.getSession(): session;
    } // preventSessionFixation.

}
