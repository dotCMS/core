package com.dotmarketing.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by freddyrodriguez on 5/2/16.
 */
public abstract  class PageRequestModeUtil {

    public static boolean isAdminMode( HttpSession session ){
       if(session==null) return false;
        boolean timemachine = session.getAttribute("tm_date")!=null;
        return !timemachine && (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
    }

    public static boolean isPreviewMode( HttpSession session ){
       if(session==null) return false;
        boolean ADMIN_MODE = isAdminMode(session);
        boolean timemachine = session.getAttribute("tm_date")!=null;
        return !timemachine && ADMIN_MODE && (session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null);
    }

    public static boolean isEditMode( HttpSession session ){
       if(session==null) return false;
        boolean ADMIN_MODE = isAdminMode(session);
        boolean timemachine = session.getAttribute("tm_date")!=null;
        return !timemachine && ADMIN_MODE && (session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null);
    }
    
    public static boolean isEditMode( HttpServletRequest request ){
      return isEditMode(request.getSession(false));
    }
    public static boolean isPreviewMode( HttpServletRequest request ){
      return isPreviewMode(request.getSession(false));
    }
    public static boolean isAdminMode( HttpServletRequest request ){
      return isAdminMode(request.getSession(false));
    }
    
    public static boolean isPageMode(HttpSession session) {
        return !isAdminMode(session);
    }
    
    public static boolean isLive( HttpServletRequest request ){
      HttpSession session = request.getSession(false);
      if(session==null) return true;
      return !( isAdminMode(session) && (isEditMode(session) || isPreviewMode(session)));
    }
    
    /**
     * Activate the Edit or preview mode in session depending of the content state
     * @param request Http request
     * @param contentLocked boolean is the content locked
     * @param canLock boolean can the user loc the content
     */
    public static void setBackEndModeInSession( HttpServletRequest request, boolean contentLocked, boolean canLock ){
        HttpSession session = request.getSession(false);
        if(contentLocked && canLock){
			session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
			session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
			session.setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");
		}else{
			session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
			session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, "true");
			session.setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");
		}
      }
    
    
}
