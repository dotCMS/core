package com.dotcms.rendering.velocity.servlet;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.exception.ResourceNotFoundException;
import com.dotcms.business.CloseDB;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPIImpl;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

public class VelocityServlet extends HttpServlet {


    
    private UserWebAPIImpl userApi = (UserWebAPIImpl) WebAPILocator.getUserWebAPI();
    
    
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    @Override
    @CloseDB
    protected final void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        final VelocityRequestWrapper request = new VelocityRequestWrapper(req);
        final String uri = CMSUrlUtil.getCurrentURI(request);
        final boolean comeFromSomeWhere = request.getHeader("referer") != null;
        

        
        final User user = (userApi.getLoggedInUser(request)!=null) 
                        ? userApi.getLoggedInUser(request) 
                        : userApi.getLoggedInFrontendUser(request) !=null
                           ? userApi.getLoggedInFrontendUser(request)
                           : userApi.getAnonymousUserNoThrow();
        
        request.setRequestUri(uri);
        final PageMode mode = PageMode.getWithNavigateMode(request);
        
        // if you are hitting the servlet without running through the other filters
        if (uri == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "VelocityServlet called without running through the CMS Filter");
            Logger.error(this.getClass(),
                    "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  "
                            + Constants.CMS_FILTER_URI_OVERRIDE);
            return;
        }
        
        // if you are a backend user, redirect you to the page edit screen
        if (user.hasConsoleAccess() && !comeFromSomeWhere){
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
            final String pageHtml = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
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
        Logger.info(this.getClass(), "Initing VelocityServlet");


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

}
