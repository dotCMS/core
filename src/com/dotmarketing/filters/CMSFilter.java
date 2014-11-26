package com.dotmarketing.filters;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
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

import org.apache.commons.logging.LogFactory;

import com.dotcms.repackage.org.apache.commons.lang.StringEscapeUtils;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.owasp.esapi.errors.EncodingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.JBossRulesUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.util.FileUtil;
import com.liferay.util.Xss;

public class CMSFilter implements Filter {

	public void destroy() {

	}

	String ASSET_PATH = null;

	CmsUrlUtil urlUtil = CmsUrlUtil.getInstance();

	enum IAm{
		PAGE,
		FOLDER,
		FILE,
		VANITY_URL,
		NOTHING_IN_THE_CMS
	}
	
	
	
	
	public static final String CMS_FILTER_IDENTITY = "CMS_FILTER_IDENTITY";
	public static final String CMS_FILTER_URI_OVERRIDE = "CMS_FILTER_URLMAP_OVERRIDE";

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		HttpSession session = request.getSession(false);

		final String uri = (request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null) ? (String) request.getAttribute(CMS_FILTER_URI_OVERRIDE)
				: URLDecoder.decode(request.getRequestURI(), "UTF-8");

		String rewrite = null;
		IAm iAm = IAm.NOTHING_IN_THE_CMS;
		
		/*
		 * Here is a list of directories that we will ignore b/c of legacy code
		 * and servlet mappings. This is a mess and should be much cleaner
		 */
		if (Xss.URIHasXSS(uri)) {
			try {
				rewrite = Xss.encodeForURL(uri);
			} catch (EncodingException e) {
				Logger.error(this, "Encoding failure. Unable to encode URI " + uri);
				throw new ServletException(e.getMessage(), e);
			}

			response.sendRedirect(rewrite);
			return;
		}

		if (request.getQueryString() != null && !UtilMethods.decodeURL(request.getQueryString()).equals(null)) {
			// http://jira.dotmarketing.net/browse/DOTCMS-6141
			if (request.getQueryString() != null && request.getQueryString().contains("\"")) {
				response.sendRedirect(uri + "?" + StringEscapeUtils.escapeHtml(StringEscapeUtils.unescapeHtml(request.getQueryString())));
				return;
			}
			if (Xss.ParamsHaveXSS(request)) {
				response.sendRedirect(uri);
				return;
			}
		}
		
		// if (excludeURI(uri)) {
		// chain.doFilter(request, response);
		// return;
		// }

		// set the preview mode
		boolean ADMIN_MODE = false;

		LogFactory.getLog(this.getClass()).debug("CMS Filter URI = " + uri);

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

			if (request.getParameter("livePage") != null && request.getParameter("livePage").equals("1")) {

				session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
				request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
				session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
				request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
				LogFactory.getLog(this.getClass()).debug("CMS FILTER Cleaning PREVIEW_MODE_SESSION LIVE!!!!");

			}

			if (request.getParameter("previewPage") != null && request.getParameter("previewPage").equals("1")) {

				session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
				request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
				session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
				request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
				LogFactory.getLog(this.getClass()).debug("CMS FILTER Cleaning EDIT_MODE_SESSION PREVIEW!!!!");
			}

			if (request.getParameter("previewPage") != null && request.getParameter("previewPage").equals("2")) {

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
		} catch (Exception e) {
			Logger.error(this, "Unable to retrieve current request host for URI " + uri);
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

		// get the users language
		long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();



		if (urlUtil.isFileAsset(uri, host, languageId)) {
			iAm= IAm.FILE;
		} else if (urlUtil.isVanityUrl(uri, host)) {
			iAm = IAm.VANITY_URL;
		} else if (urlUtil.isPageAsset(uri, host, languageId)) {
			iAm = IAm.PAGE;
		} else if (urlUtil.isFolder(uri, host)) {
			iAm = IAm.FOLDER;
		}


		// Checking if host is active

		boolean hostlive;
		try {
			hostlive = APILocator.getVersionableAPI().hasLiveVersion(host);
		} catch (Exception e1) {
			closeDbSilently();
			throw new ServletException(e1);
		}
		if (!ADMIN_MODE && !hostlive) {
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

		// if a vanity URL
		if (iAm == IAm.VANITY_URL) {
			rewrite = VirtualLinksCache.getPathFromCache(host.getHostname() + ":" + ("/".equals(uri) ? "/cmsHomePage" : uri));
			if (!UtilMethods.isSet(rewrite)) {
				rewrite = VirtualLinksCache.getPathFromCache(("/".equals(uri) ? "/cmsHomePage" : uri));
			}
			if (UtilMethods.isSet(rewrite) && rewrite.indexOf("//") > -1) {
				response.sendRedirect(rewrite);
				closeDbSilently();
				return;
			}
			if (UtilMethods.isSet(rewrite)) {
				if (urlUtil.isFileAsset(rewrite, host, languageId)) {
					iAm= IAm.FILE;
				} else if (urlUtil.isPageAsset(rewrite, host, languageId)) {
					iAm = IAm.PAGE;
				} else if (urlUtil.isFolder(rewrite, host)) {
					iAm = IAm.FOLDER;
				}
			}
		}

		if (iAm == IAm.FOLDER) {
			if (!uri.endsWith("/")) {
				response.sendRedirect(uri + "/");
				closeDbSilently();
				return;
			} else {
				rewrite = uri + Config.getStringProperty("CMS_INDEX_PAGE", "index.html");
				iAm = IAm.PAGE;
			}
		}

		// if we are not rewriting anything, use the uri
		rewrite = (rewrite == null) ? uri : rewrite;

		if (iAm == IAm.FILE) {
			Identifier ident = null;
			try {
				ident = APILocator.getIdentifierAPI().find(host, rewrite);
				request.setAttribute(CMS_FILTER_IDENTITY, ident);
				request.getRequestDispatcher("/dotAsset/").forward(request, response);
			} catch (DotDataException e) {
				Logger.error(CMSFilter.class, e.getMessage(), e);
				throw new IOException(e.getMessage());
			}
			return;
		}

		if (iAm == IAm.PAGE) {

			// JBOSS RULEZ only if a page
			JBossRulesUtils.checkObjectRulesFromXML(request);

			request.setAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE, rewrite);
			// Serving a page through the velocity servlet
			request.getRequestDispatcher("/servlets/VelocityServlet").forward(request, response);
			return;
		}

		// otherwise, pass
		chain.doFilter(req, res);
		
		

	}

	
	
	
	public void init(FilterConfig config) throws ServletException {

		ASSET_PATH = APILocator.getFileAPI().getRelativeAssetsRootPath();

	}

	private static Set<String> excludeList = null;
	private static final Integer mutex = new Integer(0);

	private static void buildExcludeList() {
		synchronized (mutex) {
			if (excludeList != null)
				return;

			Set<String> set = new HashSet<String>();

			// allow servlets to be called without a 404
			set.add("^/servlet/");
			set.add("^/servlets/");
			// Load some defaults
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
			// http://jira.dotmarketing.net/browse/DOTCMS-2178
			set.add("^/contentAsset/");
			// http://jira.dotmarketing.net/browse/DOTCMS-6753
			set.add("^/JSONTags/");
			set.add("^/spring/");
			set.add("^/api/");
			set.add("^/DOTLESS/");
			set.add("^/DOTSASS/");

			// Load exclusions from plugins
			PluginAPI pAPI = APILocator.getPluginAPI();
			List<String> pluginList = pAPI.getDeployedPluginOrder();
			if (pluginList != null) {
				for (String pluginID : pluginList) {
					try {
						String list = pAPI.loadPluginConfigProperty(pluginID, "cmsfilter.servlet.exclusions");
						Logger.info(CMSFilter.class, "plugin " + pluginID + " cmsfilter.servlet.exclusions=" + list);
						if (list != null) {
							String[] items = list.split(",");
							if (items != null && items.length > 0) {
								for (String item : items) {
									item = item.trim();
									if (UtilMethods.isSet(item) && !set.contains(item)) {
										set.add(item);
									}
								}
							}
						}
					} catch (DotDataException e) {
						Logger.debug(CMSFilter.class, "DotDataException: " + e.getMessage(), e);
					}

				}
			}
			excludeList = set;
		}
	}

	public static void addExclude(String URLPattern) {
		if (excludeList == null) {
			buildExcludeList();
		}
		synchronized (excludeList) {
			excludeList.add(URLPattern);
		}
	}

	public static void removeExclude(String URLPattern) {
		if (excludeList != null) {
			synchronized (excludeList) {
				excludeList.remove(URLPattern);
			}
		}
	}

	public static boolean excludeURI(String uri) {

		if (uri.trim().equals("/c") || uri.endsWith(".php") || uri.trim().startsWith("/c/")
				|| (uri.indexOf("/ajaxfileupload/upload") != -1)) {
			return true;
		}

		if (excludeList == null)
			buildExcludeList();

		if (excludeList.contains(uri))
			return true;

		for (String exclusion : excludeList) {
			if (RegEX.contains(uri, exclusion)) {
				return true;
			}
		}

		// finally, if we have the file, serve it
		if (!"/".equals(uri)) {
			File f = new File(FileUtil.getRealPath(uri));
			if (f.exists()) {
				return true;

			}
		}

		return false;
	}

	private void closeDbSilently() {
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) {

		} finally {
			try {
				DbConnectionFactory.closeConnection();
			} catch (Exception e) {

			}
		}

	}

}
