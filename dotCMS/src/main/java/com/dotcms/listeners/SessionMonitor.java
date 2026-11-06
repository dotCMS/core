package com.dotcms.listeners;

import static com.dotcms.cms.login.LoginServiceAPIFactory.LOG_OUT_ATTRIBUTE;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.UserSessionPayloadBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cache.DynamicTTLCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
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


    /**
     * Registers the session as soon as its {@code USER_ID} attribute is stored, whatever login
     * path stored it (form login, SAML, OAuth, the session copy made after login, Login As), so
     * the session is listed and can be ended before it makes another request. The client address
     * is recorded when the current request is known.
     *
     * @param event the attribute binding event
     */
    @Override
    public void attributeAdded(final HttpSessionBindingEvent event) {
        trackOnUserIdChange(event);
    }

    /**
     * Checks if the attribute removed is "USER_ID". If so, remove the logout user
     */
    @Override
    public void attributeRemoved(final HttpSessionBindingEvent event) {

    }

    /**
     * Same as {@link #attributeAdded(HttpSessionBindingEvent)}, for a session whose user id is
     * replaced, as Login As does.
     *
     * @param event the attribute binding event
     */
    @Override
    public void attributeReplaced(final HttpSessionBindingEvent event) {
        trackOnUserIdChange(event);
    }

    /**
     * Tracks the event's session when the attribute that changed is {@code USER_ID}.
     */
    private static void trackOnUserIdChange(final HttpSessionBindingEvent event) {
        if (event == null || !com.liferay.portal.util.WebKeys.USER_ID.equals(event.getName())) {
            return;
        }
        final HttpSession session = event.getSession();
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (request != null && session != null) {
            session.setAttribute(USER_REMOTE_ADDR, request.getRemoteAddr());
        }
        track(session);
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
            session.setAttribute(USER_REMOTE_ADDR, event.getServletRequest().getRemoteAddr());
            track(session);
        }

    }

    /**
     * Registers an authenticated session in the tracked map, so it is listed and can be ended by
     * the session tools. The session must hold the {@code USER_ID} attribute; a session without
     * it, or a {@code null} session, is ignored. Sessions of users other than the anonymous user
     * are also flagged with {@link #DOT_CLUSTER_SESSION}. Called when the user id is stored in a
     * session and again on each of its requests, which is harmless.
     *
     * @param session the session to track, usually the one just created at login
     */
    public static void track(final HttpSession session) {
        if (session == null) {
            return;
        }
        final Object userId = session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID);
        if (userId == null) {
            return;
        }
        userSessions.put(session.getId(), session);
        if (userId instanceof String && UtilMethods.isSet((String) userId)
                && !UserAPI.CMS_ANON_USER_ID.equalsIgnoreCase((String) userId)) {
            session.setAttribute(DOT_CLUSTER_SESSION, true);
        }
    }

    /**
     * Tells whether a session belongs to the user: it is the session's user, or the administrator
     * behind a Login As session ({@code PRINCIPAL_USER_ID}).
     */
    private static boolean belongsTo(final HttpSession session, final String userId) {
        for (final String key : new String[]{com.liferay.portal.util.WebKeys.USER_ID,
                com.liferay.portal.util.WebKeys.PRINCIPAL_USER_ID}) {
            final Object value = session.getAttribute(key);
            if (value instanceof String && userId.equalsIgnoreCase((String) value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ends every session of the given user that is tracked on this node (the sessions it is the
     * user of, and the Login As sessions it is the administrator behind): each one is removed from
     * the tracked map, flagged with {@link #IGNORE_REMEMBER_ME_ON_INVALIDATION} and invalidated,
     * so the browser is told to log out. Sessions of other users are left alone. A session that
     * was already invalidated is dropped from the map and skipped, without stopping the others.
     * Sessions held by other nodes are not reached, and neither is a session this node no longer
     * tracks because the tracked map is bounded in size and time; those sessions are only
     * stopped by the status check made when they next resolve their user.
     *
     * @param userId the id of the user whose sessions end; a {@code null} or blank id ends nothing
     * @return how many sessions were ended
     */
    public static int endSessionsOf(final String userId) {
        if (!UtilMethods.isSet(userId)) {
            return 0;
        }
        int ended = 0;
        for (final Map.Entry<String, HttpSession> entry : userSessions.copyAsMap().entrySet()) {
            final HttpSession session = entry.getValue();
            try {
                if (!belongsTo(session, userId)) {
                    continue;
                }
                userSessions.invalidate(entry.getKey());
                session.setAttribute(IGNORE_REMEMBER_ME_ON_INVALIDATION, true);
                session.invalidate();
                ended++;
            } catch (final IllegalStateException e) {
                // the session was already invalidated elsewhere: forget it and go on
                userSessions.invalidate(entry.getKey());
            } catch (final RuntimeException e) {
                // one session failing to end must not keep the user's other sessions alive
                userSessions.invalidate(entry.getKey());
                Logger.warn(SessionMonitor.class,
                        "Could not end a tracked session of user '" + userId + "': " + e.getMessage(), e);
            }
        }
        if (ended > 0) {
            final int count = ended;
            SecurityLogger.logInfo(SessionMonitor.class,
                    () -> "Ended " + count + " session(s) of user '" + userId + "' on this node");
        }
        return ended;
    }

}
