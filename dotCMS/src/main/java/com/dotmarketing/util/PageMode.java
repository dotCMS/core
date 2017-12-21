package com.dotmarketing.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Usage:
 * 
 * PageMode mode = PageMode.get(request);
 * PageMode mode = PageMode.get(session);
 * 
 * mode.isAdmin ; mode.showLive ; mode.respectAnonPerms ;
 * 
 * 
 * if( PageMode.get(request).isAdmin){ doAdminStuff(); }
 * 
 * contentAPI.find("sad", user, mode.respectAnonPerms);
 * 
 * contentAPI.findByIdentifier("id", 1, mode.showLive, user, mode.respectAnonPerms);
 * 
 * PageMode.setPageMode(request, PageMode.PREVIEW_MODE);
 * 
 * 
 * 
 * @author will
 *
 */
public enum PageMode {

    LIVE(true, false), 
    ADMIN_MODE(true, true), 
    PREVIEW_MODE(false, true), 
    EDIT_MODE(false, true);

    public final boolean showLive;
    public final boolean isAdmin;
    public final boolean respectAnonPerms;

    PageMode(boolean live, boolean admin) {
        this.showLive = live;
        this.isAdmin = admin;
        this.respectAnonPerms=!admin;
    }


    public static PageMode get(final HttpSession ses) {

        PageMode mode = (ses != null && ses.getAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION) != null
                && ses.getAttribute("tm_date") == null)
                        ? (PageMode) ses.getAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION)
                        : LIVE;
        return mode;
    }

    public static PageMode get(final HttpServletRequest req) {
        if (req == null || req.getSession(false) == null || null!= req.getHeader("X-Requested-With")) {
            return LIVE;
        }
        return get(req.getSession());
    }
    
    public static PageMode get(final String modeStr) {
        for(PageMode mode : values()) {
                if(mode.name().equals(modeStr)) {
                    return mode;
                }
        }
        return LIVE;
    }
    public static void setPageMode(final HttpServletRequest request, boolean contentLocked, boolean canLock) {
        if (contentLocked && canLock) {
            setPageMode(request,EDIT_MODE);
        } else {
            setPageMode(request,PREVIEW_MODE);
        }
    }

    public static void setPageMode(final HttpServletRequest request, PageMode mode) {
        request.getSession().setAttribute(WebKeys.PAGE_MODE_SESSION, mode);
        request.setAttribute(WebKeys.PAGE_MODE_SESSION, mode);
    }

}
