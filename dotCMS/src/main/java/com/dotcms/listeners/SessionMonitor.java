package com.dotcms.listeners;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.UserSessionPayloadBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dotcms.cms.login.LoginServiceAPIFactory.LOG_OUT_ATTRIBUTE;

/**
 * Listener that keeps track of logged in users by monitoring for USER_ID
 * session attribute addition.
 * 
 * By: IPFW Web Team
 */
public class SessionMonitor implements ServletRequestListener,
        HttpSessionAttributeListener, HttpSessionListener {

    public static final String DOT_CLUSTER_SESSION = "DOT_CLUSTER_SESSION";

    private final SystemEventsAPI systemEventsAPI;

    public SessionMonitor() {

        this (APILocator.getSystemEventsAPI());
    }

    public SessionMonitor(final SystemEventsAPI systemEventsAPI) {

        this.systemEventsAPI = systemEventsAPI;
    }

    
    // this will hold all logged in users
    private final Map<String, String> sysUsers = new ConcurrentHashMap<>();
    private final Map<String, HttpSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sysUsersAddress = new ConcurrentHashMap<>();
    
    public Map<String, String> getSysUsers() {
        return sysUsers;
    }
    
    public Map<String, HttpSession> getUserSessions() {
        return userSessions;
    }
    
    public Map<String, String> getSysUsersAddress() {
        return sysUsersAddress;
    }
    
    public void attributeAdded(HttpSessionBindingEvent event) {
        // Not implemented
    }
    
    /**
     * Checks if the attribute removed is "USER_ID". If so, remove the logout
     * user
     * 
     */
    public void attributeRemoved(final HttpSessionBindingEvent event) {
        final String currentAttributeName = event.getName();
        
        if (currentAttributeName.equals(com.liferay.portal.util.WebKeys.USER_ID)) {
            
            String id = event.getSession().getId();
            
            sysUsers.remove(id);
            userSessions.remove(id);
            sysUsersAddress.remove(id);
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
        final String userId = (String) event.getSession().getAttribute(com.liferay.portal.util.WebKeys.USER_ID);
        if (userId != null) {

            final String sessionId = event.getSession().getId();

            sysUsers.remove(sessionId);
            userSessions.remove(sessionId);
            sysUsersAddress.remove(sessionId);

            try {

                Logger.debug(this, "Triggering a session destroyed event");

                final boolean isLogout =
                        event.getSession().getAttribute(LOG_OUT_ATTRIBUTE) != null && Boolean.parseBoolean(event.getSession().getAttribute(LOG_OUT_ATTRIBUTE).toString());

                if (!isLogout) {
                    this.systemEventsAPI.push(new SystemEvent
                            (SystemEventType.SESSION_DESTROYED, UserSessionPayloadBuilder.build(userId, sessionId)));
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
            synchronized (sysUsers) {
                if (!sysUsers.containsKey(id)) {
                    sysUsers.put(id, userId);
                }
            }
            synchronized (userSessions) {
                if (!userSessions.containsKey(id)) {
                    userSessions.put(id, session);
                }
            }
            synchronized (sysUsersAddress) {
                if (!sysUsersAddress.containsKey(id)) {
                    sysUsersAddress.put(id, event.getServletRequest()
                            .getRemoteAddr());
                }
            }
            if (UtilMethods.isSet(userId) && !UserAPI.CMS_ANON_USER_ID.equalsIgnoreCase(userId)) {
                session.setAttribute(DOT_CLUSTER_SESSION, true);
            }
        }
        event.getServletContext().setAttribute(WebKeys.USER_SESSIONS, this);
    }
    
}
