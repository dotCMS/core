package com.dotmarketing.velocity;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.io.File;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.*;
import java.util.Calendar;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.*;
import com.liferay.portal.language.LanguageException;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.view.context.ChainedContext;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BlockPageCache;
import com.dotmarketing.business.BlockPageCache.PageCacheParameters;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.viewtools.DotTemplateTool;
import com.dotmarketing.viewtools.RequestWrapper;
import com.dotmarketing.viewtools.content.ContentMap;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;

public abstract class VelocityServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private ContentletAPI conAPI = APILocator.getContentletAPI();

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	private static PortletAPI portletAPI = APILocator.getPortletAPI();

	private static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();

	private static VisitorAPI visitorAPI = APILocator.getVisitorAPI();

	public static final ThreadLocal<Context> velocityCtx = new ThreadLocal<Context>();

	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	private String CHARSET = null;

	private String VELOCITY_HTMLPAGE_EXTENSION = "dotpage";
	
	public static final String VELOCITY_CONTEXT = "velocityContext";

	protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

		final String uri =URLDecoder.decode(
				(req.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE)!=null) 
					? (String) req.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE) 
							: req.getRequestURI()
				, "UTF-8");
	
		

        RequestWrapper request  = new RequestWrapper( req );
        request.setRequestUri(uri);

        /*
		 * Getting host object form the session
		 */
        Host host;
        try {
            host = hostWebAPI.getCurrentHost(request);
        } catch (Exception e) {
            Logger.error(this, "Unable to retrieve current request host for URI " + uri);
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

		
		if (DbConnectionFactory.isMsSql() && LicenseUtil.getLevel() < 299) {
			request.getRequestDispatcher("/portal/no_license.jsp").forward(request, response);
			return;
		}

		if (DbConnectionFactory.isOracle() && LicenseUtil.getLevel() < 399) {
			request.getRequestDispatcher("/portal/no_license.jsp").forward(request, response);
			return;
		}
		if (!LicenseUtil.isASAllowed()) {
			request.getRequestDispatcher("/portal/no_license.jsp").forward(request, response);
			return;

		}
		Long profileTime = null;
		if (Config.getBooleanProperty("VELOCITY_PROFILING", false)) {
			profileTime = Calendar.getInstance().getTimeInMillis();
		}
		try {


			if(uri==null){
				response.sendError(500, "VelocityServlet called without running through the CMS Filter");
				Logger.error(this.getClass(), "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  " + CMSFilter.CMS_FILTER_URI_OVERRIDE);
				return;
			}




			HttpSession session = request.getSession(false);
			boolean ADMIN_MODE=false;
			boolean PREVIEW_MODE=false;
			boolean EDIT_MODE=false;
			if(session!=null){
				ADMIN_MODE = PageRequestModeUtil.isAdminMode(session);
				PREVIEW_MODE = PageRequestModeUtil.isPreviewMode(session);
				EDIT_MODE = PageRequestModeUtil.isEditMode(session);
			}

			String value = request.getHeader("X-Requested-With");
			if ((value != null) && value.equals("XMLHttpRequest") && EDIT_MODE && ADMIN_MODE) {
				ADMIN_MODE = false;
			}

			// ### VALIDATE ARCHIVE ###
			if ((EDIT_MODE || PREVIEW_MODE) && isArchive(request, response, uri)) {
				PREVIEW_MODE = true;
				EDIT_MODE = false;
				request.setAttribute("archive", true);
			}
			// ### END VALIDATE ARCHIVE ###

			LanguageWebAPI langWebAPI = WebAPILocator.getLanguageWebAPI();
			langWebAPI.checkSessionLocale(request);

			// we will always need a visitor in admin mode
			if(ADMIN_MODE){
				visitorAPI.getVisitor(request,true);
			}
			
			
			if (PREVIEW_MODE && ADMIN_MODE) {
				// preview mode has the left hand menu and edit buttons on the
				// working page

				Logger.debug(VelocityServlet.class, "VELOCITY SERVLET I'M ON PREVIEW MODE!!!");

				doPreviewMode(request, response);
			} else if (EDIT_MODE && ADMIN_MODE) {
				// edit mode has the left hand menu and edit buttons on the
				// working page

				Logger.debug(VelocityServlet.class, "VELOCITY SERVLET I'M ON EDIT MODE!!!");

				doEditMode(request, response);
			} else if (ADMIN_MODE) {
				// admin mode has the left hand menu and shows the live page in
				// the frame
				Logger.debug(VelocityServlet.class, "VELOCITY SERVLET I'M ON ADMIN MODE!!!");

				doAdminMode(request, response);
			} else {
				// live mode has no frame and shows the live page
				Logger.debug(VelocityServlet.class, "VELOCITY SERVLET I'M ON LIVE MODE!!!");

				doLiveMode(request, response);
			}

		} catch (ResourceNotFoundException rnfe) {

			response.sendError(404);
			return;
			//request.setAttribute(Constants.SERVE_URL, request.getRequestURI());
			//request.getRequestDispatcher("/localResourceServlet").forward(request, response);
			
		} catch (ParseErrorException pee) {
			Logger.error(this, "Template Parse Exception : " + pee.toString(), pee);
			try {
				response.sendError(500, "Template Parse Exception");
			} catch (Throwable t) {
				Logger.error(this, t.getMessage(), t);
				PrintWriter out = response.getWriter();
				out.println("Template Parse Exception");
				out.println("On template:" + request.getRequestURI() + request.getQueryString());
			}

		} catch (MethodInvocationException mie) {
			Logger.error(this, "MethodInvocationException" + mie.toString(), mie);
			try {
				response.sendError(500, "MethodInvocationException Error on template");
			} catch (Throwable t) {
				Logger.error(this, t.getMessage(), t);
				PrintWriter out = response.getWriter();
				out.println("MethodInvocationException Error on template:" + request.getRequestURI() + request.getQueryString());
			}
		} catch (Exception e) {
			Logger.error(this, e.toString(), e);
			try {
				response.sendError(500, "MethodInvocationException Error on template");
			} catch (Throwable t) {
				Logger.error(this, t.getMessage(), t);
				PrintWriter out = response.getWriter();
				out.println("Error on template:" + request.getRequestURI() + request.getQueryString());
			}
		} finally {
			// catchall
			// added finally because of
			// http://jira.dotmarketing.net/browse/DOTCMS-1334
			try {
				HibernateUtil.commitTransaction();
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
			DbConnectionFactory.closeConnection();
			velocityCtx.remove();
		}
		if (profileTime != null) {
			profileTime = Calendar.getInstance().getTimeInMillis() - profileTime;
			VelocityProfiler.log(VelocityServlet.class, "VelocityPage time: " + request.getRequestURL() + " " + profileTime + " millis");
		}
	}

	public void init(ServletConfig config) throws ServletException {


		
		// build the dirs
		String pathWorking = Config.CONTEXT_PATH + File.separator + "WEB-INF" + File.separator + "velocity" + File.separator + "working";
		String pathLive = Config.CONTEXT_PATH + File.separator + "WEB-INF" + File.separator + "velocity" + File.separator + "live";
		
		new File(pathWorking).mkdirs();
		new File(pathLive).mkdir();


		Config.initializeConfig();
		CHARSET = Config.getStringProperty("CHARSET");
		VELOCITY_HTMLPAGE_EXTENSION = Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION");

	}

	protected void doAdminMode(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// LIVE MODE - LIVE PAGE



		com.liferay.portal.model.User backendUser = null;
		backendUser = com.liferay.portal.util.PortalUtil.getUser(request);

		response.setContentType(CHARSET);
		Context context = VelocityUtil.getWebContext(request, response);

		String uri = URLDecoder.decode(request.getRequestURI(), UtilMethods.getCharsetConfiguration());


		Host host = hostWebAPI.getCurrentHost(request);

		Identifier id = APILocator.getIdentifierAPI().find(host, uri);
		request.setAttribute("idInode", id.getInode());
		
		IHTMLPage htmlPage = VelocityUtil.getPage(id, request, false, context);
		String languageStr = "";
		if ( htmlPage.isContent() ) {
			languageStr = "_" + VelocityUtil.getLanguageId(request);
		}
		VelocityUtil.makeBackendContext(context, htmlPage, "", id.getURI(), request, true, false, false, host);

		boolean canUserWriteOnTemplate = permissionAPI.doesUserHavePermission(
		        APILocator.getHTMLPageAssetAPI().getTemplate(htmlPage, true),
				PERMISSION_WRITE, backendUser);
		context.put("EDIT_TEMPLATE_PERMISSION", canUserWriteOnTemplate);

		Template template = null;

		if (request.getParameter("leftMenu") != null) {
			template = VelocityUtil.getEngine().getTemplate("/preview_left_menu.vl");
		} else if (request.getParameter("mainFrame") != null) {
			template = VelocityUtil.getEngine().getTemplate("/live/" + id.getInode() + languageStr + "." + VELOCITY_HTMLPAGE_EXTENSION);
		} else {
			template = VelocityUtil.getEngine().getTemplate("/preview_mode.vl");
		}

		Logger.debug(VelocityServlet.class, "Got the template!!!!" + id.getInode());

		PrintWriter out = response.getWriter();
		request.setAttribute(VELOCITY_CONTEXT, context);
		try {

			template.merge(context, out);

		} catch (ParseErrorException e) {
			out.append(e.getMessage());
		}
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void doLiveMode(HttpServletRequest request, HttpServletResponse response) throws Exception {
	    LicenseUtil.startLiveMode();
	    try {
			String uri = URLDecoder.decode(request.getRequestURI(), UtilMethods.getCharsetConfiguration());
    		Host host;
            try {
                host = hostWebAPI.getCurrentHost(request);
            } catch (Exception e) {
                Logger.error(this, "Unable to retrieve current request host for URI " + uri);
                throw new ServletException(e.getMessage(), e);
            }

			//Find the current language
			long currentLanguageId = VelocityUtil.getLanguageId(request);
			Long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

			// Map with all identifier inodes for a given uri.
    		//
    		// Checking the path is really live using the livecache
			String cachedUri = null;
			try {
				/*
				First search using the current language.
				WE COULD HAVE A PAGE THAT DOES NOT EXIST IN THE DEFAULT LANGUAGE, ALWAYS USE THE LANGUAGE ID
				 */
				cachedUri = LiveCache.getPathFromCache( uri, host, currentLanguageId );
			} catch ( DotContentletStateException e ) {

				//Nothing found with the given language...

				if ( currentLanguageId != defaultLanguageId ) {
					//Now trying with the default language
					try {
						cachedUri = LiveCache.getPathFromCache( uri, host, defaultLanguageId );
					} catch ( DotContentletStateException e1 ) {
						//Ok, we found nothing....
					}
				}
			}
			// if we still have nothing, check live cache first (which has a 404 cache )
    		if (cachedUri == null) {
    			throw new ResourceNotFoundException(String.format("Resource %s not found in Live mode!", uri));
    		}
    
    		// now  we check identifier cache first (which DOES NOT have a 404 cache )
    		Identifier ident = APILocator.getIdentifierAPI().find(host, uri);
    		if(ident == null || ident.getInode() == null){
    			throw new ResourceNotFoundException(String.format("Resource %s not found in Live mode!", uri));
    		}
    		response.setContentType(CHARSET);
    		request.setAttribute("idInode", String.valueOf(ident.getInode()));
    		Logger.debug(VelocityServlet.class, "VELOCITY HTML INODE=" + ident.getInode());

			Optional<Visitor> visitor = visitorAPI.getVisitor(request);

			boolean newVisitor = false;
			boolean newVisit = false;

    		/*
    		 * JIRA http://jira.dotmarketing.net/browse/DOTCMS-4659
    		//Set long lived cookie regardless of who this is */
    		String _dotCMSID = UtilMethods.getCookieValue(request.getCookies(),
    				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

    		if (!UtilMethods.isSet(_dotCMSID)) {
    			// create unique generator engine
    			Cookie idCookie = CookieUtil.createCookie();
				_dotCMSID = idCookie.getValue();
    			response.addCookie(idCookie);
				newVisitor = true;

				if(visitor.isPresent()) {
					visitor.get().setDmid(UUID.fromString(_dotCMSID));
				}

    		}

            String _oncePerVisitCookie = UtilMethods.getCookieValue(request.getCookies(),
                    WebKeys.ONCE_PER_VISIT_COOKIE);

            if (!UtilMethods.isSet(_oncePerVisitCookie)) {
				newVisit = true;
            }

			if(newVisitor) {
				RulesEngine.fireRules(request, response, Rule.FireOn.ONCE_PER_VISITOR);
				if(response.isCommitted()) {
                /* Some form of redirect, error, or the request has already been fulfilled in some fashion by one or more of the actionlets. */
					Logger.debug(VelocityServlet.class, "A ONCE_PER_VISITOR RuleEngine Action has committed the response.");
					return;
				}
			}

			if(newVisit) {
   				RulesEngine.fireRules(request, response, Rule.FireOn.ONCE_PER_VISIT);
				if(response.isCommitted()) {
                /* Some form of redirect, error, or the request has already been fulfilled in some fashion by one or more of the actionlets. */
					Logger.debug(VelocityServlet.class, "A ONCE_PER_VISIT RuleEngine Action has committed the response.");
					return;
				}
			}

			RulesEngine.fireRules(request, response, Rule.FireOn.EVERY_PAGE);

			if(response.isCommitted()) {
                /* Some form of redirect, error, or the request has already been fulfilled in some fashion by one or more of the actionlets. */
				Logger.debug(VelocityServlet.class, "An EVERY_PAGE RuleEngine Action has committed the response.");
				return;
			}

    
    		com.liferay.portal.model.User user = null;
    		HttpSession session = request.getSession(false);
    		try {
    			if(session!=null)
    				user = (com.liferay.portal.model.User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
    		} catch (Exception nsue) {
    			Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
    		}
    
    		boolean signedIn = false;
    		if (user != null) {
    			signedIn = true;
    		}
    		Logger.debug(VelocityServlet.class, "Page Permissions for URI=" + uri);

    
    		IHTMLPage page;

            try {
                page = VelocityUtil.getPage(ident, request, true, null);
            } catch(DotDataException e) {
                Logger.info(VelocityServlet.class, "Unable to find live version of page. Identifier: " + ident.getId());
    			throw new ResourceNotFoundException(String.format("Resource %s not found in Live mode!", uri));
            }

    		// Check if the page is visible by a CMS Anonymous role
    		if (!permissionAPI.doesUserHavePermission(page, PERMISSION_READ, user, true)) {
    			// this page is protected. not anonymous access

    			/*******************************************************************
    			 * If we need to redirect someone somewhere to login before seeing a
    			 * page, we need to edit the /portal/401.jsp page to sendRedirect
    			 * the user to the proper login page. We are not using the
    			 * REDIRECT_TO_LOGIN variable in the config any longer.
    			 ******************************************************************/
    			if (!signedIn) {
    				// No need for the below LAST_PATH attribute on the front end
    				// http://jira.dotmarketing.net/browse/DOTCMS-2675
    				// request.getSession().setAttribute(WebKeys.LAST_PATH,
    				// new ObjectValuePair(uri, request.getParameterMap()));
    				request.getSession().setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, uri);
    				Logger.debug(VelocityServlet.class, "VELOCITY CHECKING PERMISSION: Page doesn't have anonymous access" + uri);
    				Logger.debug(VelocityServlet.class, "401 URI = " + uri);
    				Logger.debug(VelocityServlet.class, "Unauthorized URI = " + uri);
    				response.sendError(401, "The requested page/file is unauthorized");
    				return;
    			} else if (!permissionAPI.getReadRoles(ident).contains(APILocator.getRoleAPI().loadLoggedinSiteRole())) {
    				// user is logged in need to check user permissions
    				Logger.debug(VelocityServlet.class, "VELOCITY CHECKING PERMISSION: User signed in");

    				// check user permissions on this asset
    				if (!permissionAPI.doesUserHavePermission(ident, PERMISSION_READ, user, true)) {
    					// the user doesn't have permissions to see this page
    					// go to unauthorized page
    					Logger.warn(VelocityServlet.class, "VELOCITY CHECKING PERMISSION: Page doesn't have any access for this user");
    					response.sendError(403, "The requested page/file is forbidden");
    					return;
    				}
    			}
    		}

			//Fire the page rules until we know we have permission.
			RulesEngine.fireRules(request, response, page, Rule.FireOn.EVERY_PAGE);

    		Logger.debug(VelocityServlet.class, "Recording the ClickStream");
    		if(Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
    			if (user != null) {
    				UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,
    						APILocator.getUserAPI().getSystemUser(), false);
    				if (!userProxy.isNoclicktracking()) {
    					ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response), host);
    				}
    			} else {
    				ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response), host);
    			}
    		}

    		// Begin page caching
    		String userId = (user != null) ? user.getUserId() : "PUBLIC";
    		String language = String.valueOf(currentLanguageId);
    		String urlMap = (String) request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);
    		String queryString = request.getQueryString();
    		String persona = null;
    		Optional<Visitor> v = APILocator.getVisitorAPI().getVisitor(request, false);
    		if(v.isPresent() && v.get().getPersona() !=null){
    			persona=v.get().getPersona().getKeyTag();
    		}

    				
			PageCacheParameters cacheParameters = new BlockPageCache.PageCacheParameters(userId, language, urlMap, queryString, persona);

    		boolean buildCache = false;
    		String key = VelocityUtil.getPageCacheKey(request, response);
    		if (key != null) {
    			String cachedPage = CacheLocator.getBlockPageCache().get(page, cacheParameters);
    			if (cachedPage == null || "refresh".equals(request.getParameter("dotcache"))
    					|| "refresh".equals(request.getAttribute("dotcache"))
    					|| (request.getSession(false) !=null && "refresh".equals(request.getSession(true).getAttribute("dotcache")))) {
    				// build cached response
    				buildCache = true;
    			} else {
    				// have cached response and are not refreshing, send it
    				response.getWriter().write(cachedPage);
    				return;
    			}
    		}
    
    		Writer out = (buildCache) ? new StringWriter(4096) : new VelocityFilterWriter(response.getWriter());
    		//get the context from the requst if possible
    		Context context = VelocityUtil.getWebContext(request, response);
    		request.setAttribute("velocityContext", context);
    		Logger.debug(VelocityServlet.class, "HTMLPage Identifier:" + ident.getInode());

    		try {

				if ( page.isContent() ) {
					VelocityUtil.getEngine().getTemplate("/live/" + ident.getInode() + "_" + page.getLanguageId()
							+ "." + VELOCITY_HTMLPAGE_EXTENSION).merge(context, out);
				} else {
					VelocityUtil.getEngine().getTemplate("/live/" + ident.getInode()
							+ "." + VELOCITY_HTMLPAGE_EXTENSION).merge(context, out);
				}

			} catch (Throwable e) {
    			Logger.warn(this, "can't do live mode merge", e);
    		}
    		session = request.getSession(false);
    		if (buildCache) {
    			String trimmedPage = out.toString().trim();
    			response.getWriter().write(trimmedPage);
    			response.getWriter().close();
    			synchronized (key.intern()) {
    				//CacheLocator.getHTMLPageCache().remove(page);
    				CacheLocator.getBlockPageCache().add(page, trimmedPage, cacheParameters);
    			}
    		} else {
    			out.close();
    		}
	    }
	    finally {
	        LicenseUtil.stopLiveMode();
	    }

	}

	@SuppressWarnings("unchecked")
	public void doPreviewMode(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String uri = URLDecoder.decode(request.getRequestURI(), UtilMethods.getCharsetConfiguration());


		Host host = hostWebAPI.getCurrentHost(request);

		StringBuilder preExecuteCode = new StringBuilder();
		Boolean widgetPreExecute = false;

		// Getting the user to check the permissions
		com.liferay.portal.model.User user = null;

		try {
				user = com.liferay.portal.util.PortalUtil.getUser( request );
		} catch (Exception nsue) {
			Logger.warn(this, "Exception trying getUser: " + nsue.getMessage(), nsue);
		}

		// Getting the identifier from the uri
		Identifier id = APILocator.getIdentifierAPI().find(host, uri);
		request.setAttribute("idInode", id.getInode());
		Logger.debug(VelocityServlet.class, "VELOCITY HTML INODE=" + id.getInode());

		Template template = null;
		Template hostVariablesTemplate = null;

		// creates the context where to place the variables
		response.setContentType(CHARSET);
		Context context = VelocityUtil.getWebContext(request, response);

		IHTMLPage htmlPage = VelocityUtil.getPage(id, request, false, context);
		if("contentlet".equals(htmlPage.getType())){
			context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user,true,host, context) );
		}
		HTMLPageAPI htmlPageAPI = APILocator.getHTMLPageAPI();
		PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
		List<PublishingEndPoint> receivingEndpoints = pepAPI.getReceivingEndPoints();
		// to check user has permission to write on this page
        boolean hasWritePermOverHTMLPage = permissionAPI.doesUserHavePermission( htmlPage, PERMISSION_WRITE, user );
        boolean hasPublishPermOverHTMLPage = permissionAPI.doesUserHavePermission( htmlPage, PERMISSION_PUBLISH, user );
        boolean hasRemotePublishPermOverHTMLPage = hasPublishPermOverHTMLPage && LicenseUtil.getLevel() > 199;
        boolean hasEndPoints = UtilMethods.isSet( receivingEndpoints ) && !receivingEndpoints.isEmpty();

        context.put( "EDIT_HTMLPAGE_PERMISSION", new Boolean( hasWritePermOverHTMLPage ) );
        context.put( "PUBLISH_HTMLPAGE_PERMISSION", new Boolean( hasPublishPermOverHTMLPage ) );
        context.put( "REMOTE_PUBLISH_HTMLPAGE_PERMISSION", new Boolean( hasRemotePublishPermOverHTMLPage ) );
        context.put( "REMOTE_PUBLISH_END_POINTS", new Boolean( hasEndPoints ) );
        context.put( "canAddForm", new Boolean( LicenseUtil.getLevel() > 199 ? true : false ) );
        context.put( "canViewDiff", new Boolean( LicenseUtil.getLevel() > 199 ? true : false ) );

        context.put( "HTMLPAGE_ASSET_STRUCTURE_TYPE", htmlPage.isContent() ? ((Contentlet)htmlPage).getStructureInode() : APILocator.getHTMLPageAssetAPI().DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        context.put("HTMLPAGE_IS_CONTENT", htmlPage.isContent());

		boolean canUserWriteOnTemplate = permissionAPI.doesUserHavePermission(
		        APILocator.getHTMLPageAssetAPI().getTemplate(htmlPage, true),
				PERMISSION_WRITE, user, true);
		context.put("EDIT_TEMPLATE_PERMISSION", canUserWriteOnTemplate);

		com.dotmarketing.portlets.templates.model.Template cmsTemplate = 
		                        APILocator.getHTMLPageAssetAPI().getTemplate(htmlPage, true);
		Identifier templateIdentifier = APILocator.getIdentifierAPI().find(cmsTemplate);

		Logger.debug(VelocityServlet.class, "VELOCITY TEMPLATE INODE=" + cmsTemplate.getInode());

		VelocityUtil.makeBackendContext(context, htmlPage, cmsTemplate.getInode(), id.getURI(), request, true, false, true, host);
		context.put("previewPage", "2");
		context.put("livePage", "0");
		

		
		
		// get the containers for the page and stick them in context
		List<Container> containers = APILocator.getTemplateAPI().getContainersInTemplate(cmsTemplate,
				APILocator.getUserAPI().getSystemUser(),false);
		for (Container c : containers) {

			context.put(String.valueOf("container" + c.getIdentifier()),
					"/working/" + c.getIdentifier() + "." + Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION"));

			context.put("EDIT_CONTAINER_PERMISSION" + c.getIdentifier(), permissionAPI.doesUserHavePermission(c, PERMISSION_WRITE, user, true));

			boolean hasWritePermOverTheStructure = false;

			for (ContainerStructure cs : APILocator.getContainerAPI().getContainerStructures(c)) {
				Structure st = CacheLocator.getContentTypeCache().getStructureByInode(cs.getStructureId());

				hasWritePermOverTheStructure |= permissionAPI.doesUserHavePermission(st, PERMISSION_WRITE, user, true);
			}


			context.put("ADD_CONTENT_PERMISSION" + c.getIdentifier(), new Boolean(hasWritePermOverTheStructure));

			Logger.debug(VelocityServlet.class, String.valueOf("container" + c.getIdentifier()) + "=/working/" + c.getIdentifier() + "."
					+ Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION"));

			String sort = (c.getSortContentletsBy() == null) ? "tree_order" : c.getSortContentletsBy();

			boolean staticContainer = !UtilMethods.isSet(c.getLuceneQuery());

			List<Contentlet> contentlets = null;

			// get contentlets only for main frame
			if (request.getParameter("mainFrame") != null) {
				if (staticContainer) {
					Logger.debug(VelocityServlet.class, "Static Container!!!!");

					Logger.debug(VelocityServlet.class, "html=" + htmlPage.getInode() + " container=" + c.getInode());

					// The container doesn't have categories
					Identifier idenHtmlPage = APILocator.getIdentifierAPI().find(htmlPage);
					Identifier idenContainer = APILocator.getIdentifierAPI().find(c);
					contentlets = conAPI.findPageContentlets(idenHtmlPage.getInode(), idenContainer.getInode(), sort, true, -1, user, true);
					Logger.debug(
							VelocityServlet.class,
							"Getting contentlets for language="
									+ (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE)
									+ " contentlets =" + contentlets.size());

				}

				if (UtilMethods.isSet(contentlets) && contentlets.size() > 0) {
					Set<String> contentletIdentList = new HashSet<String>();
					List<Contentlet> contentletsFilter = new ArrayList<Contentlet>();
					for (Contentlet cont : contentlets) {
						if (!contentletIdentList.contains(cont.getIdentifier())) {
							contentletIdentList.add(cont.getIdentifier());
							contentletsFilter.add(cont);
						}
					}
					contentlets = contentletsFilter;
				}
				List<String> contentletList = new ArrayList<String>();

				if (contentlets != null && contentlets.size() > 0) {
					Iterator<Contentlet> iter = contentlets.iterator();
					int count = 0;

					while (iter.hasNext() && (count < c.getMaxContentlets())) {
						count++;

						Contentlet contentlet = (Contentlet) iter.next();
						Identifier contentletIdentifier = APILocator.getIdentifierAPI().find(contentlet);

						boolean hasWritePermOverContentlet = permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user, true);

						context.put("EDIT_CONTENT_PERMISSION" + contentletIdentifier.getInode(), new Boolean(hasWritePermOverContentlet));

						contentletList.add(String.valueOf(contentletIdentifier.getInode()));
						Logger.debug(this, "Adding contentlet=" + contentletIdentifier.getInode());
						Structure contStructure = contentlet.getStructure();
						if (contStructure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
							Field field = contStructure.getFieldVar("widgetPreexecute");
							if (field != null && UtilMethods.isSet(field.getValues())) {
								preExecuteCode.append(field.getValues().trim() + "\n");
								widgetPreExecute = true;
							}
						}

					}
				}

				// sets contentletlist with all the files to load per
				// container
				context.put("contentletList" + c.getIdentifier(), contentletList);
				context.put("totalSize" + c.getIdentifier(), new Integer(contentletList.size()));
			}
		}

		Logger.debug(
				VelocityServlet.class,
				"Before finding template: /working/" + templateIdentifier.getInode() + "."
						+ Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION"));

		Logger.debug(VelocityServlet.class, "Velocity directory:" + VelocityUtil.getEngine().getProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH));

		if (request.getParameter("leftMenu") != null) {
			/*
			 * try to get the messages from the session
			 */

			List<String> list = new ArrayList<String>();
			if (SessionMessages.contains(request, "message")) {
				list.add((String) SessionMessages.get(request, "message"));
				SessionMessages.clear(request);
			}
			if (SessionMessages.contains(request, "custommessage")) {
				list.add((String) SessionMessages.get(request, "custommessage"));
				SessionMessages.clear(request);
			}

			if (list.size() > 0) {
				ArrayList<String> mymessages = new ArrayList<String>();
				Iterator<String> it = list.iterator();

				while (it.hasNext()) {
					try {
						String message = (String) it.next();
						Company comp = PublicCompanyFactory.getDefaultCompany();
						mymessages.add(LanguageUtil.get(comp.getCompanyId(), user.getLocale(), message));
					} catch (Exception e) {
					}
				}
				context.put("vmessages", mymessages);
			}

			template = VelocityUtil.getEngine().getTemplate("/preview_left_menu.vl");
		} else if (request.getParameter("mainFrame") != null) {
			hostVariablesTemplate = VelocityUtil.getEngine().getTemplate("/working/" + host.getIdentifier() + "." + Config.getStringProperty("VELOCITY_HOST_EXTENSION"));

            if ( cmsTemplate.isDrawed() ) {//We have a designed template
                //Setting some theme variables
                Map<String, Object> dotThemeData = DotTemplateTool.theme( cmsTemplate.getTheme(), host.getIdentifier() );
                context.put( "dotTheme", dotThemeData );
                context.put( "dotThemeLayout", DotTemplateTool.themeLayout( cmsTemplate.getInode() ) );
                //Our designed template
                template = VelocityUtil.getEngine().getTemplate( (String) dotThemeData.get( "templatePath" ) );
            } else {
                template = VelocityUtil.getEngine().getTemplate( "/working/" + templateIdentifier.getInode() + "." + Config.getStringProperty( "VELOCITY_TEMPLATE_EXTENSION" ) );
            }

		} else {
			template = VelocityUtil.getEngine().getTemplate("/preview_mode.vl");
		}

		PrintWriter out = response.getWriter();
		request.setAttribute("velocityContext", context);
		try {

			if (widgetPreExecute) {
				VelocityUtil.getEngine().evaluate(context, out, "", preExecuteCode.toString());
			}
			if (hostVariablesTemplate != null)
				hostVariablesTemplate.merge(context, out);
			template.merge(context, out);

		} catch (ParseErrorException e) {
			out.append(e.getMessage());
		}
	}

    @SuppressWarnings ("unchecked")
    protected void doEditMode ( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        String uri = request.getRequestURI();

        Host host = hostWebAPI.getCurrentHost(request);

        StringBuilder preExecuteCode = new StringBuilder();
        Boolean widgetPreExecute = false;

        // Getting the user to check the permissions
        com.liferay.portal.model.User backendUser = null;
        try {
            backendUser = com.liferay.portal.util.PortalUtil.getUser( request );
        } catch ( Exception nsue ) {
            Logger.warn( this, "Exception trying getUser: " + nsue.getMessage(), nsue );
        }

        // Getting the identifier from the uri
        Identifier id = APILocator.getIdentifierAPI().find( host, uri );
        request.setAttribute( "idInode", String.valueOf( id.getInode() ) );
        Logger.debug( VelocityServlet.class, "VELOCITY HTML INODE=" + id.getInode() );

        Template template;
        Template hostVariablesTemplate = null;

        // creates the context where to place the variables
        response.setContentType( CHARSET );
		request.setAttribute("EDIT_MODE", Boolean.TRUE);
        Context context = VelocityUtil.getWebContext( request, response );

		IHTMLPage htmlPage = VelocityUtil.getPage(id, request, false, context);
		if("contentlet".equals(htmlPage.getType())){
			context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), backendUser,true,host, context) );
		}

        PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
		List<PublishingEndPoint> receivingEndpoints = pepAPI.getReceivingEndPoints();
        // to check user has permission to write on this page
        boolean hasAddChildrenPermOverHTMLPage = permissionAPI.doesUserHavePermission( htmlPage, PERMISSION_CAN_ADD_CHILDREN, backendUser );
        boolean hasWritePermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_WRITE, backendUser);
        boolean hasPublishPermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_PUBLISH, backendUser);
        boolean hasRemotePublishPermOverHTMLPage = hasPublishPermOverHTMLPage && LicenseUtil.getLevel() > 199;
        boolean hasEndPoints = UtilMethods.isSet( receivingEndpoints ) && !receivingEndpoints.isEmpty();

        context.put( "ADD_CHILDREN_HTMLPAGE_PERMISSION", new Boolean( hasAddChildrenPermOverHTMLPage ) );
        context.put( "EDIT_HTMLPAGE_PERMISSION", new Boolean( hasWritePermOverHTMLPage ) );
        context.put( "PUBLISH_HTMLPAGE_PERMISSION", new Boolean( hasPublishPermOverHTMLPage ) );
        context.put( "REMOTE_PUBLISH_HTMLPAGE_PERMISSION", new Boolean( hasRemotePublishPermOverHTMLPage ) );
        context.put( "REMOTE_PUBLISH_END_POINTS", new Boolean(hasEndPoints) );
        context.put( "canAddForm", new Boolean( LicenseUtil.getLevel() > 199 ? true : false ) );
        context.put( "canViewDiff", new Boolean( LicenseUtil.getLevel() > 199 ? true : false ) );
        
        context.put( "HTMLPAGE_ASSET_STRUCTURE_TYPE", htmlPage.isContent() ? ((Contentlet)htmlPage).getStructureInode() : "0");
        context.put( "HTMLPAGE_IS_CONTENT" , htmlPage.isContent());

        boolean canUserWriteOnTemplate = permissionAPI.doesUserHavePermission( 
                APILocator.getHTMLPageAssetAPI().getTemplate(htmlPage, true), 
                PERMISSION_WRITE, backendUser ) 
                && portletAPI.hasTemplateManagerRights( backendUser );
        context.put( "EDIT_TEMPLATE_PERMISSION", canUserWriteOnTemplate );

        com.dotmarketing.portlets.templates.model.Template cmsTemplate = 
                APILocator.getHTMLPageAssetAPI().getTemplate(htmlPage, true);
        //issue- 1775 If User doesn't have edit permission on HTML Pages
       /* if(!hasWritePermOverHTMLPage){
        	doPreviewMode(request, response);
        	return;
        }*/
        if ( cmsTemplate == null ) {// DOTCMS-4051
            cmsTemplate = new com.dotmarketing.portlets.templates.model.Template();
            Logger.debug( VelocityServlet.class, "HTMLPAGE TEMPLATE NOT FOUND" );
        }

        Identifier templateIdentifier = APILocator.getIdentifierAPI().find( cmsTemplate );

        Logger.debug(VelocityServlet.class, "VELOCITY TEMPLATE INODE=" + cmsTemplate.getInode());

        VelocityUtil.makeBackendContext( context, htmlPage, cmsTemplate.getInode(), id.getURI(), request, true, true, false, host );
        // added to show tabs
        context.put( "previewPage", "1" );
        // get the containers for the page and stick them in context
        List<Container> containers = APILocator.getTemplateAPI().getContainersInTemplate( cmsTemplate, APILocator.getUserAPI().getSystemUser(), false );
        for ( Container c : containers ) {

            context.put( String.valueOf( "container" + c.getIdentifier() ), "/working/" + c.getIdentifier() + "." + Config.getStringProperty( "VELOCITY_CONTAINER_EXTENSION" ) );

            boolean hasWritePermissionOnContainer = permissionAPI.doesUserHavePermission( c, PERMISSION_WRITE, backendUser, false )
                    && portletAPI.hasContainerManagerRights( backendUser );
            boolean hasReadPermissionOnContainer = permissionAPI.doesUserHavePermission( c, PERMISSION_READ, backendUser, false );
            context.put( "EDIT_CONTAINER_PERMISSION" + c.getIdentifier(), hasWritePermissionOnContainer );
            if ( Config.getBooleanProperty( "SIMPLE_PAGE_CONTENT_PERMISSIONING", true ) )
                context.put( "USE_CONTAINER_PERMISSION" + c.getIdentifier(), true );
            else
                context.put( "USE_CONTAINER_PERMISSION" + c.getIdentifier(), hasReadPermissionOnContainer );

            // to check user has permission to write this container
            boolean hasWritePermOverTheStructure = false;

			for (ContainerStructure cs : APILocator.getContainerAPI().getContainerStructures(c)) {
				Structure st = CacheLocator.getContentTypeCache().getStructureByInode(cs.getStructureId());

				hasWritePermOverTheStructure |= permissionAPI.doesUserHavePermission(st, PERMISSION_WRITE, backendUser);
			}


            context.put( "ADD_CONTENT_PERMISSION" + c.getIdentifier(), new Boolean( hasWritePermOverTheStructure ) );

            Logger.debug( VelocityServlet.class, String.valueOf( "container" + c.getIdentifier() ) + "=/working/" + c.getIdentifier() + "."
                    + Config.getStringProperty( "VELOCITY_CONTAINER_EXTENSION" ) );

            String sort = (c.getSortContentletsBy() == null) ? "tree_order" : c.getSortContentletsBy();

            List<Contentlet> contentlets = null;

            boolean staticContainer = !UtilMethods.isSet( c.getLuceneQuery() );

            // get contentlets only for main frame
            if ( request.getParameter( "mainFrame" ) != null ) {
                if ( staticContainer ) {
                    Logger.debug( VelocityServlet.class, "Static Container!!!!" );

                    Logger.debug( VelocityServlet.class, "html=" + htmlPage.getInode() + " container=" + c.getInode() );

                    // The container doesn't have categories
                    Identifier idenHtmlPage = APILocator.getIdentifierAPI().find( htmlPage );
                    Identifier idenContainer = APILocator.getIdentifierAPI().find( c );
                    contentlets = conAPI.findPageContentlets( idenHtmlPage.getInode(), idenContainer.getInode(), sort, true, -1, backendUser, true );
                    Logger.debug(
                            VelocityServlet.class,
                            "Getting contentlets for language="
                                    + (String) request.getSession().getAttribute( com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE )
                                    + " contentlets =" + contentlets.size() );

                } else {
                    String luceneQuery = c.getLuceneQuery();
                    int limit = c.getMaxContentlets();
                    String sortBy = c.getSortContentletsBy();
                    int offset = 0;
                    contentlets = conAPI.search( luceneQuery, limit, offset, sortBy, backendUser, true );
                }

                if ( UtilMethods.isSet( contentlets ) && contentlets.size() > 0 ) {
                    Set<String> contentletIdentList = new HashSet<String>();
                    List<Contentlet> contentletsFilter = new ArrayList<Contentlet>();
                    for ( Contentlet cont : contentlets ) {
                        if ( !contentletIdentList.contains( cont.getIdentifier() ) ) {
                            contentletIdentList.add( cont.getIdentifier() );
                            contentletsFilter.add( cont );
                        }
                    }
                    contentlets = contentletsFilter;
                }
                List<String> contentletList = new ArrayList<String>();

                if ( contentlets != null ) {
                    Iterator<Contentlet> iter = contentlets.iterator();
                    int count = 0;

                    while ( iter.hasNext() && (count < c.getMaxContentlets()) ) {
                        count++;

                        Contentlet contentlet = (Contentlet) iter.next();
                        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( contentlet );

                        boolean hasWritePermOverContentlet = permissionAPI.doesUserHavePermission( contentlet, PERMISSION_WRITE, backendUser );

                        context.put( "EDIT_CONTENT_PERMISSION" + contentletIdentifier.getInode(), new Boolean( hasWritePermOverContentlet ) );

                        contentletList.add( String.valueOf( contentletIdentifier.getInode() ) );
                        Logger.debug( this, "Adding contentlet=" + contentletIdentifier.getInode() );
                        Structure contStructure = contentlet.getStructure();
                        if ( contStructure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET ) {
                            Field field = contStructure.getFieldVar( "widgetPreexecute" );
                            if ( field != null && UtilMethods.isSet( field.getValues() ) ) {
                                preExecuteCode.append( field.getValues().trim() + "\n" );
                                widgetPreExecute = true;
                            }

                        }
                    }
                }
                // sets contentletlist with all the files to load per
                // container
                context.put( "contentletList" + c.getIdentifier(), contentletList );
                context.put( "totalSize" + c.getIdentifier(), new Integer( contentletList.size() ) );
                // ### Add the structure fake contentlet ###
                if ( contentletList.size() == 0 ) {

                	List<ContainerStructure> csList = APILocator.getContainerAPI().getContainerStructures(c);
                    for (ContainerStructure cs : csList) {
                    	contentletList.add( cs.getStructureId() + "" );
					}


                    // sets contentletlist with all the files to load per
                    // container
                    context.remove( "contentletList" + c.getIdentifier() );
                    context.remove( "totalSize" + c.getIdentifier() );
                    // http://jira.dotmarketing.net/browse/DOTCMS-2876
                    context.put( "contentletList" + c.getIdentifier(), new long[0] );
                    context.put( "totalSize" + c.getIdentifier(), 0 );
                }
                // ### END Add the structure fake contentlet ###

            }
        }

        Logger.debug(
                VelocityServlet.class,
                "Before finding template: /working/" + templateIdentifier.getInode() + "."
                        + Config.getStringProperty( "VELOCITY_TEMPLATE_EXTENSION" ) );

        Logger.debug( VelocityServlet.class, "Velocity directory:" + VelocityUtil.getEngine().getProperty( RuntimeConstants.FILE_RESOURCE_LOADER_PATH ) );

        if ( request.getParameter( "leftMenu" ) != null ) {
            /*
                * try to get the messages from the session
                */

            List<String> list = new ArrayList<String>();
            if ( SessionMessages.contains( request, "message" ) ) {
                list.add( (String) SessionMessages.get( request, "message" ) );
                SessionMessages.clear( request );
            }
            if ( SessionMessages.contains( request, "custommessage" ) ) {
                list.add( (String) SessionMessages.get( request, "custommessage" ) );
                SessionMessages.clear( request );
            }

            if ( list.size() > 0 ) {
                ArrayList<String> mymessages = new ArrayList<String>();
                Iterator<String> it = list.iterator();

                while ( it.hasNext() ) {
                    try {
                        String message = (String) it.next();
                        Company comp = PublicCompanyFactory.getDefaultCompany();
                        mymessages.add( LanguageUtil.get( comp.getCompanyId(), backendUser.getLocale(), message ) );
                    } catch ( Exception e ) {
                    }
                }
                context.put( "vmessages", mymessages );
            }

            template = VelocityUtil.getEngine().getTemplate( "/preview_left_menu.vl" );
        } else if ( request.getParameter( "mainFrame" ) != null ) {
            hostVariablesTemplate = VelocityUtil.getEngine().getTemplate( "/working/" + host.getIdentifier() + "." + Config.getStringProperty( "VELOCITY_HOST_EXTENSION" ) );

            if ( cmsTemplate.isDrawed() ) {//We have a designed template
                //Setting some theme variables
                Map<String, Object> dotThemeData = DotTemplateTool.theme( cmsTemplate.getTheme(), host.getIdentifier() );
                context.put( "dotTheme", dotThemeData );
                context.put( "dotThemeLayout", DotTemplateTool.themeLayout( cmsTemplate.getInode() ) );
                //Our designed template
                template = VelocityUtil.getEngine().getTemplate( (String) dotThemeData.get( "templatePath" ) );
            } else {
                template = VelocityUtil.getEngine().getTemplate( "/working/" + templateIdentifier.getInode() + "." + Config.getStringProperty( "VELOCITY_TEMPLATE_EXTENSION" ) );
            }
        } else {
            // Return a resource not found right away if the page is not found,
            // not try to load the frames
            if ( !InodeUtils.isSet( templateIdentifier.getInode() ) )
                throw new ResourceNotFoundException( "" );
            template = VelocityUtil.getEngine().getTemplate( "/preview_mode.vl" );
        }

        PrintWriter out = response.getWriter();
        request.setAttribute( "velocityContext", context );
        try {
            if ( widgetPreExecute ) {
                VelocityUtil.getEngine().evaluate( context, out, "", preExecuteCode.toString() );
            }
            if ( hostVariablesTemplate != null )
                hostVariablesTemplate.merge( context, out );
            template.merge( context, out );

        } catch ( ParseErrorException e ) {
            out.append( e.getMessage() );
        }
    }





	// EACH CLIENT MAY HAVE ITS OWN VARIABLES
	// WE HAVE THE CLASS CLIENT THAT WILL IMPLEMENT THIS METHOD AND WILL BE ON
	// THE WEB.XML FILE
	protected abstract void _setClientVariablesOnContext(HttpServletRequest request, ChainedContext context);

	private boolean isArchive(HttpServletRequest request, HttpServletResponse response, String uri) throws PortalException, SystemException, DotDataException, DotSecurityException {



		Host host = null;
		String hostId = "";

		/*
		 * String pageHostId = request.getParameter("host_id"); if (pageHostId
		 * != null) { try { hostId = Long.parseLong(pageHostId); } catch
		 * (Exception ex) { } }
		 */
		hostId = request.getParameter("host_id");
		if (!InodeUtils.isSet(hostId)) {
			host = hostWebAPI.getCurrentHost(request);
			hostId = host.getIdentifier();
		}
		else {
			User user = (com.liferay.portal.model.User) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
			if(user==null) {
				user = com.liferay.portal.util.PortalUtil.getUser(request);
			}
			host = hostWebAPI.find(hostId, user, true);
		}

		// Getting the identifier from the uri
		Identifier id = APILocator.getIdentifierAPI().find(host, uri);
		long langId = VelocityUtil.getLanguageId(request);
		request.setAttribute("idInode", String.valueOf(id.getInode()));
		
		IHTMLPage htmlPage = VelocityUtil.getPage(id, request, false, VelocityUtil.getWebContext(request, response));

		return htmlPage.isArchived();
	}

	/**
	 * @author will this filter class strips all leading whitespace from the
	 *         server response which is helpful for xml feeds and the like.
	 */

	public class VelocityFilterWriter extends FilterWriter {

		private boolean firstNonWhiteSpace = false;

		public VelocityFilterWriter(Writer arg0) {
			super(arg0);

		}

		@Override
		public void write(char[] arg0) throws IOException {
			if (firstNonWhiteSpace) {
				super.write(arg0);
			} else {

				for (int i = 0; i < arg0.length; i++) {
					if (arg0[i] > 32) {
						firstNonWhiteSpace = true;
					}
					if (firstNonWhiteSpace) {
						super.write(arg0[i]);
					}

				}

			}

		}

		@Override
		public void write(String arg0) throws IOException {
			if (firstNonWhiteSpace) {
				super.write(arg0);
			} else {
				char[] stringChar = arg0.toCharArray();
				for (int i = 0; i < stringChar.length; i++) {

					if (stringChar[i] > 32) {
						firstNonWhiteSpace = true;
						super.write(arg0.substring(i, stringChar.length));
						break;
					}

				}

			}

		}

	}

}
