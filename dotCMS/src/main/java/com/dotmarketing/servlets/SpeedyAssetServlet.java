package com.dotmarketing.servlets;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class SpeedyAssetServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static String realPath = null;
	private static String assetPath = "/assets";

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    public void init(ServletConfig config) throws ServletException {
        // Set the asset paths
        try {
            realPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) { }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) { }
    }




    protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

		if (Config.CONTEXT == null) {
			Config.CONTEXT = this.getServletContext();
			Logger.error(this, "Config.CONTEXT is null. RESETTING  Cannot Serve Files without this!!!!!!");
		}

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

		File f;
		boolean PREVIEW_MODE = false;
		boolean EDIT_MODE = false;
		HttpSession session = request.getSession(false);
		boolean serveWorkingVersion = false;
		boolean isLoggedToBackend = false;

		if(session != null) {
			PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null));
			try {
				EDIT_MODE = (((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null)));
				isLoggedToBackend = WebAPILocator.getUserWebAPI().isLoggedToBackend(request);
			} catch (DotRuntimeException e) {
				Logger.error(this, "Error: Unable to determine if there's a logged user.", e);
			} catch (PortalException e) {
				Logger.error(this, "Error: Unable to determine if there's a logged user.", e);
			} catch (SystemException e) {
				Logger.error(this, "Error: Unable to determine if there's a logged user.", e);
			}

		}
		//GIT-4506
		if(isLoggedToBackend){
			if(!EDIT_MODE && !PREVIEW_MODE)// LIVE_MODE
				serveWorkingVersion = false;
			else
				serveWorkingVersion = true;
		}else{
			serveWorkingVersion = false;//Frontend
		}

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

		String uri = "";
		try {
			String relativePath = null;
			Identifier ident = null;
			if(request.getParameter("path") == null) {

				if(request.getAttribute(CMSFilter.CMS_FILTER_IDENTITY)!=null){
						ident = (Identifier) request.getAttribute(CMSFilter.CMS_FILTER_IDENTITY);
				}else{
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
					try{
						ident = APILocator.getIdentifierAPI().find(identifier);
					}catch(Exception ex){
						Logger.debug(SpeedyAssetServlet.class, "Identifier not found going to try as a File Asset", ex);
					}
				}
				
				//Language is in request, let's load it. Otherwise use the language in session
				long lang = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
				
				if(ident != null && ident.getURI() != null && !ident.getURI().equals("")){

					if(serveWorkingVersion){
						uri = WorkingCache.getPathFromCache(ident.getURI(), ident.getHostId(), lang);
						if(!UtilMethods.isSet(realPath)){
							f = new File(FileUtil.getRealPath(assetPath + uri));
						}else{
							f = new File(realPath + uri);
						}
						
						//If URI cannot be found with selected language and property above is true, 
						//let's pull the file with default Language Instead
						if(uri == null && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)){
							uri = WorkingCache.getPathFromCache(ident.getURI(), ident.getHostId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());
							if(!UtilMethods.isSet(realPath)){
								f = new File(FileUtil.getRealPath(assetPath + uri));
								}else{
									f = new File(realPath + uri);
							 	}
							}
						
						if(uri == null || !f.exists() || !f.canRead()) {
							if(uri == null){
								Logger.warn(SpeedyAssetServlet.class, "URI is null");
							}
							if( !f.exists()){
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : " + f.getAbsolutePath());
							}
							if( !f.canRead()){
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read : " + f.getAbsolutePath());
							}
							response.sendError(404);
							return;
						}

					}else {
						try{
							uri = LiveCache.getPathFromCache(ident.getURI(), ident.getHostId(), lang);
						}catch (Exception e) {
							if(isLoggedToBackend){
								response.setHeader( "Pragma", "no-cache" );
								response.setHeader( "Cache-Control", "no-cache" );
								response.setDateHeader( "Expires", 0 );
								response.sendError(404);
								return;
							}
						}
						if(!UtilMethods.isSet(realPath)){
							f = new File(FileUtil.getRealPath(assetPath + uri));
						}else{
							f = new File(realPath + uri);
						}
						
						//If URI cannot be found with selected language and property above is true, 
						//let's pull the file with default Language Instead
						if(StringUtils.isBlank(uri) && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)){
							try{
								uri = LiveCache.getPathFromCache(ident.getURI(), ident.getHostId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());
								}catch (Exception e) {
									if(isLoggedToBackend){
										response.setHeader( "Pragma", "no-cache" );
										response.setHeader( "Cache-Control", "no-cache" );
										response.setDateHeader( "Expires", 0 );
										response.sendError(404);
										return;
										}
									}
							if(!UtilMethods.isSet(realPath)){
								f = new File(FileUtil.getRealPath(assetPath + uri));
								}else{
									f = new File(realPath + uri);
									}
							}

						if(StringUtils.isBlank(uri)) {
						    Logger.warn(SpeedyAssetServlet.class, "URI is null");
						    response.sendError(404);
                            return;
						}

						if(!f.exists() || !f.canRead()) {
							if( !f.exists()){
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : " + f.getAbsolutePath());
							}
							if( !f.canRead()){
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read  : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read : " + f.getAbsolutePath());
							}



							response.sendError(404);
							return;
						}
					}
				} else {
					Logger.warn(this, "Invalid identifier passed: url = " + request.getRequestURI());
					//Invalid identifier number passed
					response.sendError(404);
					return;
				}

			} else{
				relativePath = request.getParameter("path");
				f = new File(APILocator.getFileAPI().getRealAssetsRootPath() + relativePath);
				if(!f.exists() || !f.canRead()) {
					Logger.warn(this, "Invalid path passed: path = " + relativePath + ", file doesn't exists.");
					//Invalid path given
					response.sendError(404);
					return;
				}
			}

			if(f.getPath().endsWith(".groovy") || f.getPath().endsWith(".php") || f.getPath().endsWith(".rb")  
					|| f.getPath().endsWith(".vtl")|| f.getPath().endsWith(".vm")){
				Logger.warn(this, "SpeedyAsset servlet should not serve this types: .groovy, .php, .rb, .vtl and .vm  ");
				return;
				
			}

			String inode = null;

			IFileAsset file = null;
			Identifier identifier = null;
			boolean canRead = false;
			if(UtilMethods.isSet(relativePath) && relativePath.contains("fileAsset")){
				String[] splits = relativePath.split(Pattern.quote(File.separator));
				if(splits.length>0){
					inode = splits[3];
					Contentlet cont =  null;
					try{
						cont = APILocator.getContentletAPI().find(inode, user, true);
						file = APILocator.getFileAssetAPI().fromContentlet(cont);
						identifier = APILocator.getIdentifierAPI().find(cont);
						canRead = true;
					}catch(Exception e){	
						Logger.warn(this, "Unable to find file asset contentlet with inode " + inode, e);
					}
				}
			}else if(UtilMethods.isSet(uri) && uri.contains("fileAsset")){
				String[] splits = uri.split(Pattern.quote(File.separator));
				if(splits.length>0){
					inode = splits[3];
					Contentlet cont =  null;
					try{
						cont = APILocator.getContentletAPI().find(inode, user, true);
						file = APILocator.getFileAssetAPI().fromContentlet(cont);
						identifier = APILocator.getIdentifierAPI().find(cont);
						canRead = true;
					}catch(Exception e){	
						Logger.warn(this, "Unable to find file asset contentlet with inode " + inode, e);
					}
				}
			}else{
			
				inode = UtilMethods.getFileName(f.getName());
				file = APILocator.getFileAPI().find(inode, user, true);
				identifier = APILocator.getIdentifierAPI().find((com.dotmarketing.portlets.files.model.File)file);
				com.dotmarketing.portlets.files.model.File fProxy = new com.dotmarketing.portlets.files.model.File();
	            fProxy.setIdentifier(identifier.getInode());
	            canRead = permissionAPI.doesUserHavePermission(fProxy, PERMISSION_READ, user, true);
			}
			
			
			//Checking permissions
        	/**
        	 * Build a fake proxy file object so we
        	 * can get inheritable permissions on it
        	 * without having to hit cache or db
        	 */
            

            if (!canRead) {
            	if(user == null){
            		request.getSession(true).setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, request.getRequestURI());//DOTCMS-5682
            		//Sending user to unauthorized the might send him to login
            		response.sendError(401, "The requested file is unauthorized");
            	}else{
            		//sending the user to forbidden
            		response.sendError(403, "The requested file is forbidden");
            	}
           		return;
            }
            

            request.getRequestDispatcher("/contentAsset/raw-data/" + inode + "/fileAsset/?byInode=true").forward(request, response);


		} catch (Exception e) {
			Logger.debug(this, "General Error occurred serving asset = " + request.getRequestURI() + (request.getQueryString() != null?"?"+request.getQueryString():""), e);
			//DOTCMS-1981
			//response.sendError(404, "Asset not Found");
		}
	}
}
