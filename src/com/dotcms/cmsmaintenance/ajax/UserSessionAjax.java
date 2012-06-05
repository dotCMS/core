package com.dotcms.cmsmaintenance.ajax;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.directwebremoting.WebContextFactory;

import com.dotcms.listeners.SessionMonitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class UserSessionAjax {
    public boolean validateUser() {
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = null;
        try {
            user = com.liferay.portal.util.PortalUtil.getUser(req);
            if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)){
                throw new DotSecurityException("User does not have access to the CMS Maintance Portlet");
            }
            return true;
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            throw new DotRuntimeException (e.getMessage());
        }
    }

    public void invalidateSession(String sessionId) throws PortalException, SystemException, NoSuchUserException, DotDataException, DotSecurityException {
        validateUser();
        SessionMonitor sm = (SessionMonitor)
                WebContextFactory.get().getServletContext().getAttribute(WebKeys.USER_SESSIONS);

        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);

        if(sm.getUserSessions().containsKey(sessionId)) {
            HttpSession session=sm.getUserSessions().get(sessionId);
            User user=APILocator.getUserAPI().loadUserById(sm.getSysUsers().get(sessionId), APILocator.getUserAPI().getSystemUser(), false);

            if(!currentUser.getUserId().equals(user.getUserId())) {
        		session.invalidate();
        	} else {
        		throw new IllegalArgumentException("can't invalidate session "+sessionId);
        	}
        }
        else {
            throw new IllegalArgumentException("can't invalidate session "+sessionId);
        }
    }

    public void invalidateAllSessions() throws PortalException, SystemException, NoSuchUserException, DotDataException, DotSecurityException {
        validateUser();
        SessionMonitor sm = (SessionMonitor)
                WebContextFactory.get().getServletContext().getAttribute(WebKeys.USER_SESSIONS);

        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);

        for(String id : sm.getSysUsers().keySet()) {
        	HttpSession session=sm.getUserSessions().get(id);
        	User user=APILocator.getUserAPI().loadUserById(sm.getSysUsers().get(id), APILocator.getUserAPI().getSystemUser(), false);

        	if(!currentUser.getUserId().equals(user.getUserId())) {
        		session.invalidate();
        	}
        }
    }

    public List<Map<String,String>> getSessionList() throws NoSuchUserException, DotDataException, DotSecurityException {
        validateUser();
        List<Map<String,String>> sessionList=new ArrayList<Map<String,String>>();
        SessionMonitor sm = (SessionMonitor)
                WebContextFactory.get().getServletContext().getAttribute(WebKeys.USER_SESSIONS);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for(String id : sm.getSysUsers().keySet()) {
            Map<String,String> ss=new HashMap<String,String>();
            ss.put("sessionId",id);
            User user=APILocator.getUserAPI().loadUserById(sm.getSysUsers().get(id), APILocator.getUserAPI().getSystemUser(), false);
            ss.put("userId",user.getUserId());
            ss.put("userEmail", user.getEmailAddress());
            ss.put("userFullName", user.getFullName());
            ss.put("address", sm.getSysUsersAddress().get(id));
            HttpSession session=sm.getUserSessions().get(id);
            ss.put("sessionTime", sdf.format(System.currentTimeMillis()-session.getCreationTime()));
            sessionList.add(ss);
        }
        return sessionList;
    }
}