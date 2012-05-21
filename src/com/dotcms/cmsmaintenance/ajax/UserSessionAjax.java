package com.dotcms.cmsmaintenance.ajax;

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
    
    @SuppressWarnings("unchecked")
    public void invalidateSession(String sessionId) {
        validateUser();
        SessionMonitor sm = (SessionMonitor)
                WebContextFactory.get().getServletContext().getAttribute(WebKeys.USER_SESSIONS);
        if(sm.getUserSessions().containsKey(sessionId)) {
            HttpSession session=sm.getUserSessions().get(sessionId);
            session.invalidate();
        }
        else {
            throw new IllegalArgumentException("can't invalidate session "+sessionId);
        }
    }
    
    public List<Map<String,String>> getSessionList() throws NoSuchUserException, DotDataException, DotSecurityException {
        validateUser();
        List<Map<String,String>> sessionList=new ArrayList<Map<String,String>>();
        SessionMonitor sm = (SessionMonitor)
                WebContextFactory.get().getServletContext().getAttribute(WebKeys.USER_SESSIONS);
        for(String id : sm.getSysUsers().keySet()) {
            Map<String,String> ss=new HashMap<String,String>();
            ss.put("sessionId",id);
            User user=APILocator.getUserAPI().loadUserById(sm.getSysUsers().get(id), APILocator.getUserAPI().getSystemUser(), false);
            ss.put("userId",user.getUserId());
            ss.put("userEmail", user.getEmailAddress());
            ss.put("userFullName", user.getFullName());
            ss.put("address", sm.getSysUsersAddress().get(id));
            sessionList.add(ss);
        }
        return sessionList;
    }
}