package com.dotcms.rendering.velocity.viewtools.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

public final class NavResultHydrated extends NavResult{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /** Prefixes of the Page API endpoints that render a page through Velocity. */
    private static final String RENDER_PREFIX = "/api/v1/page/render/";
    private static final String RENDER_HTML_PREFIX = "/api/v1/page/renderHTML/";

    /**
     * Supplies the configured index page name ({@code CMS_INDEX_PAGE}, default {@code index}) —
     * the same value {@link com.dotmarketing.filters.CMSFilter} appends when it normalizes a
     * folder URL on the front end. Package-private and mutable purely so tests can vary it:
     * {@code HTMLPageAssetAPIImpl.CMS_INDEX_PAGE} is a {@code static final} vavr {@code Lazy}
     * that memoizes on first read, so {@code Config.setProperty} cannot change it afterwards.
     */
    @VisibleForTesting
    static Supplier<String> indexPageName = HTMLPageAssetAPIImpl.CMS_INDEX_PAGE::get;

    final NavResult navResult;
    final transient ViewContext context;

    public NavResultHydrated(final NavResult navResult, final ViewContext context) {
        super(navResult.getUnhydratedNavResult());
        this.navResult = navResult.getUnhydratedNavResult();
        this.context = context;
        this.setType(navResult.getType());
    }
    @Override
    @JSONIgnore
    public NavResult getUnhydratedNavResult() {
      return this.navResult;
    }
    

    /**
     * Tells whether this navigation item is the one the current request is on, which is what
     * {@code $nav.active} resolves to in a Velocity template. Templates typically use it to mark
     * the current section and keep its children expanded.
     *
     * @return {@code true} when this item is the current page, or a folder containing it
     */
    public boolean isActive() {
        if (context == null || !UtilMethods.isSet(navResult.getHref())) {
            return false;
        }
        final HttpServletRequest req = (HttpServletRequest) context.getRequest();
        return req != null
                && isActive(req.getRequestURI(), navResult.getHref(), isFolder(), isCodeLink());
    }

    /**
     * The active-state decision, pulled out of {@link #isActive()} as a pure function of its
     * inputs so it can be exercised directly over the full range of request URI shapes. Holds no
     * state and reads nothing beyond its arguments.
     *
     * @param requestURI the raw request URI, still carrying any Page API prefix
     * @param href       the navigation item's URL path
     * @param folder     whether the item is a folder rather than a page or link
     * @param codeLink   whether the item is a Velocity code link, which is never active
     * @return {@code true} when the item is the current page, or a folder containing it
     */
    @VisibleForTesting
    static boolean isActive(final String requestURI, final String href, final boolean folder,
            final boolean codeLink) {

        // A Page API render carries a prefix; the front end does not. Which it is decides whether
        // the index candidate below is even considered.
        final String strippedURI = stripRenderPrefix(requestURI);
        final String reqURI = strippedURI != null ? strippedURI : requestURI;

        if (matches(reqURI, href, folder, codeLink)) {
            return true;
        }

        // Only the Page API needs the second attempt. CMSFilter has already redirected a folder URL
        // and appended the index page name before Velocity sees a front-end request, so a front-end
        // URI always carries a page segment and is matched above, exactly as it always has been.
        // The Page API is a JAX-RS resource outside that filter, so a folder-style URL such as
        // /api/v1/page/render/TravelHub arrives with no page segment at all -- which is what the
        // Universal Visual Editor sends when it navigates in-editor. See issue #37105.
        if (strippedURI == null) {
            return false;
        }
        final String indexCandidate = indexCandidate(reqURI);
        return indexCandidate != null && matches(indexCandidate, href, folder, codeLink);
    }

    /**
     * Removes the prefix of a Page API rendering endpoint, keeping the leading slash of the page
     * path.
     *
     * <p>Both tests require the trailing slash, which makes them mutually exclusive so their order
     * cannot matter, and stops a page path that merely begins with {@code render} being mangled.
     * {@code /api/v1/page/_render-sources/} is deliberately not handled: it returns references only
     * and renders no Velocity, so no navigation item is ever hydrated on that path.
     *
     * @param requestURI the raw request URI
     * @return the page path, or {@code null} when the URI carries no rendering prefix
     */
    private static String stripRenderPrefix(final String requestURI) {
        if (requestURI.startsWith(RENDER_HTML_PREFIX)) {
            return requestURI.substring(RENDER_HTML_PREFIX.length() - 1);
        }
        if (requestURI.startsWith(RENDER_PREFIX)) {
            return requestURI.substring(RENDER_PREFIX.length() - 1);
        }
        return null;
    }

    /**
     * Builds the URI this request would have had if it had named the folder's index page, which is
     * the shape {@link com.dotmarketing.filters.CMSFilter} hands the front end.
     *
     * @param reqURI the prefix-stripped request URI
     * @return the candidate URI, or {@code null} when the URI already names the index page
     */
    private static String indexCandidate(final String reqURI) {
        final String indexPage = indexPageName.get();
        if (reqURI.endsWith(StringPool.SLASH + indexPage)) {
            return null;
        }
        return reqURI.endsWith(StringPool.SLASH)
                ? reqURI + indexPage
                : reqURI + StringPool.SLASH + indexPage;
    }

    /**
     * The original active-state test, unchanged but for a guard against a URI holding no slash.
     *
     * @param uri      the URI to compare against
     * @param href     the navigation item's URL path
     * @param folder   whether the item is a folder
     * @param codeLink whether the item is a Velocity code link
     * @return whether the item is active for this URI
     */
    private static boolean matches(final String uri, final String href, final boolean folder,
            final boolean codeLink) {

        final int lastSlash = uri.lastIndexOf('/');
        if (lastSlash < 0) {
            return false;
        }
        // We exclude the page name from the Request URI so we can check if page's parent
        // object is the real active object
        String parentPath = uri.substring(0, lastSlash);
        if (!parentPath.endsWith(StringPool.SLASH)) {
            // Adding a slash at the end of the path, so it avoids false positives
            // when two or more paths from the same level starts with the same name
            parentPath = parentPath + StringPool.SLASH;
        }
        // If the current item is a folder, we check if it's part of current URI
        if (folder && !href.endsWith(StringPool.SLASH)) {
            return parentPath.startsWith(href + StringPool.SLASH);
        }
        // If it's a page, we check instead if it's the current URI
        return !codeLink && href.equalsIgnoreCase(uri);
    }

    @Override
    public String getCodeLink() {
        if (navResult.getCodeLink() != null && (navResult.getCodeLink()
            .contains("$")
                || navResult.getCodeLink()
                    .contains("#"))) {
            return UtilMethods.evaluateVelocity(navResult.getCodeLink(), context.getVelocityContext());
        } else {
            return navResult.getCodeLink();
        }
    }
    @Override
    public String getTitle() throws Exception {
        return navResult.getTitle();
    }

    @Override
    public String getHostId() {
        return navResult.getHostId();
    }

    @Override
    public String getHref() {
        return navResult.getHref();
    }

    @Override
    public boolean isShowOnMenu() {
        return navResult.isShowOnMenu();
    }

    @Override
    public boolean isCodeLink() {
        return navResult.isCodeLink();
    }

    @Override
    public int getOrder() {
        return navResult.getOrder();
    }


    @Override
    public boolean isFolder() {
        return navResult.isFolder();
    }

    @Override
    public List<? extends NavResult> getChildren() throws Exception {

        final List<NavResultHydrated> navList = this.navResult.getChildren().stream()
                .map(result -> new NavResultHydrated(result, this.context)).collect(Collectors.toList());

        if (Config.getBooleanProperty("ENABLE_NAV_PERMISSION_CHECK", false)) {
            // now filtering permissions
            final HttpServletRequest request        = this.context.getRequest();
            User currentUser                        = WebAPILocator.getUserWebAPI().getLoggedInUser(request);

            if (currentUser == null) {

                currentUser = APILocator.getUserAPI().getAnonymousUser();
            } else {

                if (currentUser.isAdmin()) {

                    return navList;
                }
            }

            final List<NavResult>    navAllowedList = new ArrayList<>(navList.size());

            for (final NavResult navResult : navList) {

                try {
                    if (APILocator.getPermissionAPI()
                        .doesUserHavePermission(navResult, PermissionAPI.PERMISSION_READ, currentUser)) {

                        navAllowedList.add(navResult);
                    }
                } catch (Exception ex) {
                    Logger.error(this, ex.getMessage(), ex);
                }
            }

            return navAllowedList;
        }

        return navList;
    }

    @Override
    public String getParentPath() throws DotDataException, DotSecurityException {
        return navResult.getParentPath();
    }


    @Override
    public NavResult getParent() throws DotDataException, DotSecurityException {
        return navResult.getParent();
    }


    @Override
    public List<String> getChildrenFolderIds() {
        return navResult.getChildrenFolderIds();
    }

    @Override
    public String getType() {
        return navResult.getType();
    }


    @Override
    public String getTarget() {
        return navResult.getTarget();
    }

    @Override
    public String getOwner() {
        return navResult.getOwner();
    }

    @Override
    public String getPermissionId() {
        return navResult.getPermissionId();
    }




}
