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
        Logger.debug(this, "======Starting SpeedyAssetServlet_service=====");

        /*
		 * Getting host object form the session
		 */
        HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
        Host host;
        try {
            Logger.debug(this, "SpeedyAssetServlet_service Getting host object from the request");
            host = hostWebAPI.getCurrentHost(request);
            Logger.debug(this, "SpeedyAssetServlet_service Host object retrieved from the request is: " + host.getIdentifier());
        } catch (Exception e) {
            Logger.error(this, "Unable to retrieve current request host");
            throw new ServletException(e.getMessage(), e);
        }

        // Checking if host is active
        boolean hostlive;
        boolean _adminMode = UtilMethods.isAdminMode(request, response);
        Logger.debug(this, "SpeedyAssetServlet_service Is Admin Mode: " + _adminMode);

        try {
            hostlive = APILocator.getVersionableAPI().hasLiveVersion(host);
            Logger.debug(this, "SpeedyAssetServlet_service host has live version: " + hostlive);
        } catch (Exception e1) {
            Logger.debug(this, "SpeedyAssetServlet_service Exception trying to check if host has live version: " + e1.getMessage());
            UtilMethods.closeDbSilently();
            throw new ServletException(e1);
        }
        if (!_adminMode && !hostlive) {
            try {
                Logger.debug(this, "SpeedyAssetServlet_service Host is not live and is not admin mode, sending service unavailable error");
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
        Logger.debug(this, "SpeedyAssetServlet_service PageMode: " + mode);


        //GIT-4506

        boolean serveWorkingVersion = !mode.showLive;
        Logger.debug(this, "SpeedyAssetServlet_service Serve Working Version: " + serveWorkingVersion);



        Identifier id = resolveIdentifier(request);
        Logger.debug(this, "SpeedyAssetServlet_service Identifier Resolved: " + id);
        if(id==null){
            Logger.debug(this, "SpeedyAssetServlet_service Invalid identifier passed: url = " + request.getRequestURI());
            response.sendError(404);
            return;
        }

        //Language is in request, let's load it. Otherwise use the language in session
        long lang = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        Logger.debug(this, "SpeedyAssetServlet_service Language: " + lang);
        try{
            Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), lang);
            Logger.debug(this.getClass(), "SpeedyAssetServlet_service contentletVersionInfo: " + (cvi.isEmpty() ? "Not Found" : cvi.toString()));
            if(cvi.isEmpty() && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)){
                Logger.debug(this, "SpeedyAssetServlet_service Contentlet-Version-Info is empty, trying to get default language");
                cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());
                Logger.debug(this.getClass(), "SpeedyAssetServlet_service contentletVersionInfo for defaultLang " + (cvi.isEmpty() ? "Not Found" : cvi.toString()));
            }

            if(cvi.isEmpty()) {
                Logger.debug(this.getClass(), "SpeedyAssetServlet_service contentletVersionInfo is empty, throwing exception");
                throw new DotDataException("Can't find Contentlet-Version-Info. Identifier: "
                        + id.getId(), ". Lang: " + APILocator.getLanguageAPI().getDefaultLanguage().getId());
            }

            String conInode = serveWorkingVersion ? cvi.get().getWorkingInode() : cvi.get().getLiveInode();
            Logger.debug(this.getClass(), "SpeedyAssetServlet_service contentletInode to serve: " + conInode);
            String referrer = "/contentAsset/raw-data/" + conInode + "/fileAsset/?byInode=true";
            Logger.debug(this.getClass(), "SpeedyAssetServlet_service dispatched to: " + referrer);
            request.getRequestDispatcher(referrer).forward(request, response);
        }
        catch(Exception e){
            Logger.warn(this, "Exception trying to file: " +e);
        }




    }



    private Identifier resolveIdentifier(HttpServletRequest request) {
        Logger.debug(this, "--SpeedyAssetServlet_resolveIdentifier from Request--");
        Identifier ident = (Identifier) request.getAttribute(Constants.CMS_FILTER_IDENTITY);
        Logger.debug(this, "SpeedyAssetServlet_resolveIdentifier Identifier from attribute: " + (ident == null ? "Not Found" : ident.toString()));
        if(ident==null){
            if(request.getParameter("path")==null) {
                Logger.debug(this, "SpeedyAssetServlet_resolveIdentifier Param 'Path' is null, getting from the URI");
                // Getting the identifier from the path like /dotAsset/{identifier}.{ext} E.G. /dotAsset/1234.js
                StringTokenizer _st = new StringTokenizer(request.getRequestURI(), "/");

                Logger.debug(this, "SpeedyAssetServlet_resolveIdentifier Requesting by url: " + request.getRequestURI());

                String _fileName = null;
                while(_st.hasMoreElements()){
                    _fileName = _st.nextToken();
                }

                Logger.debug(this, "SpeedyAssetServlet_resolveIdentifier Parsed filename: " + _fileName);

                String identifier = UtilMethods.getFileName(_fileName);

                Logger.debug(SpeedyAssetServlet.class, "SpeedyAssetServlet_resolveIdentifier Loading identifier: " + identifier);
                try {
                    ident = APILocator.getIdentifierAPI().find(identifier);
                    Logger.debug(this.getClass(), "SpeedyAssetServlet_resolveIdentifier Identifier: " + (ident == null? "Not Found" : ident.toString()));
                } catch (DotDataException e) {
                    Logger.debug(this.getClass(), e.getMessage());

                }
            }else if( request.getParameter("path")!=null){
                Logger.debug(this, "SpeedyAssetServlet_resolveIdentifier Param 'Path' is not null, path: " + request.getParameter("path"));
                Host host;
                try {
                    host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
                    Logger.debug(this, "SpeedyAssetServlet_service Host object retrieved from the request is: " + host.getIdentifier());
                    ident = APILocator.getIdentifierAPI().find(host, request.getParameter("path"));
                    Logger.debug(this.getClass(), "SpeedyAssetServlet_resolveIdentifier Identifier: " + (ident == null? "Not Found" : ident.toString()));
                } catch (DotDataException | PortalException | SystemException | DotSecurityException e) {
                    Logger.debug(this.getClass(), e.getMessage());
                }
            }
        }
        return ident;
    }


}
