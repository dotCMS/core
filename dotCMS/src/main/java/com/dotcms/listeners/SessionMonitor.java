package com.dotcms.listeners;

import static com.dotcms.cms.login.LoginServiceAPIFactory.LOG_OUT_ATTRIBUTE;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.UserSessionPayloadBuilder;
import com.dotcms.cache.DynamicTTLCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.Map;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Listener that keeps track of logged in users by monitoring for USER_ID session attribute addition.
 * <p>
 * By: IPFW Web Team
 */
public class SessionMonitor implements ServletRequestListener,
        HttpSessionAttributeListener, HttpSessionListener {

    public static final String DOT_CLUSTER_SESSION = "DOT_CLUSTER_SESSION";
    public static final String IGNORE_REMEMBER_ME_ON_INVALIDATION = "IGNORE_REMEMBER_ME_ON_INVALIDATION";
    public static final String USER_REMOTE_ADDR = "USER_REMOTE_ADDR";

    private final SystemEventsAPI systemEventsAPI;
    private static final Lazy<Long> SESSIONS_TO_TRACK_MAX = Lazy.of(() -> Config.getLongProperty("SESSIONS_TO_TRACK_MAX", 5000L));
    private static final Lazy<Long> SESSIONS_TO_TRACK_TTL = Lazy.of(() -> Config.getLongProperty("SESSIONS_TO_TRACK_TTL", 30 * 60 * 1000L));
    private static final DynamicTTLCache<String, HttpSession> userSessions = new DynamicTTLCache<String, HttpSession>(SESSIONS_TO_TRACK_MAX.get(), SESSIONS_TO_TRACK_TTL.get());



    public static Lazy<SessionMonitor> get() {
        return Lazy.of(SessionMonitor::new);
    }




    public SessionMonitor() {
        this(APILocator.getSystemEventsAPI());
    }
    public SessionMonitor(final SystemEventsAPI systemEventsAPI) {
        this.systemEventsAPI = systemEventsAPI;
    }




    public Map<String, HttpSession> getUserSessions() {

        return userSessions.copyAsMap();
    }


    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        // Not implemented
    }

    /**
     * Checks if the attribute removed is "USER_ID". If so, remove the logout user
     */
    @Override
    public void attributeRemoved(final HttpSessionBindingEvent event) {

    }

    /**
     * Do nothing here.
     */
    @Override

    public void attributeReplaced(HttpSessionBindingEvent event) {
        // Not implemented
    }

    @Override
    public void sessionCreated(final HttpSessionEvent event) {
        // Not implemented
        Logger.debug(this, "Session created");
    }

    private static final Lazy<Boolean> sendSessionDestroyedToWebSocket = Lazy.of(() ->  Config.getBooleanProperty("SEND_SESSION_DESTROYED_TO_WEBSOCKET", false));

    @Override
    public void sessionDestroyed(final HttpSessionEvent event) {

        Logger.debug(this, "Session destroyed");

        HttpSession session = event.getSession();
        if (session == null) {
            return;
        }
        final String userId = (String) session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID);
        if (userId == null) {
            return;
        }

        final String sessionId = session.getId();
        userSessions.invalidate(sessionId);

        boolean ignoreRememberMe = session.getAttribute(IGNORE_REMEMBER_ME_ON_INVALIDATION) != null;

        Logger.debug(this, "Triggering a session destroyed event");

        final boolean isLogout =
                session.getAttribute(LOG_OUT_ATTRIBUTE) != null && Boolean.parseBoolean(
                        session.getAttribute(LOG_OUT_ATTRIBUTE).toString());

        if (isLogout) {
            Try.run(() -> this.systemEventsAPI.push(new SystemEvent
                    (SystemEventType.SESSION_LOGOUT, UserSessionPayloadBuilder.build(userId, sessionId))));
            return;
        }

        // sending this event to the websocket client will automatically logout the user
        // which sounds great except for the case where the user has clicked "REMEMBER_ME"
        if (ignoreRememberMe || sendSessionDestroyedToWebSocket.get()) {
            Try.run(() -> this.systemEventsAPI.push(new SystemEvent
                    (SystemEventType.SESSION_DESTROYED,
                            UserSessionPayloadBuilder.build(userId, sessionId))));

        }



    }

    @Override
    public void requestDestroyed(ServletRequestEvent arg0) {
        // Not implemented
    }

    /**
     * When a User has successfully logged in, that is, the {@code USER_ID} attribute is present, dotCMS will keep track
     * of such a session in an internal in-memory Map. This is done so CMS Administrators will be able to know which
     * Users are logged in, and be able to terminate their sessions if needed.
     * <p>Additionally, an attribute called {@link #DOT_CLUSTER_SESSION} is added to the session in order to flag it as
     * an authenticated session. It's present i both back-end and front-end sessions. This attribute is crucial for the
     * Redis-based Session Manager plugin to work correctly, when enabled.</p>
     *
     * @param event The {@link ServletRequestEvent} instance of the event that triggered this action.
     */
    @Override
    public void requestInitialized(final ServletRequestEvent event) {
        final HttpSession session = ((HttpServletRequest) event.getServletRequest())
                .getSession(false);
        if (session != null && session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID) != null) {
            final String userId = (String) session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID);
            session.setAttribute(USER_REMOTE_ADDR, event.getServletRequest().getRemoteAddr());
            final String id = session.getId();
            userSessions.put(id, session);

            if (UtilMethods.isSet(userId) && !UserAPI.CMS_ANON_USER_ID.equalsIgnoreCase(userId)) {
                session.setAttribute(DOT_CLUSTER_SESSION, true);
            }
        }

    }

}
