package com.dotcms.rendering.velocity.servlet;

import static com.dotmarketing.util.WebKeys.LOGIN_MODE_PARAMETER;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.business.CloseDB;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.UserWebAPIImpl;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.LoginMode;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.exception.ResourceNotFoundException;

public class VelocityServlet extends HttpServlet {


    private final UserWebAPIImpl userApi;
    private final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI;

    @VisibleForTesting
    public VelocityServlet(final UserWebAPI userApi, final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI) {
        this.userApi = (UserWebAPIImpl)userApi;
        this.htmlPageAssetRenderedAPI = htmlPageAssetRenderedAPI;
    }

    public VelocityServlet() {
        this(WebAPILocator.getUserWebAPI(),APILocator.getHTMLPageAssetRenderedAPI());
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /*
    * Returns the page mode based on the login mode or the FE/BE roles
     */
    public static PageMode processPageMode (final User user, final HttpServletRequest request) {

        final LoginMode loginMode = LoginMode.get(request);
        Logger.debug(VelocityServlet.class, "VelocityServlet_processPageMode LoginMode: " + loginMode.toString());

        if (null != request.getParameter(WebKeys.PAGE_MODE_PARAMETER)){
            return PageMode.get(request);
        }

        if (LoginMode.UNKNOWN == loginMode) {
            return determinePageMode(request, user, LoginMode.UNKNOWN);
        }

        if ( LoginMode.FE == loginMode) {
            return PageMode.setPageMode(request, PageMode.LIVE, false);
        }

        return  useNavigateMode(request, loginMode) ?
                 PageMode.setPageMode(request, PageMode.NAVIGATE_EDIT_MODE, false) : PageMode.setPageMode(request, PageMode.PREVIEW_MODE, false);
    }

    /**
     * This method will determine the page mode based on the user and the login mode
     * @param request HttpServletRequest
     * @param user User
     * @param loginMode LoginMode
     * @return PageMode
     */
    private static PageMode determinePageMode(HttpServletRequest request, User user, LoginMode loginMode) {
        if (user.isFrontendUser()) {
            return PageMode.setPageMode(request, PageMode.LIVE, false);
        }

        if (useNavigateMode(request, loginMode)) {
            return PageMode.setPageMode(request, PageMode.NAVIGATE_EDIT_MODE,false);
        }

        return PageMode.setPageMode(request, PageMode.LIVE, false);
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
    @CloseDB
    protected final void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        Logger.debug(this, "======Starting VelocityServlet_service=====");
        final VelocityRequestWrapper request =VelocityRequestWrapper.wrapVelocityRequest(req);
        final String uri = CMSUrlUtil.getCurrentURI(request);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

        Logger.debug(this, "VelocityServlet_service Uri: " + uri);

        final User user = (userApi.getLoggedInUser(request)!=null)
                ? userApi.getLoggedInUser(request)
                : userApi.getLoggedInFrontendUser(request) !=null
                ? userApi.getLoggedInFrontendUser(request)
                : userApi.getAnonymousUserNoThrow();

        Logger.debug(this, "VelocityServlet_service User " + user.toString());

        request.setRequestUri(uri);
        final PageMode mode = processPageMode(user, request);
        Logger.debug(this, "VelocityServlet_service Pagemode: " + mode.toString());

        // if you are hitting the servlet without running through the other filters
        if (uri == null) {
            Logger.debug(this, "VelocityServlet_service uri is null");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "VelocityServlet called without running through the CMS Filter");
            Logger.error(this.getClass(),
                    "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  "
                            + Constants.CMS_FILTER_URI_OVERRIDE);
            return;
        }

        
        
        // try to get the page
        try {
            final PageContext pageContextBuild = PageContextBuilder.builder()
                    .setPageUri(uri)
                    .setUser(user)
                    .setPageMode(mode)
                    .build();
            Logger.debug(this, "VelocityServlet_service PageContext: " + pageContextBuild.toString());
            final String pageHtml = htmlPageAssetRenderedAPI.getPageHtml(
                    pageContextBuild,
                    request,
                    response
            );

            Logger.debug(this, "VelocityServlet_service pageHtml: " + pageHtml);
            APILocator.getRequestCostAPI().addCostHeader(request, response);
            response.getOutputStream().write(pageHtml.getBytes());
        } catch (ResourceNotFoundException rnfe) {
            Logger.warnAndDebug(this.getClass(), "ResourceNotFoundException" + rnfe.toString(), rnfe);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (DotSecurityException dse) {
            Logger.warnAndDebug(this.getClass(), dse.getMessage(),dse);
            if(!response.isCommitted()) {
                if(user==null || APILocator.getUserAPI().getAnonymousUserNoThrow().equals(user)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
        } catch (HTMLPageAssetNotFoundException hpnfe) {
            Logger.warnAndDebug(this.getClass(), hpnfe.getMessage(),hpnfe);
            if(!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
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
