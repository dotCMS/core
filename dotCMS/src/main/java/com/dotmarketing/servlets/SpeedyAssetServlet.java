package com.dotmarketing.servlets;

import com.dotmarketing.filters.Constants;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

public class SpeedyAssetServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

    public void init(ServletConfig config) throws ServletException {
      if (Config.CONTEXT == null) {
        Config.CONTEXT = this.getServletContext();
        Logger.error(this, "Config.CONTEXT is null. RESETTING  Cannot Serve Files without this!!!!!!");
        return;
    }
    }




    protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {


        /*
		 * Getting host object form the session
		 */
        HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
        Host host;
        try {
            host = hostWebAPI.getCurrentHost(request);
        } catch (Exception e) {
            Logger.error(this, "Unable to retrieve current request host");
            throw new ServletException(e.getMessage(), e);
        }

        // Checking if host is active
        boolean hostlive;
        boolean _adminMode = UtilMethods.isAdminMode(request, response);

        try {
            hostlive = APILocator.getVersionableAPI().hasLiveVersion(host);
        } catch (Exception e1) {
            UtilMethods.closeDbSilently();
            throw new ServletException(e1);
        }
        if (!_adminMode && !hostlive) {
            try {
                Company company = PublicCompanyFactory.getDefaultCompany();
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        LanguageUtil.get(company.getCompanyId(), company.getLocale(), "server-unavailable-error-message"));
            } catch (LanguageException e) {
                Logger.error(CMSFilter.class, e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            return;
        }


        PageMode mode = PageMode.get(request);
		HttpSession session = request.getSession(false);


		//GIT-4506

		boolean serveWorkingVersion = !mode.showLive;

        User user = null;
        try {
            if (session != null)
                user = (com.liferay.portal.model.User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
        	if(user==null){
				user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			}
        } catch (Exception nsue) {
            Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
        }

		

		Identifier id = resolveIdentifier(request); 
		if(id==null){
		  Logger.debug(this, "Invalid identifier passed: url = " + request.getRequestURI());
          response.sendError(404);
          return;
        }

				//Language is in request, let's load it. Otherwise use the language in session
		long lang = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
		try{
  		  Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), lang);

  		  if(!cvi.isPresent() && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)){
  		      cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());
  		  }

  		  if(!cvi.isPresent()) {
  		      throw new DotDataException("Can't find Contentlet-Version-Info. Identifier: "
                      + id.getId(), ". Lang: " + APILocator.getLanguageAPI().getDefaultLanguage().getId());
          }
  
  		  String conInode = serveWorkingVersion ? cvi.get().getWorkingInode() : cvi.get().getLiveInode();
          String referrer = "/contentAsset/raw-data/" + conInode + "/fileAsset/?byInode=true";
          request.getRequestDispatcher(referrer).forward(request, response);
		}
        catch(Exception e){
          Logger.warn(this, "Exception trying to file: " +e);
        }




	}



    private Identifier resolveIdentifier(HttpServletRequest request) {
      Identifier ident = (Identifier) request.getAttribute(Constants.CMS_FILTER_IDENTITY);
      if(ident==null){
        if(request.getParameter("path")==null) {
          // Getting the identifier from the path like /dotAsset/{identifier}.{ext} E.G. /dotAsset/1234.js
          StringTokenizer _st = new StringTokenizer(request.getRequestURI(), "/");

          Logger.debug(this, "Requesting by url: " + request.getRequestURI());

          String _fileName = null;
          while(_st.hasMoreElements()){
              _fileName = _st.nextToken();
          }

          Logger.debug(this, "Parsed filename: " + _fileName);

          String identifier = UtilMethods.getFileName(_fileName);

          Logger.debug(SpeedyAssetServlet.class, "Loading identifier: " + identifier);
          try {
            ident = APILocator.getIdentifierAPI().find(identifier);
          } catch (DotDataException e) {
            Logger.debug(this.getClass(), e.getMessage());

          }
        }else if( request.getParameter("path")!=null){
          Host host;
          try {
            host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            ident = APILocator.getIdentifierAPI().find(host, request.getParameter("path"));
          } catch (DotDataException | PortalException | SystemException | DotSecurityException e) {
            Logger.debug(this.getClass(), e.getMessage());
          }
        }
      }
      return ident;
    }


}
