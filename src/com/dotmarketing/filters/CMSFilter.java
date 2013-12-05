package com.dotmarketing.filters;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.JBossRulesUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.liferay.util.Xss;

public class CMSFilter implements Filter {

	public void destroy() {

    }

    String ASSET_PATH = null;

    String VELOCITY_PAGE_EXTENSION = null;

    String ASSET_REAL_PATH = null;

    String CMS_ANONYMOUS_ROLE = null;

    String folderPathRegEx = ".*\\.[a-zA-Z0-9]{2,9}$";



    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {


        final String dotExtension = "." + VELOCITY_PAGE_EXTENSION;
        final String httpProtocol = "http://";
        final String httpsProtocol = "https://";

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);
        String uri = request.getRequestURI();

        uri = URLDecoder.decode(uri, "UTF-8");

		Company company = PublicCompanyFactory.getDefaultCompany();


        /*
         * Here is a list of directories that we will ignore b/c of legacy code
         * and servlet mappings. This is a mess and should be much cleaner
         */

		if(Xss.URLHasXSS(uri)){
			uri = Xss.strip(uri);
			if(uri.equals("")){
				uri = "/";
			}
			response.sendRedirect(uri);
			return;
		}

		if(!UtilMethods.decodeURL(request.getQueryString()).equals(null)){
			//http://jira.dotmarketing.net/browse/DOTCMS-6141
			if(request.getQueryString() != null && request.getQueryString().contains("\"")){
				response.sendRedirect(uri+"?"+StringEscapeUtils.escapeHtml(StringEscapeUtils.unescapeHtml(request.getQueryString())));
				return;
			}
			String queryString=UtilMethods.decodeURL(request.getQueryString());
			if(Xss.URLHasXSS(queryString)){
				response.sendRedirect(uri);
				return;
			}
			
		}

        if (excludeURI(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // set the preview mode
        boolean ADMIN_MODE = false;
        boolean EDIT_MODE = false;
        boolean PREVIEW_MODE = false;

        LogFactory.getLog(this.getClass()).debug("CMS Filter URI = " + uri);

        PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        if (session != null) {
            // struts crappy messages have to be retrived from session
            if (session.getAttribute(Globals.ERROR_KEY) != null) {
                request.setAttribute(Globals.ERROR_KEY, session.getAttribute(Globals.ERROR_KEY));
                session.removeAttribute(Globals.ERROR_KEY);
            }
            if (session.getAttribute(Globals.MESSAGE_KEY) != null) {
                request.setAttribute(Globals.MESSAGE_KEY, session.getAttribute(Globals.MESSAGE_KEY));
                session.removeAttribute(Globals.MESSAGE_KEY);
            }
            // set the preview mode
            ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
            PREVIEW_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null && ADMIN_MODE);
            EDIT_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null && ADMIN_MODE);

            if (request.getParameter("livePage") != null && request.getParameter("livePage").equals("1")) {
                PREVIEW_MODE = false;
                EDIT_MODE = false;
                session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
                request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
                session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
                request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
                LogFactory.getLog(this.getClass()).debug("CMS FILTER Cleaning PREVIEW_MODE_SESSION LIVE!!!!");

            }

            if (request.getParameter("previewPage") != null && request.getParameter("previewPage").equals("1")) {
                PREVIEW_MODE = false;
                EDIT_MODE = true;
                session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
                request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
                session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
                request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
                LogFactory.getLog(this.getClass()).debug("CMS FILTER Cleaning EDIT_MODE_SESSION PREVIEW!!!!");
            }

            if (request.getParameter("previewPage") != null && request.getParameter("previewPage").equals("2")) {
                PREVIEW_MODE = true;
                EDIT_MODE = false;
                session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, "true");
                request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, "true");
                session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
                request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
                LogFactory.getLog(this.getClass()).debug("CMS FILTER Cleaning PREVIEW_MODE_SESSION PREVIEW!!!!");
            }
        }

        /*
         * Getting host object form the session
         */
        HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
        Host host;
		try {
			host = hostWebAPI.getCurrentHost(request);
		} catch (PortalException e) {
    		Logger.error(this, "Unable to retrieve current request host for URI " + uri);
    		throw new ServletException(e.getMessage(), e);
		} catch (SystemException e) {
    		Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
    		throw new ServletException(e.getMessage(), e);
		} catch (DotDataException e) {
    		Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
    		throw new ServletException(e.getMessage(), e);
		} catch (DotSecurityException e) {
    		Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
    		throw new ServletException(e.getMessage(), e);
		}

        /*
         * If someone is trying to go right to an asset without going through
         * the cms, give them a 404
         */

        if (UtilMethods.isSet(ASSET_PATH) && uri.startsWith(ASSET_PATH)) {
            response.sendError(403, "Forbidden");
            return;
        }

        String pointer = null;


        if(!uri.equals(pointer) && !uri.endsWith("/") && ! RegEX.contains(uri, folderPathRegEx) && uri.indexOf("/dotCMS/") == -1) {
        	Enumeration enm = req.getParameterNames();
        	StringBuffer params = new StringBuffer("");
            for (; enm.hasMoreElements(); ) {
            	String name = (String)enm.nextElement();
            	params.append(name + "=" + req.getParameter(name));
            	if(enm.hasMoreElements())
            		params.append(StringPool.AMPERSAND);
            }
			response.sendRedirect(uri + "/" + (params.length() > 0 ? "?" + params : ""));
			return;
		}



        /* if edit mode */
        if (PREVIEW_MODE || EDIT_MODE) {
			try {				
				if (uri.endsWith("/"))
					uri = uri.substring(0, uri.length() - 1);
				pointer = WorkingCache.getPathFromCache(uri, host);

				if(!UtilMethods.isSet(pointer)){//DOTCMS-7062
					pointer = LiveCache.getPathFromCache(uri, host);
				}

            if (!UtilMethods.isSet(pointer) && (uri.endsWith(dotExtension) || InodeUtils.isSet(APILocator.getFolderAPI().findFolderByPath(uri, host,APILocator.getUserAPI().getSystemUser(),false).getInode()))) {
                String url = uri;
                if (!uri.endsWith(dotExtension)) {
                    url += "index" + dotExtension;
                }
                request.getRequestDispatcher("/html/portlet/ext/htmlpages/page_not_found_404.jsp?url=" + url + "&hostId=" + host.getIdentifier()).forward(
                        req, res);
                return;
            }
            LogFactory.getLog(this.getClass()).debug("CMS preview pointer = " + uri + ":" + pointer);
			} catch (Exception e) {
				Logger.debug(this.getClass(), "Can't find pointer " + uri);
			}
            /* if live mode */
        } else {

			try {
				pointer = LiveCache.getPathFromCache(uri, host);
			} catch (Exception e) {
				Logger.debug(this.getClass(), "Can't find pointer " + uri);
				try {
					if(WebAPILocator.getUserWebAPI().isLoggedToBackend(request)){
						response.setHeader( "Pragma", "no-cache" );
						response.setHeader( "Cache-Control", "no-cache" );
						response.setDateHeader( "Expires", 0 );
						response.sendError(404);
						return;
					}
				} catch (Exception e1) {
					Logger.debug(this.getClass(), "Can't find pointer " + uri);
				}
			}
            // If the cache hits the db the connection needs to be manually
            // closed
            try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(CMSFilter.class, e.getMessage(), e);
			}
            LogFactory.getLog(this.getClass()).debug("CMS live pointer = " + uri + ":" + pointer);

        }

        /*
         * Checking if host is active
         */
        boolean hostlive;
        try {
            hostlive = APILocator.getVersionableAPI().hasLiveVersion(host);
        } catch (Exception e1) {
            throw new ServletException(e1);
        }
        if(!ADMIN_MODE && !hostlive) {
        	//Checking if it has a maintenance virtual link
        	pointer = (String) VirtualLinksCache.getPathFromCache(host.getHostname() + ":/cmsMaintenancePage");
        	if(pointer == null) {
        		try {
					response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, LanguageUtil.get(company.getCompanyId(), company.getLocale(), "server-unavailable-error-message"));
				} catch (LanguageException e) {
					Logger.error(CMSFilter.class, e.getMessage(), e);
					response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				}
        		return;
        	}

        }

        // if absolute link somewhere else
        if (UtilMethods.isSet(pointer) && (pointer.startsWith(httpProtocol) || pointer.startsWith(httpsProtocol))) {
            response.sendRedirect(pointer);
            return;
        }

        // virtual links only after other links
        if (!UtilMethods.isSet(pointer)) {
            if (uri.endsWith("/"))
                uri = uri.substring(0, uri.length() - 1);
            pointer = VirtualLinksCache.getPathFromCache(host.getHostname() + ":" + uri);

            if (!UtilMethods.isSet(pointer)) {
                pointer = VirtualLinksCache.getPathFromCache(uri);
            }

            if (UtilMethods.isSet(pointer)) { // is it a virtual link?
                LogFactory.getLog(this.getClass()).debug("CMS found virtual link pointer = " + uri + ":" + pointer);
                boolean external = false;
                String auxPointer = pointer;
                if(auxPointer.indexOf("http://") != -1 || auxPointer.indexOf("https://") != -1)
                {
                	try {
	                	User systemUser = APILocator.getUserAPI().getSystemUser();

	                	auxPointer = auxPointer.replace("https://","");
	                	auxPointer = auxPointer.replace("http://","");
	                	int startIndex = 0;
	                	int endIndex = auxPointer.indexOf("/");
	                	if(startIndex < endIndex)
	                	{
	                		String localHostName = auxPointer.substring(startIndex,endIndex);
	                		Host localHost = hostWebAPI.findByName(localHostName, systemUser, false);
	                		if(localHost ==null || !InodeUtils.isSet(localHost.getInode())){
	                			external=true;
	                		}
	                	}
	                	else
	                	{
	                		external = true;
	                	}
                	} catch (DotSecurityException e) {
                		Logger.error(this, "Unable to retrieve host were the virtual link " + uri + " is pointing.", e);
                		throw new ServletException(e.getMessage(), e);
                	} catch (DotDataException e) {
                		Logger.error(this, "Unable to retrieve host were the virtual link " + uri + " is pointing.", e);
                		throw new ServletException(e.getMessage(), e);
					}
                }
                if (!external) {
                	String ext = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
                	if (!pointer.contains("." + ext + "?")) {
                		boolean isDotPage = true;
                		if(!pointer.contains("." + ext) && !pointer.endsWith("/")
                				&& pointer.substring(pointer.lastIndexOf("/")).contains(".")){
                			uri = pointer;
                			try {
								pointer = LiveCache.getPathFromCache(uri, host);
							} catch (Exception e) {
								Logger.debug(this.getClass(), "Can't find pointer " + uri);
							}
							isDotPage = false;
                		}

                		if(isDotPage){
                			if (pointer.contains("?") && !pointer.contains("#")) {
                				int index = pointer.indexOf('?');
                				String indexPage = "index." + ext;
                				if ((0 < index) && (pointer.charAt(index-1) != '/'))
                					indexPage = "/" + indexPage;

                				pointer = pointer.substring(0, index) + indexPage + pointer.substring(index);
                			} else {
                				if(pointer.endsWith("/")){
                					pointer = pointer.substring(0, pointer.lastIndexOf("/"));
                				}
                				String endSlash = pointer.substring(pointer.lastIndexOf("/"));
                				if (!pointer.endsWith("." + ext) && !endSlash.contains("#")) {
                					if (!pointer.endsWith("/"))
                						pointer += "/";
                					pointer += "index." + ext;
                				}else if(endSlash.contains("#") && !(pointer.indexOf("http://") != -1 || pointer.indexOf("https://")!=-1)){
                					String reqUrl = request.getRequestURL().toString();
                					pointer = reqUrl.replaceAll(uri.endsWith("/")?uri:uri+"/", pointer);
                				}
                			}
                		}

                	}else if(pointer.contains("#") && !(pointer.indexOf("http://") != -1 || pointer.indexOf("https://")!=-1)){
                		String endSlash = pointer.substring(pointer.lastIndexOf("/"));
                		if(endSlash.contains("#")){
                			String reqUrl = request.getRequestURL().toString();
                			pointer = reqUrl.replaceAll(uri.endsWith("/")?uri:uri+"/", pointer);
                		}

                	}
                }
    			/*
    			 * Apply Rules to pointer
    			 */
				JBossRulesUtils.checkObjectRulesFromXML(request);
            }

        }
        if (UtilMethods.isSet(pointer) && (pointer.startsWith(httpProtocol) || pointer.startsWith(httpsProtocol))) {
            response.sendRedirect(pointer);
            return;
        }

        if (UtilMethods.isSet(pointer)) {

            if (!endInTheVelocityPageExtension(pointer)) {
                // Validate the permission
                User user = null;
                try {
                    if (session != null)
                        user = (com.liferay.portal.model.User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
                } catch (Exception nsue) {
                    Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
                }

                if(user==null) {
                	try {
						user = com.liferay.portal.util.PortalUtil.getUser(request);
					} catch (Exception nsue) {
	                    Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
	                }
                }

                boolean signedIn = false;
                if (user != null) {
                    signedIn = true;
                }

                Identifier ident = null;

                try {
                	ident =APILocator.getIdentifierAPI().find(host,uri);
                	/**
                	 * Build a fake proxy file object so we
                	 * can get inheritable permissions on it
                	 * without having to hit cache or db
                	 */
                	boolean canRead = false;
                	if(ident.getAssetType().equals("contentlet")){
                		long langId;
            			String langIdReq = req.getParameter("language_id");
            			if(!UtilMethods.isSet(langIdReq)){
            				langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            			}else{
            				langId = Long.parseLong(langIdReq);
            			}

                		try{
                			ContentletVersionInfo cinfo = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), langId);
                			Contentlet proxy  = new Contentlet();
                			if(UtilMethods.isSet(cinfo.getLiveInode()))
                				proxy = APILocator.getContentletAPI().find(cinfo.getLiveInode(), user, true);
                			else if(WebAPILocator.getUserWebAPI().isLoggedToBackend(request))
                				proxy = APILocator.getContentletAPI().find(cinfo.getWorkingInode(), user, true);
                			canRead = UtilMethods.isSet(proxy.getInode());
                		}catch(Exception e){
    						Logger.warn(this, "Unable to find file asset contentlet with identifier " + ident.getId(), e);
                		}

                	}else{
                		com.dotmarketing.portlets.files.model.File f = new com.dotmarketing.portlets.files.model.File();
                        (f).setIdentifier(ident.getInode());
                        canRead = permissionAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, true);
    					f = null;
                	}

					if (!canRead) {

					    /***********************************************************
					     * If we need to redirect someone somewhere to login before
					     * seeing a page, we need to edit the /portal/401.jsp page
					     * to sendRedirect the user to the proper login page. We are
					     * not using the REDIRECT_TO_LOGIN variable in the config
					     * any longer.
					     **********************************************************/

					    // this page is protected. not anonymous access
					    if (!signedIn) {
					        // user is not logged in, needs to go to login page.
					        // go to login page

//                      No need for the below LAST_PATH attribute on the front end http://jira.dotmarketing.net/browse/DOTCMS-2675
//                        request.getSession(true).setAttribute(com.liferay.portal.util.WebKeys.LAST_PATH,
//                                new ObjectValuePair(uri, request.getParameterMap()));
					        request.getSession(true).setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, uri);

					        LogFactory.getLog(CMSFilter.class).debug("VELOCITY CHECKING PERMISSION: Page doesn't have anonymous access" + uri);

					        LogFactory.getLog(CMSFilter.class).debug("Unauthorized URI = " + uri);
					        response.sendError(401, "The requested page/file is unauthorized");
					        return;

					    } else {
					        // the user doesn't have permissions to see this
					        // page
					        // go to unauthorized page
					        LogFactory.getLog(CMSFilter.class).warn("VELOCITY CHECKING PERMISSION: Page doesn't have any access for this user");
					        response.sendError(403, "The requested page/file is forbidden");
					        return;
					    }

					}
				} catch (DotDataException e) {
					Logger.error(CMSFilter.class,e.getMessage(),e);
					throw new IOException(e.getMessage());
				}
                String mimeType = APILocator.getFileAPI().getMimeType(Config.CONTEXT.getRealPath(pointer));
                response.setContentType(mimeType);
            }
            LogFactory.getLog(this.getClass()).debug("CMS Filter going to redirect to pointer");

            if(pointer.endsWith(dotExtension)){
            	//Serving a page through the velocity servlet
                request.getRequestDispatcher(pointer).forward(request, response);
            } else {
            	//Serving a regular asset through the speedy asset servlet
                request.getRequestDispatcher("/dotAsset?path=" + pointer).forward(request, response);
            }
            return;

        }

        /*
         * This will allow any file not under CMS to be served (.jsps, mapped
         * dirs, etc...)
         */

        chain.doFilter(request, response);

    }

    public void init(FilterConfig config) throws ServletException {
        VELOCITY_PAGE_EXTENSION = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
        ASSET_PATH = APILocator.getFileAPI().getRelativeAssetsRootPath();
        ASSET_REAL_PATH = Config.getStringProperty("ASSET_REAL_PATH");
        CMS_ANONYMOUS_ROLE = Config.getStringProperty("CMS_ANONYMOUS_ROLE");


    }

    private static Set<String> excludeList=null;
    private static final Integer mutex=new Integer(0);
    private static void buildExcludeList() {
        synchronized(mutex) {
         if(excludeList!=null) return;
         
         Set<String> set=new HashSet<String>();
         
    	 // allow servlets to be called without a 404
         set.add("^/servlet/");
         set.add("^/servlets/");
         //Load some defaults
         set.add("^/portal/");
         set.add("^/icon$");
         set.add("^/dwr/");
         set.add("^/titleServlet$");
         set.add("^/TitleServlet$");
         set.add("^/categoriesServlet$");
         set.add("^/xspf$");
         set.add("^/thumbnail$");
         set.add("^/html/skin/");
         set.add("^/webdav/");
         set.add("^/dotAsset/");
         set.add("^/JSONContent/");
         set.add("^/resize_image$");
         set.add("^/image/company_logo$");
         set.add("^/dotScheduledJobs$");
         set.add("^/dot_slideshow$");
         set.add("^/redirect$");
         set.add("^/imageShim$");
         set.add("^/DotAjaxDirector/");
         set.add("^/cmis/");
         set.add("^/permalink/");
         set.add("^/controlGif$");
         set.add("^/Captcha.jpg$");
         set.add("^/audioCaptcha.wav$");
         // http://jira.dotmarketing.net/browse/DOTCMS-5187
         set.add("^/admin$");
         set.add("^/admin/");
         set.add("^/edit$");
         set.add("^/edit/");
         set.add("^/dotTailLogServlet/");
         //http://jira.dotmarketing.net/browse/DOTCMS-2178
         set.add("^/contentAsset/");
         //http://jira.dotmarketing.net/browse/DOTCMS-6753
         set.add("^/JSONTags/");
         set.add("^/spring/");
         set.add("^/api/");

         //Load exclusions from plugins
         PluginAPI pAPI=APILocator.getPluginAPI();
         List<String> pluginList=pAPI.getDeployedPluginOrder();
         if (pluginList!=null) {
 	        for (String pluginID:pluginList) {
 	        	try {
 					String list=pAPI.loadPluginConfigProperty(pluginID, "cmsfilter.servlet.exclusions");
 					Logger.info(CMSFilter.class,"plugin "+pluginID+" cmsfilter.servlet.exclusions="+list);
 					if (list!=null) {
 						String[] items=list.split(",");
 						if (items!=null && items.length>0) {
 							for (String item:items) {
 								item=item.trim();
 								if (UtilMethods.isSet(item) && !set.contains(item)) {
 										set.add(item);
 								}
 							}
 						}
 					}
 				} catch (DotDataException e) {
 					Logger.debug(CMSFilter.class,"DotDataException: " + e.getMessage(),e);
 				}

 	        }
         }
         excludeList=set;
        }
    }

    public static void addExclude(String URLPattern){
    	if(excludeList== null){
    		buildExcludeList();
    	}
    	synchronized(excludeList){
    		excludeList.add(URLPattern);
    	}
    }

	public static void removeExclude(String URLPattern){
		if(excludeList!= null){
			synchronized(excludeList){
				excludeList.remove(URLPattern);
			}
    	}
    }
    
    public static boolean excludeURI(String uri) {
        if (uri.trim().equals("/c")
                || uri.endsWith(".php")
        		|| uri.trim().startsWith("/c/")
        		|| (uri.indexOf("/ajaxfileupload/upload") != -1)
        		||  new File(Config.CONTEXT.getRealPath(uri)).exists()
        		&& !"/".equals(uri)) {
        	return true;
        }
        
        if(excludeList==null) buildExcludeList();

        if(excludeList.contains(uri)) return true;

        for ( String exclusion : excludeList ) {
            if ( RegEX.contains( uri, exclusion ) ) {
                return true;
            }
        }
        return false;
   }

    private boolean endInTheVelocityPageExtension(String URI) {
        boolean returnValue = false;
        if (URI.indexOf("?") != -1) {
            URI = URI.substring(0, URI.indexOf("?"));
        }
        returnValue = (URI.endsWith(VELOCITY_PAGE_EXTENSION) ? true : false);
        return returnValue;
    }
}
