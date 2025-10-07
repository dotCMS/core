package com.dotcms.rendering.velocity.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.cost.RequestCost;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.directive.DotCacheDirective;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rendering.velocity.viewtools.content.ContentTool;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.container.ContainerResource;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.DisplayedLanguage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.StringPool;
import com.liferay.util.SystemProperties;
import io.vavr.control.Try;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

public class VelocityUtil {
    public final static String REFRESH="refresh";
    public final static String NO="no";
    public final static String DOTCACHE="dotcache";
	private static VelocityEngine ve = null;
	private static Map<String, String> digitToLetter = new HashMap<>();

	static {
		digitToLetter.put("0", "zero");
		digitToLetter.put("1", "one");
		digitToLetter.put("2", "two");
		digitToLetter.put("3", "three");
		digitToLetter.put("4", "four");
		digitToLetter.put("5", "five");
		digitToLetter.put("6", "six");
		digitToLetter.put("7", "seven");
		digitToLetter.put("8", "eight");
		digitToLetter.put("9", "nine");
	}

	private static class Holder {
		private static final VelocityUtil INSTANCE = new VelocityUtil();
	}

	protected VelocityUtil(){}

	public static VelocityUtil getInstance() {
		return Holder.INSTANCE;
	}

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

	@RequestCost(increment = 5)
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

		String variableToReturn = variable;

		// starts with number
		if(variableToReturn.matches("^\\d.*")) {
			variableToReturn = replaceStartingNumberWithWrittenNumber(variableToReturn);
		}

		// start with char different than "_A-Za-z"
		if(variableToReturn.matches("[^_A-Za-z].*")) {
			variableToReturn = variableToReturn.replaceAll("[^_0-9A-Za-z]", "_");
		}

		if(variableToReturn.matches("[a-zA-Z].*")) {
			variableToReturn = (firstLetterUppercase)
					? StringUtils.camelCaseUpper(variableToReturn)
					: StringUtils.camelCaseLower(variableToReturn);
		}

		return variableToReturn;
	}

	/**
	 * Replace invalid tokens on the parameter "contextTokenIdentifier"  by allowed tokens for the velocity context identifier
	 * For instance, the tokens / and : are not allowed, so they are replaced by -
	 *
	 * so if you need to add a key into the velocity context that might have invalid token, you can do
	 *
	 * velocityContext.put(VelocityUtil.escapeContextTokenIdentifier(myWeirdKey), value)
	 *
	 * @param contextTokenIdentifier String
	 * @return String
	 */
	public static String escapeContextTokenIdentifier(final String contextTokenIdentifier) {

		final String escapeToken = UtilMethods.isSet(contextTokenIdentifier)?
				org.apache.commons.lang3.StringUtils.replace(contextTokenIdentifier, StringPool.COLON, StringPool.DASH):StringPool.BLANK;

		return org.apache.commons.lang3.StringUtils.replace(escapeToken, StringPool.FORWARD_SLASH, StringPool.DASH);
	}

	@VisibleForTesting
	static String replaceStartingNumberWithWrittenNumber(final String string) {

		final String subString = string.substring(0, 1);

		if(!subString.matches("[0-9]")) {
			return string;
		}

		return digitToLetter.get(subString) + string.substring(1);

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
	 *
	 * @deprecated Use the mockable version instead {@link VelocityUtil#getContext(HttpServletRequest, HttpServletResponse)}
	 */
	@Deprecated()
	public static ChainedContext getWebContext(HttpServletRequest request, HttpServletResponse response) {
		return getWebContext(getBasicContext(), request, response);
	}

	public ChainedContext getContext(final HttpServletRequest request, final HttpServletResponse response) {
		return getWebContext(getBasicContext(), request, response);
	}
	
  /**
   * This will return a velocity context for workflow actionlet.
   * It will mock a Request and Response and then use
   */
  public Context getWorkflowContext(final WorkflowProcessor processor) {
    
    final Contentlet contentlet = processor.getContentlet();
    final ContentType contentType = contentlet.getContentType();
    final Host host =  Try
        .of(() -> Host.SYSTEM_HOST.equals(contentlet.getHost()) || null == contentlet.getHost() 
        ? APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)
            : APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false))
        .getOrElse(APILocator.systemHost());
    
    final HttpServletRequest requestProxy= (HttpServletRequestThreadLocal.INSTANCE.getRequest() != null) ? HttpServletRequestThreadLocal.INSTANCE.getRequest()
        : new FakeHttpRequest(host.getHostname(), null).request();
    
    
    
    
    final HttpServletResponse responseProxy = new BaseResponse().response();


    
    
    Context context = getWebContext(VelocityUtil.getBasicContext(), requestProxy, responseProxy);
    context.put("host", host);
    context.put("contentType", contentType);
    context.put("host_id", host.getIdentifier());
    context.put("user", processor.getUser());
    context.put("company", APILocator.getCompanyAPI().getDefaultCompany());
    context.put("workflow", processor);
    context.put("actionName", processor.getAction().getName());
    context.put("stepName", processor.getStep().getName());
    context.put("stepId", processor.getStep().getId());
    context.put("nextAssign", processor.getNextAssign().getName());
    context.put("workflowMessage", processor.getWorkflowMessage());
    context.put("nextStepResolved", processor.getNextStep().isResolved());
    context.put("nextStepId", processor.getNextStep().getId());
    context.put("nextStepName", processor.getNextStep().getName());
    context.put("ipAddress", requestProxy.getRemoteAddr());
    
    // set the link to the contentlet
    if(UtilMethods.isSet(contentlet.getInode())) {
      final Company company = APILocator.getCompanyAPI().getDefaultCompany();
      if(UtilMethods.isSet(contentlet.getInode())) {
        context.put("linkToContent", company.getPortalURL() + "/dotAdmin/#/c/content/" + contentlet.getInode());
      }
    }
    
    
    
    
    if (UtilMethods.isSet(processor.getTask())) {
      context.put("workflowTask", processor.getTask());
      context.put("workflowTaskTitle",
          UtilMethods.isSet(processor.getTask().getTitle()) ? processor.getTask().getTitle() : processor.getContentlet().getTitle());
      context.put("modDate", processor.getTask().getModDate());
    } else {
      context.put("workflowTaskTitle", processor.getContentlet().getTitle());
      context.put("modDate", processor.getContentlet().getModDate());
    }
    context.put("contentTypeName", processor.getContentlet().getContentType().name());
    context.put("content", contentlet);
    context.put("contentlet", contentlet);
    context.put("contentMap", new ContentMap(contentlet, processor.getUser(),PageMode.PREVIEW_MODE,host,context));
    return context;
   }
 

  public ChainedContext getWorkflowContext(final HttpServletRequest request, final HttpServletResponse response,
      final WorkflowProcessor processor) {

    return getWebContext(getBasicContext(), request, response);
  }

	public static ChainedContext getWebContext(Context ctx, final HttpServletRequest requestIn, HttpServletResponse response) {


        final VelocityRequestWrapper request = VelocityRequestWrapper.wrapVelocityRequest(requestIn );


        if ( request.getAttribute( com.dotcms.rendering.velocity.Constants.VELOCITY_CONTEXT ) != null && request.getAttribute( com.dotcms.rendering.velocity.Constants.VELOCITY_CONTEXT ) instanceof ChainedContext ) {
            return (ChainedContext) request.getAttribute( com.dotcms.rendering.velocity.Constants.VELOCITY_CONTEXT  );
        } 
        
        if ( request.getAttribute( "User-Agent" ) != null && request.getAttribute( "User-Agent" ).equals( Constants.USER_AGENT_DOTCMS_BROWSER ) ) {
            request.setCustomUserAgentHeader( Constants.USER_AGENT_DOTCMS_BROWSER );
        }
        
        final ChainedContext context = new ChainedContext( ctx== null ? getBasicContext() : ctx , getEngine(), request, response );
    
        
        context.put("context", context);

		/*
		 * if we have a toolbox manager, get a toolbox from it See
		 * /WEB-INF/toolbox.xml
		 */
		
		context.setToolbox(getToolboxManager().getToolboxContext(context));

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
        }
        if (request != null && request.getAttribute(WebKeys.WIKI_CONTENTLET) != null) {
            final String urlMapId = (request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE) != null)
                    ? (String) request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE)
                    : (String) request.getAttribute(WebKeys.WIKI_CONTENTLET);
            ContentTool tool = new ContentTool();
            tool.init(context);
            ContentMap cMap = tool.find(urlMapId);
            context.put("URLMapContent", cMap);
            if (session != null && request.getAttribute(WebKeys.WIKI_CONTENTLET_URL) != null) {
                session.setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, request.getAttribute(WebKeys.WIKI_CONTENTLET_URL));
            }
        }
		
		
		

		// put the list of languages on the page
		context.put("languages", getLanguages());
		
		if(!UtilMethods.isSet(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)) && session!=null) {
		    context.put("language", (String) session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE));
		}else {
		    context.put("language", request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE));
		}
		

		Host host= WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
		context.put("host", host);

		context.put("pdfExport", false);
        context.put("dotPageMode", PageMode.get(request));


		User user = PortalUtil.getUser(request);
		context.put("user", user);

		Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request, false);
		if(visitor.isPresent()){
		    context.put("visitor", visitor.get());
		}
		
		return context;

	}

	public String  merge(final String templatePath, final Context ctx) {
		try {
			return mergeTemplate(templatePath, ctx);
		} catch (ResourceNotFoundException | ParseErrorException e) {
			Logger.error(ContainerResource.class, e.getMessage());
			throw e;
		} catch (Exception e) {
			Logger.error(ContainerResource.class, e.getMessage());
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * Merge a velocity resource
	 *
	 * @param templatePath
	 * @param ctx
	 * @return
	 * @throws ResourceNotFoundException
	 * @throws ParseErrorException
	 * @throws Exception
	 *
	 * @deprecated Use the mockable version instead {@link VelocityUtil#merge(String, Context)}
	 */
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
	

	private static List<Language> getLanguages(){
		return APILocator.getLanguageAPI().getLanguages();
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
    public static boolean shouldPageCache(final HttpServletRequest request, final IHTMLPage page)
            throws DotDataException, DotSecurityException {

        if (page == null || page.getCacheTTL() < 1) {
            return false;
        }
        // don't cache posts
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod()) ) {
            return false;
        }
        // nocache passed either as a session var, as a request var or as a
        // request attribute
        if (NO.equals(request.getParameter(DOTCACHE))
                || REFRESH.equals(request.getParameter(DOTCACHE))
                || NO.equals(request.getAttribute(DOTCACHE))
                || (request.getSession(false) != null && NO.equals(request.getSession(true).getAttribute(DOTCACHE)))
				|| (request.getSession(false) != null && REFRESH.equals(request.getSession(true).getAttribute(DOTCACHE))) ) {
            return false;
        }



        return true;
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
		List<DisplayedLanguage> languages = new ArrayList<>();
		List<DisplayedLanguage> allDisplayLanguages = new ArrayList<>();

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

					if(APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage()) {
						allDisplayLanguages.add(new DisplayedLanguage(language, true));
					}

					continue;
				}
			}
			if(language.getId()==APILocator.getLanguageAPI().getDefaultLanguage().getId()) {
				doesContentHaveDefaultLang = true;
			}

			languages.add(new DisplayedLanguage(language, false));

			if(APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage()) {
				allDisplayLanguages.add(new DisplayedLanguage(language, false));
			}
		}

		if(APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage() && doesContentHaveDefaultLang){
			return allDisplayLanguages;
		}

		return languages;
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

	/**
	 * Gets the value of the DONT_USE_DIRECTIVE_CACHE from the context.
	 * If the value is a Boolean, it returns it as is.
	 * If the value is a String, it parses it to a Boolean.
	 * If the value is not set or not a Boolean or String, it returns false.
	 *
	 * @param context The context from which to retrieve the value.
	 * @return true if the directive cache should not be used, false otherwise.
	 */
	public static boolean getDontUseDirectiveCache(Context context) {
		final Object dontCacheObj = context.get(DotCacheDirective.DONT_USE_DIRECTIVE_CACHE);
		if (dontCacheObj instanceof Boolean) {
			return (Boolean) dontCacheObj;
		} else if (dontCacheObj instanceof String) {
			return Boolean.parseBoolean((String) dontCacheObj);
		}
		return false;
	}


}
