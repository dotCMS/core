package com.dotcms.rendering.velocity.util;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.velocity.viewtools.LanguageWebAPI;
import com.dotcms.rendering.velocity.viewtools.RequestWrapper;
import com.dotcms.visitor.domain.Visitor;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.DisplayedLanguage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.ToolboxManager;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;

import com.liferay.portal.model.User;
import com.liferay.util.SystemProperties;


public class VelocityUtil {
    public final static String REFRESH="refresh";
    public final static String NO="no";
    public final static String DOTCACHE="dotcache";
	private static VelocityEngine ve = null;
	private static boolean DEFAULT_PAGE_TO_DEFAULT_LANGUAGE = LanguageWebAPI.canDefaultPageToDefaultLanguage();
	
	private synchronized static void init(){
		if(ve != null)
			return;
		ve = new VelocityEngine();
		try{
			ve.init(SystemProperties.getProperties());

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
        if ( request.getAttribute( com.dotcms.rendering.velocity.Constants.VELOCITY_CONTEXT ) != null && request.getAttribute( com.dotcms.rendering.velocity.Constants.VELOCITY_CONTEXT ) instanceof ChainedContext ) {
            return (ChainedContext) request.getAttribute( "velocityContext" );
        } else {
            RequestWrapper rw = new RequestWrapper( request );
            if ( request.getAttribute( "User-Agent" ) != null && request.getAttribute( "User-Agent" ).equals( Constants.USER_AGENT_DOTCMS_BROWSER ) ) {
                rw.setCustomUserAgentHeader( Constants.USER_AGENT_DOTCMS_BROWSER );
            }
            context = new ChainedContext( ctx, getEngine(), rw, response, Config.CONTEXT );
        }

        context.put("context", context);
		Logger.debug(VelocityUtil.class, "ChainedContext=" + context);
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
        context.put("dotPageMode", PageMode.get(request));
		if(request.getSession(false)!=null){
			try {
				User user = (com.liferay.portal.model.User) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
				context.put("user", user);

				Visitor visitor = (Visitor) request.getSession().getAttribute(WebKeys.VISITOR);
				context.put("visitor", visitor);

			} catch (Exception nsue) {
				Logger.error(VelocityUtil.class, nsue.getMessage(), nsue);
			}
		}
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
	

	
	   

    /**
     * This method tries to build a cache key based on information given in the request. Post
     * requests are ignored and will not be cached.
     *
     * @param request - The {@link HttpServletRequest} object.
     * @return The page cache key if the page can be cached. If it can't be cached or caching is not
     *         available, returns <code>null</code>.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public static String getPageCacheKey(final HttpServletRequest request, final IHTMLPage page)
            throws DotDataException, DotSecurityException {
        if (LicenseUtil.getLevel() <= LicenseLevel.COMMUNITY.level) {
            return null;
        }
        if (page == null || page.getCacheTTL() < 1) {
            return null;
        }
        // don't cache posts
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        // nocache passed either as a session var, as a request var or as a
        // request attribute
        if (NO.equals(request.getParameter(DOTCACHE)) || REFRESH.equals(request.getParameter(DOTCACHE))
                || NO.equals(request.getAttribute(DOTCACHE))
                || (request.getSession(false) != null && NO.equals(request.getSession(true).getAttribute(DOTCACHE)))) {
            return null;
        }


        StringBuilder sb = new StringBuilder();
        sb.append(page.getInode());
        sb.append("_" + page.getModDate().getTime());
        return sb.toString();
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
					        VelocityUtil.class,
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
     * 
     * @param id
     * @param request
     * @param live
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static IHTMLPage getPage(Identifier id, long tryLang, boolean live)
            throws DotDataException, DotSecurityException {


        IHTMLPage htmlPage;


        Contentlet contentlet;

        try {
            contentlet = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), live, tryLang,
                    APILocator.getUserAPI().getSystemUser(), false);
            htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);

        } catch (DotStateException dse) {
            if (DEFAULT_PAGE_TO_DEFAULT_LANGUAGE && tryLang != APILocator.getLanguageAPI().getDefaultLanguage().getId()) {
                contentlet = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), live,
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
                htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
            } else {
                throw new DotDataException(
                        "Can't find content. Identifier: " + id.getId() + ", Live: " + live + ", Lang: " + tryLang, dse);
            }

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
