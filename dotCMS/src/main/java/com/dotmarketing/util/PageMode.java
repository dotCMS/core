package com.dotmarketing.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;
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
    ADMIN_MODE(true, true, true),
    PREVIEW_MODE(false, true),
    WORKING(false, true),
    EDIT_MODE(false, true),
    NAVIGATE_EDIT_MODE(false, true);

    private static PageMode DEFAULT_PAGE_MODE = LIVE;

    public final boolean showLive;
    public final boolean isAdmin;
    public final boolean respectAnonPerms;

    PageMode(boolean live, boolean admin) {
        this(live, admin, !admin);
    }

    PageMode(final boolean live, final boolean admin, final boolean respectAnonPerms) {
        this.showLive = live;
        this.isAdmin = admin;
        this.respectAnonPerms = respectAnonPerms;
    }


    public static PageMode get() {
        final HttpServletRequest req = Try.of(()->HttpServletRequestThreadLocal.INSTANCE.getRequest()).getOrNull();
        return get(req);

    }
    

    private static PageMode get(final HttpSession ses) {

        PageMode mode = PageMode.isPageModeSet(ses)
                        ? (PageMode) ses.getAttribute(WebKeys.PAGE_MODE_SESSION)
                        : DEFAULT_PAGE_MODE;

        return mode;
    }

    public static PageMode getWithNavigateMode(final HttpServletRequest req) {
        HttpSession ses = req.getSession(false);
        PageMode mode = PageMode.isPageModeSet(ses)
                ? PageMode.getCurrentPageMode(ses)
                : DEFAULT_PAGE_MODE;

        return mode;
    }

    public static PageMode get(final HttpServletRequest request) {

        if (request == null || null!= request.getHeader("X-Requested-With")) {

            return DEFAULT_PAGE_MODE;
        }

        PageMode pageMode = null;

        if (null != request.getParameter(WebKeys.PAGE_MODE_PARAMETER)) {

            pageMode = PageMode.get(request.getParameter(WebKeys.PAGE_MODE_PARAMETER));
            request.setAttribute(WebKeys.PAGE_MODE_PARAMETER, pageMode);
        }

        if (null == pageMode && null != request.getAttribute(WebKeys.PAGE_MODE_PARAMETER)) {

            pageMode = (PageMode) request.getAttribute(WebKeys.PAGE_MODE_PARAMETER);
        }

        final HttpSession session = request.getSession(false);
        if (null == pageMode) {
            pageMode = get(session);
        }

        if (DEFAULT_PAGE_MODE != pageMode) {

            final User user = PortalUtil.getUser(request);

            if (user == null || !user.isBackendUser()) {
                pageMode = DEFAULT_PAGE_MODE;
            }
        }
        if(Logger.isDebugEnabled(PageMode.class)){
           Logger.debug(PageMode.class,String.format("PageMode for uri `%s` is `%s`", request.getRequestURI(), pageMode));
        }
        return pageMode;
    }
    
    public static PageMode get(final String modeStr) {
        for(final PageMode mode : values()) {
                if(mode.name().equalsIgnoreCase(modeStr)) {
                    return mode;
                }
        }
        return DEFAULT_PAGE_MODE;
    }

    public static PageMode setPageMode(final HttpServletRequest request, boolean contentLocked, boolean canLock) {
        
        PageMode mode = PREVIEW_MODE;
        if (contentLocked && canLock) {
            mode=EDIT_MODE;
        } 
        return setPageMode(request,mode);

    }
    
    
    /**
     * Page mode can only be set for back end users, not for front end users (even logged in Front end users)
     * @param request
     * @param mode
     * @return
     */
    public static PageMode setPageMode(final HttpServletRequest request, final PageMode mode) {
        
        if (DEFAULT_PAGE_MODE != mode) {
            final User user = PortalUtil.getUser(request);
            if (user == null || !user.isBackendUser()) {
                return DEFAULT_PAGE_MODE;
            }
        }
        
        if(request.getSession(false)!=null) {
            request.getSession().setAttribute(WebKeys.PAGE_MODE_SESSION, mode);
        }
        request.setAttribute(WebKeys.PAGE_MODE_PARAMETER, mode);
        return mode;
    }

    private static boolean isPageModeSet(final HttpSession ses) {
        return (ses != null && ses.getAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION) != null
                && ses.getAttribute("tm_date") == null);
    }

    private static PageMode getCurrentPageMode(final HttpSession ses) {
        PageMode sessionPageMode = ses==null ? DEFAULT_PAGE_MODE : (PageMode) ses.getAttribute(WebKeys.PAGE_MODE_SESSION);

        if (isNavigateEditMode(ses)) {
            return PageMode.NAVIGATE_EDIT_MODE;
        } else {
            return sessionPageMode;
        }
    }

    private static boolean isNavigateEditMode(final HttpSession ses) {
        PageMode sessionPageMode = ses==null ? DEFAULT_PAGE_MODE : (PageMode) ses.getAttribute(WebKeys.PAGE_MODE_SESSION);
        HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        final User user = PortalUtil.getUser(request);
        if (user == null || !user.isBackendUser()) {
            return false;
        }
        
        
        return  sessionPageMode != PageMode.LIVE &&
                request != null &&
                request.getAttribute(WebKeys.PAGE_MODE_PARAMETER) == null ;
    }

}
