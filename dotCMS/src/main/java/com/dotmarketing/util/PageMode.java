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

    private static final PageMode DEFAULT_PAGE_MODE = LIVE;

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
        final HttpServletRequest req = Try.of(HttpServletRequestThreadLocal.INSTANCE::getRequest).getOrNull();
        return get(req);

    }

    public static PageMode get(final HttpServletRequest request) {

        if (request == null || null!= request.getHeader("X-Requested-With")) {

            return DEFAULT_PAGE_MODE;
        }

        final User user = PortalUtil.getUser(request);

        // only backend users can see non-live assets
        if (user == null || !user.isBackendUser()) {
            return DEFAULT_PAGE_MODE;
        }

        if (null != request.getParameter(WebKeys.PAGE_MODE_PARAMETER)) {
            final PageMode pageMode = PageMode.get(request.getParameter(WebKeys.PAGE_MODE_PARAMETER));
            request.setAttribute(WebKeys.PAGE_MODE_PARAMETER, pageMode);
            return pageMode;
        }

        if (null != request.getAttribute(WebKeys.PAGE_MODE_PARAMETER)) {
            return (PageMode) request.getAttribute(WebKeys.PAGE_MODE_PARAMETER);
        }

        final HttpSession session = request.getSession(false);
        if (null !=session && null != session.getAttribute(WebKeys.PAGE_MODE_SESSION)) {
            return (PageMode) session.getAttribute(WebKeys.PAGE_MODE_SESSION);
        }

        Logger.debug(PageMode.class,()->String.format("Setting PREVIEW_MODE for uri `%s`", request.getRequestURI()));

        return PREVIEW_MODE;
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
    public static PageMode setPageMode(final HttpServletRequest request, final PageMode mode){
        return setPageMode(request, mode, true);
    }

    /**
     * Page mode can only be set for back end users, not for front end users (even logged in Front end users)
     * We should avoid setting the mode in the session.
     * Session should be used to keep immutable data. like the user of company. But not state data that might dictate the behavior of the UI.
     * I'm introducing this method to avoid setting the mode in the session.
     * Many times passing stuff down the request is enough.
     * @param request HttpServletRequest
     * @param mode PageMode to set
     * @param setSession if true, the mode will be set in the session
     * @return
     */
    public static PageMode setPageMode(final HttpServletRequest request, final PageMode mode, final boolean setSession) {
        if (DEFAULT_PAGE_MODE != mode) {
            final User user = PortalUtil.getUser(request);
            if (user == null || !user.isBackendUser()) {
                return DEFAULT_PAGE_MODE;
            }
        }
        //here we're saying that... we want to set page mode in the request and not override the session actual value
        if( setSession && null != request.getSession(false)) {
            request.getSession().setAttribute(WebKeys.PAGE_MODE_SESSION, mode);
        }
        request.setAttribute(WebKeys.PAGE_MODE_PARAMETER, mode);
        return mode;
    }

    /**
     * Checks if the current Page Mode belongs to Edit Mode.
     *
     * @return If only working Contentlet versions are being returned and the Admin Mode is set, returns {@code true}.
     */
    public boolean isEditMode() {
        return this.equals(PageMode.EDIT_MODE);
    }

    @Override
    public String toString() {
        return this.name();
    }

}
