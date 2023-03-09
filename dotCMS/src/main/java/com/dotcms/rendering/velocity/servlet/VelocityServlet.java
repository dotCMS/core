package com.dotcms.rendering.velocity.servlet;

import static com.dotmarketing.util.WebKeys.LOGIN_MODE_PARAMETER;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDB;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
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
import javax.servlet.http.HttpSession;
import org.apache.velocity.exception.ResourceNotFoundException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    @VisibleForTesting
    public static PageMode processPageMode (final User user, final HttpServletRequest request) {

        final LoginMode loginMode = LoginMode.get(request);

        if (LoginMode.UNKNOWN == loginMode) {

            return user.isFrontendUser()
                    ? PageMode.setPageMode(request, PageMode.LIVE)
                    :  user.isBackendUser()
                    ? PageMode.getWithNavigateMode(request)
                    :  PageMode.setPageMode(request, PageMode.LIVE);
        }

        return  LoginMode.FE == loginMode?
                PageMode.setPageMode(request, PageMode.LIVE): PageMode.getWithNavigateMode(request);
    }

    @Override
    @CloseDB
    protected final void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        final VelocityRequestWrapper request =VelocityRequestWrapper.wrapVelocityRequest(req);
        final String uri = CMSUrlUtil.getCurrentURI(request);
        final boolean comeFromSomeWhere = request.getHeader("referer") != null;
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final User user = (userApi.getLoggedInUser(request)!=null)
                        ? userApi.getLoggedInUser(request) 
                        : userApi.getLoggedInFrontendUser(request) !=null
                           ? userApi.getLoggedInFrontendUser(request)
                           : userApi.getAnonymousUserNoThrow();
        
        request.setRequestUri(uri);
        final PageMode mode = processPageMode(user, request);

        // if you are hitting the servlet without running through the other filters
        if (uri == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "VelocityServlet called without running through the CMS Filter");
            Logger.error(this.getClass(),
                    "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  "
                            + Constants.CMS_FILTER_URI_OVERRIDE);
            return;
        }
        
        // if you are a backend user, redirect you to the page edit screen
        if (user!=null && user.hasConsoleAccess() && !isFrontendLogin(request) && !comeFromSomeWhere){
            goToEditPage(uri,request, response);
            return;
        } 
        
        // if you are not running ee
        if ((DbConnectionFactory.isMsSql() && LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
                        || (DbConnectionFactory.isOracle() && LicenseUtil.getLevel() < LicenseLevel.PRIME.level)
                        || (!LicenseUtil.isASAllowed())) {
            Logger.error(this, "Enterprise License is required");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        
        // try to get the page
        try {
            final String pageHtml = htmlPageAssetRenderedAPI.getPageHtml(
                    PageContextBuilder.builder()
                            .setPageUri(uri)
                            .setPageMode(mode)
                            .setUser(user)
                            .setPageMode(mode)
                            .build(),
                    request,
                    response
            );
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
