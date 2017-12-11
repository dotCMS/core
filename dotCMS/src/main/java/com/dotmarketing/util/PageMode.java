package com.dotmarketing.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Usage:
 * 
 * PageMode mode = PageMode.get(request);
 * 
 * mode.isAdmin ; mode.showLive ;
 * 
 * or
 * 
 * if( PageMode.get(request).isAdmin){ doAdminStuff(); }
 * 
 * @author will
 *
 */
public enum PageMode {

    ANON(true, false), 
    LIVE(true, true), PREVIEW(false, true), EDIT(false, true);

    public final boolean showLive;
    public final boolean isAdmin;

    PageMode(boolean live, boolean admin) {
        this.showLive = live;
        this.isAdmin = admin;
    }


    public static PageMode get(HttpSession ses) {

        PageMode mode = (ses != null && ses.getAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION) != null
                && ses.getAttribute("tm_date") == null)
                        ? (PageMode) ses.getAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION)
                        : ANON;

        return mode;
    }

    public static PageMode get(HttpServletRequest req) {
        if (req == null || req.getSession(false) == null) {
            return ANON;
        }
        return get(req.getSession());
    }

    public static void setPageMode(HttpServletRequest request, boolean contentLocked, boolean canLock) {
        if (contentLocked && canLock) {
            setPageMode(request,EDIT);
        } else {
            setPageMode(request,PREVIEW);
        }
    }

    public static void setPageMode(HttpServletRequest request, PageMode mode) {
        request.getSession().setAttribute(WebKeys.PAGE_MODE_SESSION, mode);
        request.setAttribute(WebKeys.PAGE_MODE_SESSION, mode);
    }

}
