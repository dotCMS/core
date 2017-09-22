package com.dotmarketing.util;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.base.CaseFormat;
import com.dotcms.repackage.org.apache.logging.log4j.util.Strings;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.DisplayedLanguage;
import com.dotmarketing.viewtools.LanguageWebAPI;
import com.liferay.portal.model.User;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.ToolboxManager;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.velocity.VelocityServlet;
import com.dotmarketing.viewtools.RequestWrapper;
import com.liferay.portal.model.Company;
import com.liferay.util.*;


public class VelocityUtil {

	private static VelocityEngine ve = null;
	private static String dotResourceLoaderClassName = null;
	private static boolean DEFAULT_PAGE_TO_DEFAULT_LANGUAGE = LanguageWebAPI.canDefaultPageToDefaultLanguage();
	
	private synchronized static void init(){
		if(ve != null)
			return;
		ve = new VelocityEngine();
		try{
			ve.init(SystemProperties.getProperties());
			dotResourceLoaderClassName = SystemProperties.get(SystemProperties.get("resource.loader") + ".resource.loader.class");
			Logger.debug(VelocityUtil.class, SystemProperties.getProperties().toString());
		}catch (Exception e) {
			Logger.error(VelocityUtil.class,e.getMessage(),e);
		}
	}
	
	public static VelocityEngine getEngine(){
		if(ve == null){
			init();
			if(ve == null){
				Logger.fatal(VelocityUtil.class,"Velocity Engine unable to initialize : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("Velocity Engine unable to initialize : THIS SHOULD NEVER HAPPEN");
			}
		}
		return ve;
	}
	/**
	 * Changes $ and # to velocity escapes.  This is helps filter out velocity code injections.
	 * @param s 
	 * @return
	 */
	public static String cleanVelocity(String s) {
		if (s==null) {
			return null;
		}
		s=s.replace("$", "${esc.dollar}");
		s=s.replace("#", "${esc.hash}");
		return s;
	}

	public static String getDotResourceLoaderClassName() {
		if(dotResourceLoaderClassName == null){
			init();
			if(dotResourceLoaderClassName == null){
				Logger.fatal(VelocityUtil.class,"Velocity Engine unable to initialize : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("Velocity Engine unable to initialize : THIS SHOULD NEVER HAPPEN");
			}
		}
		return dotResourceLoaderClassName;
	}
	
	public String parseVelocity(String velocityCode, Context ctx){
		VelocityEngine ve = VelocityUtil.getEngine();
		StringWriter stringWriter = new StringWriter();
		try {
		   ve.evaluate(ctx, stringWriter, "VelocityUtil:parseVelocity", velocityCode);
		}catch (Exception e) {
		Logger.error(this,e.getMessage(),e);
		}
		return stringWriter.toString(); 
		
	}


	public static String convertToVelocityVariable(final String variable, boolean firstLetterUppercase){
		

	      return (firstLetterUppercase) 
	              ? StringUtils.camelCaseUpper(variable)
	              : StringUtils.camelCaseLower(variable);
		
	}
	
	
	public static Boolean isNotAllowedVelocityVariableName(String variable){
		

		String [] notallwdvelvars={"inode","type", "modDate", "owner", "ownerCanRead", "ownerCanWrite", "ownerCanPublish",
				"modUser", "working", "live", "deleted", "locked","structureInode", "languageId", "permissions",
				"identifier", "conHost", "conFolder", "Host", "folder"}; 
		Boolean found=false;
		for(String notallowed : notallwdvelvars){
			 if(variable.equalsIgnoreCase(notallowed)){
				 found=true;
			 }
			
		}
		return found;
	}

	/**
	 * Returns a basic Velocity Context without any toolbox or request
	 * response, session objects;
	 * @return
	 */

	public static Context getBasicContext() {
		Context context = new VelocityContext();
		VelocityServlet.velocityCtx.set(context);
		context.put("UtilMethods", new UtilMethods());
		context.put("PortletURLUtil", new PortletURLUtil());
		context.put("quote", "\"");
		context.put("pounds", "##");
		context.put("return", "\n");
		context.put("velocityContext", context);
		context.put("language", "1");
		context.put("InodeUtils", new InodeUtils());
		return context;
	}
	
	
	/**
	 * Gets creates Velocity context will all the toolbox, user, host, language and request stuff
	 * inside the map
	 * @param request
	 * @param response
	 * @return
	 */
	public static ChainedContext getWebContext(HttpServletRequest request, HttpServletResponse response) {
		return getWebContext(getBasicContext(), request, response);
	}
	
	
	public static ChainedContext getWebContext(Context ctx, HttpServletRequest request, HttpServletResponse response) {

        if ( ctx == null ) {
            ctx = getBasicContext();
        }

        // http://jira.dotmarketing.net/browse/DOTCMS-2917

		//get the context from the request if possible
        ChainedContext context;
        if ( request.getAttribute( VelocityServlet.VELOCITY_CONTEXT ) != null && request.getAttribute( VelocityServlet.VELOCITY_CONTEXT ) instanceof ChainedContext ) {
            return (ChainedContext) request.getAttribute( "velocityContext" );
        } else {
            RequestWrapper rw = new RequestWrapper( request );
            if ( request.getAttribute( "User-Agent" ) != null && request.getAttribute( "User-Agent" ).equals( Constants.USER_AGENT_DOTCMS_BROWSER ) ) {
                rw.setCustomUserAgentHeader( Constants.USER_AGENT_DOTCMS_BROWSER );
            }
            context = new ChainedContext( ctx, getEngine(), rw, response, Config.CONTEXT );
        }

        context.put("context", context);
		Logger.debug(VelocityServlet.class, "ChainedContext=" + context);
		/*
		 * if we have a toolbox manager, get a toolbox from it See
		 * /WEB-INF/toolbox.xml
		 */
		context.setToolbox(getToolboxManager().getToolboxContext(context));


		// put the list of languages on the page
		context.put("languages", getLanguages());
		HttpSession session = request.getSession(false);
		if(!UtilMethods.isSet(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)) && session!=null)
		    context.put("language", (String) session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE));
		else
		    context.put("language", request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE));

		try {
			Host host;
			host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			context.put("host", host);
		} catch (Exception e) {
			Logger.error(VelocityUtil.class,e.getMessage(),e);
		}
		context.put("pdfExport", false);

		if(request.getSession(false)!=null){
			try {
				User user = (com.liferay.portal.model.User) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
				context.put("user", user);

				Visitor visitor = (Visitor) request.getSession().getAttribute(WebKeys.VISITOR);
				context.put("visitor", visitor);

			} catch (Exception nsue) {
				Logger.error(VelocityServlet.class, nsue.getMessage(), nsue);
			}
		}
		VelocityServlet.velocityCtx.set(context);
		return context;

	}

	public static String mergeTemplate(String templatePath, Context ctx) throws ResourceNotFoundException, ParseErrorException, Exception{
		VelocityEngine ve = VelocityUtil.getEngine();
		Template template = null;
		StringWriter sw = new StringWriter();
		template = ve.getTemplate(templatePath);

		template.merge(ctx, sw);

		return sw.toString();
		
	}
	
	public static String eval(String velocity, Context ctx) throws ResourceNotFoundException, ParseErrorException, Exception{
		VelocityEngine ve = VelocityUtil.getEngine();
		StringWriter sw = new StringWriter();
		ve.evaluate(ctx, sw, "velocity eval", velocity);
		return sw.toString();
		
	}
	private static ToolboxManager toolboxManager=null;
    public static ToolboxManager getToolboxManager () {
        if ( toolboxManager == null ) {
            synchronized ( VelocityUtil.class ) {
                if ( toolboxManager == null ) {
                    toolboxManager = ServletToolboxManager.getInstance( Config.CONTEXT, Config.getStringProperty("TOOLBOX_MANAGER_PATH", "/WEB-INF/toolbox.xml"));
                }
            }

        }
        return toolboxManager;
    }
	
	private static List<Language> languages =null;
	
	private static List<Language> getLanguages(){
		if(languages ==null){
			synchronized (VelocityUtil.class) {
				if(languages ==null){
					languages = APILocator.getLanguageAPI().getLanguages();
				}
			}
		}
		return languages;
		
	}
	

	
	public static void makeBackendContext(Context context, IHTMLPage htmlPage, String cmsTemplateInode, String idURI, HttpServletRequest request,
			boolean ADMIN_MODE, boolean EDIT_MODE, boolean PREVIEW_MODE, Host host) throws DotDataException {
		context.put("context", context);

		// stick some useful variables in the context
		if (htmlPage != null) {
			context.put("HTMLPAGE_INODE", String.valueOf(htmlPage.getInode()));
			context.put("HTMLPAGE_IDENTIFIER", String.valueOf(htmlPage.getIdentifier()));
			context.put("HTMLPAGE_TITLE", htmlPage.getTitle());
			context.put("HTMLPAGE_META", htmlPage.getMetadata());
			//http://jira.dotmarketing.net/browse/DOTCMS-6427
			context.put("HTMLPAGE_DESCRIPTION", htmlPage.getSeoDescription());
			context.put("HTMLPAGE_KEYWORDS", htmlPage.getSeoKeywords());
			context.put("HTMLPAGE_SECURE", String.valueOf(htmlPage.isHttpsRequired()));
			context.put("HTMLPAGE_REDIRECT", htmlPage.getRedirect());
			context.put("friendlyName", htmlPage.getFriendlyName());
			context.put("pageTitle", htmlPage.getTitle());
			Date moddate = htmlPage.getModDate();

			moddate = new Date(moddate.getTime());

			context.put("HTML_PAGE_LAST_MOD_DATE", moddate);

			try {
				context.put("htmlPageInode", htmlPage.getInode());

				// for browsing the tree
				String view = java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue()
						+ " and deleted=" + com.dotmarketing.db.DbConnectionFactory.getDBFalse() + "and language_id = "
						+ (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) + ")", "UTF-8");
				context.put("view", view);
			} catch (Exception e) {
				Logger.warn(VelocityUtil.class, e.toString(), e);
			}
		}

		context.put("HTMLPAGE_SERVER_NAME", request.getServerName());
		context.put("VTLSERVLET_URI", UtilMethods.encodeURIComponent(idURI));
		if (request.getQueryString() != null && request.getQueryString().length() > 0) {
			context.put("queryString", request.getQueryString());
		} else {
			context.put("queryString", "");
		}
		context.put("TEMPLATE_INODE", String.valueOf(cmsTemplateInode));

		context.put("mainFrame", request.getParameter("mainFrame"));
		context.put("previewFrame", request.getParameter("previewFrame"));

		if (EDIT_MODE) {
			// gets user id from request for mod user
			com.liferay.portal.model.User backendUser = null;

			try {
				backendUser = com.liferay.portal.util.PortalUtil.getUser(request);
				// Skin skin = backendUser.getSkin();
				// context.put("USER_SKIN", skin.getSkinId());
			} catch (Exception nsue) {
				Logger.warn(VelocityUtil.class, "Exception trying yo getUser: " + nsue.getMessage(), nsue);
			}

			// to check user has permission to publish this page
			boolean permission = APILocator.getPermissionAPI().doesUserHavePermission(htmlPage, PERMISSION_PUBLISH, backendUser);
			context.put("permission", new Boolean(permission));

			// Check if the user is a CMS Administrator
			boolean adminUser = false;
			try {
				Company company = null;
				company = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

				String adminRoleKey = "";
				try {
					Role adminRole = APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CMS_ADMINISTRATOR_ROLE"));
					adminRoleKey = adminRole.getRoleKey();
				} catch (Exception e) {
				}

				Role[] userRoles = (Role[]) APILocator.getRoleAPI().loadRolesForUser(backendUser.getUserId()).toArray(new Role[0]);
				for (int i = 0; i < userRoles.length; i++) {
					Role userRole = (Role) userRoles[i];
					if (userRole.getRoleKey().equals(adminRoleKey)) {
						adminUser = true;
					}
				}
			} catch (Exception e) {
			}
			context.put("cmsAdminUser", new Boolean(adminUser));

		}

		// gets pageChannel for this path
		String pageChannel = UtilMethods.getPageChannel(idURI);
		context.put("pageChannel", pageChannel);
		context.put("PREVIEW_MODE", new Boolean(PREVIEW_MODE));
		context.put("EDIT_MODE", new Boolean(EDIT_MODE));
		context.put("ADMIN_MODE", new Boolean(ADMIN_MODE));

		// for publish button on admin control
		// I HAVE TO FIX THIS!!!!! FOR THE NEW ONE
		context.put("TEMPLATE_LIVE_CONTENT", new Boolean(false));
		context.put("CONTAINER_LIVE_CONTENT", new Boolean(false));
		context.put("CONTENTLET_LIVE_CONTENT", new Boolean(false));

		// If we are on preview mode and seeing the live page
		String livePage = (EDIT_MODE && ADMIN_MODE) ? "0" : "1";

		Logger.debug(VelocityServlet.class, "livePage=" + livePage);

		context.put("livePage", livePage);

		context.put("language", (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE));

		if (ADMIN_MODE) {

			// Making sure you are viewing the latest list of languages on any
			// admin mode
			languages = APILocator.getLanguageAPI().getLanguages();
			context.put("languages", languages);

			// gets user id from request for mod user
			com.liferay.portal.model.User backendUser = null;

			try {
				backendUser = com.liferay.portal.util.PortalUtil.getUser(request);
				// Skin skin = backendUser.getSkin();
				// context.put("USER_SKIN", skin.getSkinId());
				context.put("backendUser", backendUser);
			} catch (Exception nsue) {
				Logger.warn(VelocityUtil.class, "Exception trying yo getUser: " + nsue.getMessage(), nsue);
			}

			HttpSession session = request.getSession();
			context.put("directorURL", session.getAttribute(com.dotmarketing.util.WebKeys.DIRECTOR_URL));
			context.put("viewFoldersURL", session.getAttribute(com.dotmarketing.util.WebKeys.VIEW_FOLDERS_URL));
			context.put("previewPageURL", session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_PAGE_URL));
			context.put("viewContentsURL", session.getAttribute(com.dotmarketing.util.WebKeys.VIEW_CONTENTS_URL));
			context.put("viewBrowserURL", session.getAttribute(com.dotmarketing.util.WebKeys.VIEW_BROWSER_URL));



		}

		context.put("host", host);

	}

	/**
	 * This method tries to build a cache key based on information given in the
	 * request. Post requests are ignored and will not be cached.
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The page cache key if the page can be cached. If it can't be
	 *         cached or caching is not available, returns <code>null</code>.
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public static String getPageCacheKey(HttpServletRequest request, HttpServletResponse response) throws DotDataException, DotSecurityException {
		if (LicenseUtil.getLevel() <= LicenseLevel.COMMUNITY.level) {
			return null;
		}
		// don't cache posts
		if (!"GET".equalsIgnoreCase(request.getMethod())) {
			return null;
		}
		// nocache passed either as a session var, as a request var or as a
		// request attribute
		if ("no".equals(request.getParameter("dotcache"))
				|| "no".equals(request.getAttribute("dotcache"))
				|| (request.getSession(false) !=null && "no".equals(request.getSession(true).getAttribute("dotcache")))) {
			return null;
		}
		String idInode = (String) request.getAttribute("idInode");
		Identifier id;

		try {
			id=APILocator.getIdentifierAPI().find(idInode);
		} catch (DotDataException e1) {
			Logger.warn(VelocityUtil.class, "can't load page identifier",e1);
			return null;
		}

		IHTMLPage page = getPage(id, request, false, null);
		if (page == null || page.getCacheTTL() < 1) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(page.getInode());
		sb.append("_" + page.getModDate().getTime());
		return sb.toString();
	}

	public static long getLanguageId(HttpServletRequest request) {
		return WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
	}

	/**
	 * Retrieves the list of languages a given Content Page ({@link Contentlet})
	 * is available on. This is useful for keeping users from selecting a page
	 * language that has no associated content at the moment.
	 *
	 * @param contentlet
	 *            - The Content Page.
	 * @return The {@link List} of languages the content page is available on.
	 */
	public static List<DisplayedLanguage> getAvailableContentPageLanguages(
			Contentlet contentlet) {
		List<DisplayedLanguage> languages = new ArrayList<DisplayedLanguage>();
		List<DisplayedLanguage> allDisplayLanguages = new ArrayList<DisplayedLanguage>();

		boolean doesContentHaveDefaultLang = false;

		for (Language language : APILocator.getLanguageAPI().getLanguages()) {
			if (language.getId() != contentlet.getLanguageId()) {
				try {
					APILocator.getContentletAPI()
							.findContentletByIdentifier(
									contentlet.getIdentifier(), false,
									language.getId(),
									APILocator.getUserAPI().getSystemUser(),
									false);
				} catch (Exception e) {
					Logger.debug(
							VelocityServlet.class,
							"The page is not available in language "
									+ language.getId() + ". Just keep going.");

					if(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE) {
						allDisplayLanguages.add(new DisplayedLanguage(language, true));
					}

					continue;
				}
			}
			if(language.getId()==APILocator.getLanguageAPI().getDefaultLanguage().getId()) {
				doesContentHaveDefaultLang = true;
			}

			languages.add(new DisplayedLanguage(language, false));

			if(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE) {
				allDisplayLanguages.add(new DisplayedLanguage(language, false));
			}
		}

		if(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE && doesContentHaveDefaultLang){
			return allDisplayLanguages;
		}

		return languages;
	}

	/**
	 * This returns the proper ihtml page based on id, state and language
	 * @param id
	 * @param request
	 * @param live
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static IHTMLPage getPage(Identifier id, HttpServletRequest request, boolean live, Context context) throws DotDataException, DotSecurityException{

		long langId = getLanguageId(request);
		request.setAttribute("idInode", String.valueOf(id.getInode()));

		IHTMLPage htmlPage;
		if(id.getAssetType().equals("contentlet")) {

			Contentlet contentlet;

			try{
				contentlet = APILocator.getContentletAPI()
								.findContentletByIdentifier(id.getId(),
										live,
										langId,
										APILocator.getUserAPI().getSystemUser(), false);
				htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);

			} catch(DotStateException dse){
				if(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE && langId!= APILocator.getLanguageAPI().getDefaultLanguage().getId()){
					contentlet = APILocator.getContentletAPI()
									.findContentletByIdentifier(id.getId(),
											live,
											APILocator.getLanguageAPI().getDefaultLanguage().getId(),
											APILocator.getUserAPI().getSystemUser(), false);
					htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
				} else{
					throw new DotDataException("Can't find content. Identifier: " + id.getId() + ", Live: " +live+ ", Lang: " + langId, dse);
				}

			}

			if(UtilMethods.isSet(context)){
				context.put("availablePageLangs", getAvailableContentPageLanguages(contentlet));
			}
		} else {
			htmlPage = (IHTMLPage) APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(), false);
		}

		return htmlPage;
	}

	/**
	 * Gets the Velocity Root Path. Looks for it on the Config, if not found the it get defaulted to /WEB-INF/velocity
	 *
	 * @return String
	 */
	public static String getVelocityRootPath() {
		Logger.debug(VelocityUtil.class, "Fetching the velocity ROOT path...");

		String velocityRootPath;

		velocityRootPath = Config.getStringProperty("VELOCITY_ROOT", "/WEB-INF/velocity");
		if (velocityRootPath.startsWith("/WEB-INF")) {
			Logger.debug(VelocityUtil.class, "Velocity ROOT Path not found, defaulting it to '/WEB-INF/velocity'");
			String startPath = velocityRootPath.substring(0, 8);
			String endPath = velocityRootPath.substring(9, velocityRootPath.length());
			velocityRootPath = com.liferay.util.FileUtil.getRealPath(startPath) + File.separator + endPath;
		}

		Logger.debug(VelocityUtil.class, String.format("Velocity ROOT path found: %s", velocityRootPath));
		return velocityRootPath;
	}

}
