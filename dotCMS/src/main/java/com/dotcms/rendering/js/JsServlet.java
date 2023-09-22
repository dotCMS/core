package com.dotcms.rendering.js;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.business.CloseDB;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.UserWebAPIImpl;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.LoginMode;
import com.dotmarketing.util.PageMode;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.apache.velocity.exception.ResourceNotFoundException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static com.dotmarketing.util.WebKeys.LOGIN_MODE_PARAMETER;

/**
 * Handles files with extension .jstl and runs those when accessed
 * @author jsanca
 */
public class JsServlet extends HttpServlet {

    private final UserWebAPIImpl userApi;
    private final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI;

    @VisibleForTesting
    public JsServlet(final UserWebAPI userApi, final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI) {
        this.userApi = (UserWebAPIImpl)userApi;
        this.htmlPageAssetRenderedAPI = htmlPageAssetRenderedAPI;
    }

    public JsServlet() {
        this(WebAPILocator.getUserWebAPI(), APILocator.getHTMLPageAssetRenderedAPI());
    }

    /*
     * Returns the page mode based on the login mode or the FE/BE roles
     */
    @VisibleForTesting
    public static PageMode processPageMode (final User user, final HttpServletRequest request) {

        final LoginMode loginMode = LoginMode.get(request);

        if (LoginMode.UNKNOWN == loginMode) {

            return user.isFrontendUser()
                    ? PageMode.setPageMode(request, PageMode.LIVE)
                    :  useNavigateMode(request, loginMode)
                    ? PageMode.setPageMode(request, PageMode.NAVIGATE_EDIT_MODE)
                    :  PageMode.setPageMode(request, PageMode.LIVE);
        }

        if ( LoginMode.FE == loginMode) {
            return PageMode.setPageMode(request, PageMode.LIVE);
        }

        return  useNavigateMode(request, loginMode) ?
                PageMode.setPageMode(request, PageMode.NAVIGATE_EDIT_MODE) : PageMode.setPageMode(request, PageMode.PREVIEW_MODE);
    }

    private static boolean useNavigateMode(final HttpServletRequest request, LoginMode loginMode) {


        if (LoginMode.FE == loginMode) {
            return false;
        }

        final boolean disabledNavigateMode = Boolean.parseBoolean(request.getParameter("disabledNavigateMode"));

        if (disabledNavigateMode) {
            return false;
        }

        final String referer = request.getHeader("referer");
        return referer != null && referer.contains("/dotAdmin");
    }

    @Override
    protected final void service(final HttpServletRequest request,
                                 final HttpServletResponse response) throws ServletException, IOException {

        final VelocityRequestWrapper requestWrapper = VelocityRequestWrapper.wrapVelocityRequest(request);
        final String uri = CMSUrlUtil.getCurrentURI(requestWrapper);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(requestWrapper);
        HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

        final User user = (userApi.getLoggedInUser(requestWrapper)!=null)
                ? userApi.getLoggedInUser(requestWrapper)
                : userApi.getLoggedInFrontendUser(requestWrapper) !=null
                ? userApi.getLoggedInFrontendUser(requestWrapper)
                : userApi.getAnonymousUserNoThrow();

        requestWrapper.setRequestUri(uri);
        final PageMode mode = processPageMode(user, requestWrapper);

        // if you are hitting the servlet without running through the other filters
        if (uri == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JsServlet called without running through the CMS Filter");
            Logger.error(this.getClass(),
                    "You cannot call the JsServlet without passing the requested url via a  requestAttribute called  "
                            + Constants.CMS_FILTER_URI_OVERRIDE);
            return;
        }

        // if you are not running ee
        if (!LicenseUtil.isASAllowed()) {
            Logger.error(this, "Enterprise License is required");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {

            final Reader reader       = new StringReader("?????"); // todo: this should be recover from the site
            final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ScriptEngineFactory.JAVASCRIPT_ENGINE);
            final Object result       = engine.eval(requestWrapper, response, reader,
                    CollectionsUtils.map("mode", mode,
                            "user", user,
                            "uri", uri));

            final String output = result.toString();
            response.getOutputStream().write(output.getBytes());
        } catch (ResourceNotFoundException rnfe) {
            Logger.warnAndDebug(this.getClass(), "ResourceNotFoundException" + rnfe.toString(), rnfe);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } /*catch (DotSecurityException dse) {
            Logger.warnAndDebug(this.getClass(), dse.getMessage(),dse);
            if(!response.isCommitted()) {
                if(user==null || APILocator.getUserAPI().getAnonymousUserNoThrow().equals(user)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            }
        } */ catch (HTMLPageAssetNotFoundException hpnfe) {
            Logger.warnAndDebug(this.getClass(), hpnfe.getMessage(),hpnfe);
            if(!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IllegalStateException state) {
            // Eat this, client disconnect noise
            Logger.debug(this, ()-> "IllegalStateException" + state.toString());
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(),e);
            if(!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Exception Error on template");
            }

        }

    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        Logger.info(this.getClass(), "Initializing VelocityServlet");


    }

    /**
     * This will redirect an edit mode request to either the url that matches the page requested or if
     * it is a URLMap, to the url mapped page.
     *
     * @param requestURI
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private void goToEditPage(final String requestURI, final HttpServletRequest request, final HttpServletResponse response)
            throws  IOException {

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        final String url = String.format("/dotAdmin/#/edit-page/content?url=%s", requestURI);
        response.sendRedirect(url);
    }

    /**
     * This revise if the use is logged in from the frontend
     * @param request
     * @return
     */
    private boolean isFrontendLogin(final HttpServletRequest request){
        final HttpSession session = request.getSession(false);
        if (null != session) {
            final LoginMode mode = (LoginMode) session.getAttribute(LOGIN_MODE_PARAMETER);
            return (LoginMode.FE == mode);
        }
        return false;
    }
}
