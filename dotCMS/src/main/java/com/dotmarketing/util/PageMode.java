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

    /**
     * Resolves the {@link PageMode} for a sub-resource request (a stylesheet, image, or other file asset
     * pulled in by a rendered page) by also considering the {@code Referer}.
     * <p>
     * Such assets are fetched by the browser as standalone requests that carry no {@code ?mode=} of their
     * own, and dotCMS does not persist the page mode to the session when a page is viewed with
     * {@code ?mode=LIVE}. As a result {@link #get(HttpServletRequest)} alone falls back to a stale session
     * value or {@code PREVIEW_MODE} (working) for a backend user, leaking unpublished content into a LIVE
     * page view. To close that gap, when the request itself declares no {@code mode} and the caller is a
     * backend user, the {@code mode} declared on the {@code Referer} (the page URL that requested this
     * asset) is honored.
     * <p>
     * The backend-user gate preserves the security boundary: an anonymous request can never escape the
     * forced-LIVE behavior via a spoofed {@code Referer}, because it never reaches the fallback.
     *
     * @param request the current sub-resource request
     * @return the resolved {@link PageMode}
     */
    public static PageMode getWithReferer(final HttpServletRequest request) {
        final PageMode requestMode = get(request);

        if (request == null || null != request.getParameter(WebKeys.PAGE_MODE_PARAMETER)) {
            return requestMode;
        }

        final User user = PortalUtil.getUser(request);
        if (user == null || !user.isBackendUser()) {
            return requestMode;
        }

        final String refererMode = modeFromReferer(request.getHeader("Referer"));
        return UtilMethods.isSet(refererMode) ? get(refererMode) : requestMode;
    }

    /**
     * Extracts the {@code mode} query-string parameter from a {@code Referer} URL, if present.
     *
     * @param referer the raw {@code Referer} header value (may be {@code null})
     * @return the {@code mode} value declared on the Referer, or {@code null} if absent/unparseable
     */
    private static String modeFromReferer(final String referer) {
        if (!UtilMethods.isSet(referer) || !referer.contains("?")) {
            return null;
        }
        final String query = referer.substring(referer.indexOf('?') + 1);
        for (final String pair : query.split("&")) {
            final int eq = pair.indexOf('=');
            if (eq > 0 && WebKeys.PAGE_MODE_PARAMETER.equals(pair.substring(0, eq))) {
                return pair.substring(eq + 1);
            }
        }
        return null;
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
