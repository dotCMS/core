package com.dotcms.listeners;

import static com.dotcms.cms.login.LoginServiceAPIFactory.LOG_OUT_ATTRIBUTE;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.UserSessionPayloadBuilder;
import com.dotcms.cache.DynamicTTLCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import io.vavr.Lazy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final SystemEventsAPI systemEventsAPI;
    private final static Lazy<Long> SESSIONS_TO_TRACK_MAX = Lazy.of(() -> Config.getLongProperty("SESSIONS_TO_TRACK_MAX", 5000L));
    private final static Lazy<Long> SESSIONS_TO_TRACK_TTL = Lazy.of(() -> Config.getLongProperty("SESSIONS_TO_TRACK_TTL", 30 * 60 * 1000L));


    public SessionMonitor() {
        this(APILocator.getSystemEventsAPI());
    }
    public SessionMonitor(final SystemEventsAPI systemEventsAPI) {
        this.systemEventsAPI = systemEventsAPI;
    }


    // this will hold all logged in users
    private final DynamicTTLCache<String, String> sysUsers =new DynamicTTLCache<String, String>(SESSIONS_TO_TRACK_MAX.get(), SESSIONS_TO_TRACK_TTL.get());

    private final DynamicTTLCache<String, HttpSession> userSessions = new DynamicTTLCache<String, HttpSession>(SESSIONS_TO_TRACK_MAX.get(), SESSIONS_TO_TRACK_TTL.get());


    private final DynamicTTLCache<String, String> sysUsersAddress =new DynamicTTLCache<String, String>(SESSIONS_TO_TRACK_MAX.get(), SESSIONS_TO_TRACK_TTL.get());

    public Map<String, String> getSysUsers() {
        return sysUsers.copyAsMap();
    }

    public Map<String, HttpSession> getUserSessions() {

        return userSessions.copyAsMap();
    }

    public Map<String, String> getSysUsersAddress() {
        return sysUsersAddress.copyAsMap();
    }

    public void attributeAdded(HttpSessionBindingEvent event) {
        // Not implemented
    }

    /**
     * Checks if the attribute removed is "USER_ID". If so, remove the logout user
     */
    public void attributeRemoved(final HttpSessionBindingEvent event) {
        final String currentAttributeName = event.getName();

        if (currentAttributeName.equals(com.liferay.portal.util.WebKeys.USER_ID)) {

            String id = event.getSession().getId();

            sysUsers.invalidate(id);
            userSessions.invalidate(id);
            sysUsersAddress.invalidate(id);
        }
    }

    /**
     * Do nothing here.
     */
    public void attributeReplaced(HttpSessionBindingEvent event) {
        // Not implemented
    }

    public void sessionCreated(final HttpSessionEvent event) {
        // Not implemented
        Logger.debug(this, "Session created");
    }

    public void sessionDestroyed(final HttpSessionEvent event) {

        Logger.debug(this, "Session destroyed");

        HttpSession session = event.getSession();
        final String userId = (String) session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID);
        if (userId != null) {

            final String sessionId = session.getId();

            sysUsers.invalidate(sessionId);
            userSessions.invalidate(sessionId);
            sysUsersAddress.invalidate(sessionId);

            try {

                boolean ignoreRememberMe = session.getAttribute(IGNORE_REMEMBER_ME_ON_INVALIDATION) != null;
                Logger.debug(this, "Triggering a session destroyed event");

                final boolean isLogout =
                        event.getSession().getAttribute(LOG_OUT_ATTRIBUTE) != null && Boolean.parseBoolean(
                                event.getSession().getAttribute(LOG_OUT_ATTRIBUTE).toString());

                if (!isLogout) {
                    // sending this event to the websocket client will automatically logout the user
                    // which sounds great except for the case where the user has clicked "REMEMBER_ME"
                    if (ignoreRememberMe || Config.getBooleanProperty("SEND_SESSION_DESTROYED_TO_WEBSOCKET", false)) {
                        this.systemEventsAPI.push(new SystemEvent
                                (SystemEventType.SESSION_DESTROYED,
                                        UserSessionPayloadBuilder.build(userId, sessionId)));
                    }
                } else {
                    this.systemEventsAPI.push(new SystemEvent
                            (SystemEventType.SESSION_LOGOUT, UserSessionPayloadBuilder.build(userId, sessionId)));
                }
            } catch (DotDataException e) {

                Logger.debug(this, "Could not sent the session destroyed event" + e.getMessage(), e);
            }
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
            final String id = session.getId();
            sysUsers.put(id, userId);
            userSessions.put(id, session);
            sysUsersAddress.put(id, event.getServletRequest()
                    .getRemoteAddr());

            if (UtilMethods.isSet(userId) && !UserAPI.CMS_ANON_USER_ID.equalsIgnoreCase(userId)) {
                session.setAttribute(DOT_CLUSTER_SESSION, true);
            }
        }
        event.getServletContext().setAttribute(WebKeys.USER_SESSIONS, this);
    }

}
