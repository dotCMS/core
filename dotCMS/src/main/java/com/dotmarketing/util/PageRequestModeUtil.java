package com.dotmarketing.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 
 * Use {link {@link PageMode}
 * @author will
 *
 */
@Deprecated
public abstract  class PageRequestModeUtil {

    public static boolean isAdminMode( HttpSession session ){
       return PageMode.get(session).isAdmin;
    }

    public static boolean isPreviewMode( HttpSession session ){
        return PageMode.get(session) == PageMode.PREVIEW_MODE;
    }

    public static boolean isEditMode( HttpSession session ){
        return PageMode.get(session) == PageMode.EDIT_MODE;
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
      return PageMode.get(request).showLive;
    }
    
    /**
     * Activate the Edit or preview mode in session depending of the content state
     * @param request Http request
     * @param contentLocked boolean is the content locked
     * @param canLock boolean can the user loc the content
     */
    public static void setBackEndModeInSession( HttpServletRequest request, boolean contentLocked, boolean canLock ){

        if(contentLocked && canLock){
            PageMode.setPageMode(request, PageMode.EDIT_MODE);
		}else{
		    PageMode.setPageMode(request, PageMode.PREVIEW_MODE);
		}
      }
    
    
}
