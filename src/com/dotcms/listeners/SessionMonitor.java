package com.dotcms.listeners;

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

import com.dotmarketing.util.WebKeys;

/**
 * Listener that keeps track of logged in users by monitoring for USER_ID
 * session attribute addition.
 * 
 * By: IPFW Web Team
 */
public class SessionMonitor implements ServletRequestListener,
        HttpSessionAttributeListener, HttpSessionListener {
    
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
    
    public void sessionCreated(HttpSessionEvent event) {
    }
    
    public void sessionDestroyed(HttpSessionEvent event) {
        String userId = (String) event.getSession().getAttribute("USER_ID");
        if (userId != null) {
            String id = event.getSession().getId();
            sysUsers.remove(id);
            userSessions.remove(id);
            sysUsersAddress.remove(id);
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
