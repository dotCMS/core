package com.dotcms.rendering.velocity.servlet;

import com.dotcms.business.CloseDB;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotmarketing.business.APILocator;
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
import org.apache.velocity.exception.ResourceNotFoundException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class VelocityServlet extends HttpServlet {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    @Override
    @CloseDB
    protected final void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        VelocityRequestWrapper request = new VelocityRequestWrapper(req);
        final String uri = CMSUrlUtil.getCurrentURI(request);

        if (uri == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "VelocityServlet called without running through the CMS Filter");
            Logger.error(this.getClass(),
                    "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  "
                            + Constants.CMS_FILTER_URI_OVERRIDE);
            return;
        }

        final boolean comeFromSomeWhere = request.getHeader("referer") != null;

        if (APILocator.getLoginServiceAPI().isLoggedIn(request) && !comeFromSomeWhere){
            goToEditPage(uri,request, response);
        } else {

            if ((DbConnectionFactory.isMsSql() && LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) ||
                    (DbConnectionFactory.isOracle() && LicenseUtil.getLevel() < LicenseLevel.PRIME.level) ||
                    (!LicenseUtil.isASAllowed())) {
                Logger.error(this, "Enterprise License is required");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            request.setRequestUri(uri);
            final PageMode mode = PageMode.getWithNavigateMode(request);
            try {
                final String pageHtml = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                        PageContextBuilder.builder()
                                .setPageUri(uri)
                                .setPageMode(mode)
                                .setUser(WebAPILocator.getUserWebAPI().getLoggedInUser(request))
                                .setPageMode(mode)
                                .build(),
                        request,
                        response
                );
                response.getOutputStream().write(pageHtml.getBytes());
            } catch (ResourceNotFoundException rnfe) {
                Logger.error(this, "ResourceNotFoundException" + rnfe.toString(), rnfe);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (DotSecurityException dse) {
                Logger.warnAndDebug(this.getClass(), dse.getMessage(),dse);
                if(!response.isCommitted()) {
                    User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
                    if(user==null || APILocator.getUserAPI().getAnonymousUserNoThrow().equals(user)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                }
            } catch (HTMLPageAssetNotFoundException hpnfe) {
                if(!response.isCommitted()) {
                    Logger.warnAndDebug(this.getClass(), hpnfe.getMessage(),hpnfe);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            } catch (java.lang.IllegalStateException state) {
                Logger.debug(this, "IllegalStateException" + state.toString());
                // Eat this, client disconnect noise
            } catch (Exception e) {
                if(!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Exception Error on template");
                }
                Logger.warnAndDebug(this.getClass(), e.getMessage(),e);
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
