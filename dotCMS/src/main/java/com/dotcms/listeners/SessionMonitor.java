package com.dotcms.listeners;

import com.dotcms.api.system.event.*;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
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

import static com.dotcms.cms.login.LoginServiceAPIFactory.LOG_OUT_ATTRIBUTE;

/**
 * Listener that keeps track of logged in users by monitoring for USER_ID
 * session attribute addition.
 * 
 * By: IPFW Web Team
 */
public class SessionMonitor implements ServletRequestListener,
        HttpSessionAttributeListener, HttpSessionListener {


    private final SystemEventsAPI systemEventsAPI;

    public SessionMonitor() {

        this (APILocator.getSystemEventsAPI());
    }

    public SessionMonitor(final SystemEventsAPI systemEventsAPI) {

        this.systemEventsAPI = systemEventsAPI;
    }

    
    // this will hold all logged in users
    private Map<String, String> sysUsers = new ConcurrentHashMap<String, String>();
    private Map<String, HttpSession> userSessions = new ConcurrentHashMap<String, HttpSession>();
    private Map<String, String> sysUsersAddress = new ConcurrentHashMap<String, String>();
    
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
    }
    
    /**
     * Checks if the attribute removed is "USER_ID". If so, remove the logout
     * user
     * 
     */
    public void attributeRemoved(HttpSessionBindingEvent event) {
        String currentAttributeName = event.getName().toString();
        
        if (currentAttributeName.equals("USER_ID")) {
            
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
    }
    
    public void sessionCreated(final HttpSessionEvent event) {

        /*final String userId = (String) event.getSession().getAttribute("USER_ID");
        if (userId != null) {
            try {

                Logger.debug(this, "Triggering a session created event");

                this.systemEventsAPI.push(new SystemEvent
                        (SystemEventType.SESSION_CREATED, new Payload(new Date())));
            } catch (DotDataException e) {

                Logger.debug(this, "Could not sent the session created event" + e.getMessage(), e);
            }
        }*/
    }
    
    public void sessionDestroyed(final HttpSessionEvent event) {

        final String userId = (String) event.getSession().getAttribute("USER_ID");
        if (userId != null) {

            final String sessionId = event.getSession().getId();

            sysUsers.remove(sessionId);
            userSessions.remove(sessionId);
            sysUsersAddress.remove(sessionId);

            try {

                Logger.debug(this, "Triggering a session destroyed event");

                final Boolean isLogout = event.getSession().getAttribute(LOG_OUT_ATTRIBUTE) != null ?
                        Boolean.parseBoolean(event.getSession().getAttribute(LOG_OUT_ATTRIBUTE).toString()) :
                        false;

                if (!isLogout) {
                    this.systemEventsAPI.push(new SystemEvent
                            (SystemEventType.SESSION_DESTROYED, UserSessionPayloadBuilder.build(userId, sessionId)));
                }
            } catch (DotDataException e) {

                Logger.debug(this, "Could not sent the session destroyed event" + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public void requestDestroyed(ServletRequestEvent arg0) {
    }
    
    @Override
    public void requestInitialized(ServletRequestEvent event) {
        HttpSession session = ((HttpServletRequest) event.getServletRequest())
                .getSession(false);
        if (session != null && session.getAttribute("USER_ID") != null) {
            String userId = (String) session.getAttribute("USER_ID");
            String id = session.getId();
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
        }
        event.getServletContext().setAttribute(WebKeys.USER_SESSIONS, this);
    }
    
}
