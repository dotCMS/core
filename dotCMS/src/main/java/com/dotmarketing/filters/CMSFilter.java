package com.dotmarketing.filters;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Optional;
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

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.util.*;
import org.apache.commons.logging.LogFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.util.Xss;

public class CMSFilter implements Filter {

	private final HttpServletRequestThreadLocal requestThreadLocal =
			HttpServletRequestThreadLocal.INSTANCE;

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



	public static final String CMS_INDEX_PAGE = Config.getStringProperty("CMS_INDEX_PAGE", "index");
	public static final String CMS_FILTER_IDENTITY = "CMS_FILTER_IDENTITY";
	public static final String CMS_FILTER_URI_OVERRIDE = "CMS_FILTER_URLMAP_OVERRIDE";

	private static VisitorAPI visitorAPI = APILocator.getVisitorAPI();

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		// set the request in the threadlocal.
		this.requestThreadLocal.setRequest(request);

		final String uri = (request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null) ? (String) request.getAttribute(CMS_FILTER_URI_OVERRIDE)
				: URLDecoder.decode(request.getRequestURI(), "UTF-8");

		String xssRedirect = xssCheck(uri, request.getQueryString());
		if(xssRedirect!=null){
			response.sendRedirect(xssRedirect);
			return;
		}



		IAm iAm = IAm.NOTHING_IN_THE_CMS;

		LogFactory.getLog(this.getClass()).debug("CMS Filter URI = " + uri);




		/*
		 * Getting host object form the session
		 */
		HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
		Host host;
		try {
			host = hostWebAPI.getCurrentHost(request);
			request.setAttribute("host", host);
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

		String rewrite = null;
		String queryString = request.getQueryString();
		// if a vanity URL
		if (iAm == IAm.VANITY_URL) {

			rewrite = VirtualLinksCache.getPathFromCache(host.getHostname() + ":" + ("/".equals(uri) ? "/cmsHomePage" : uri.endsWith("/")?uri.substring(0, uri.length() - 1):uri));

			if (!UtilMethods.isSet(rewrite)) {
				rewrite = VirtualLinksCache.getPathFromCache(("/".equals(uri) ? "/cmsHomePage" : uri.endsWith("/")?uri.substring(0, uri.length() - 1):uri));
			}
			if (UtilMethods.isSet(rewrite) && rewrite.contains("//")) {
				response.sendRedirect(rewrite);

				closeDbSilently();
				return;
			}
			if (UtilMethods.isSet(rewrite)) {
				if(rewrite!=null && rewrite.contains("?")){
					String[] arr = rewrite.split("\\?",2);
					rewrite = arr[0];
					if(arr.length>1){
						queryString= arr[1];
					}
				}
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
				// If the value comes from the uri override attribute, used it, if not use the same uri as the
				// current request, no decoding needed.
				String undecodeUri = (request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null) ? (String) request.getAttribute(CMS_FILTER_URI_OVERRIDE)
						: request.getRequestURI();
				if(UtilMethods.isSet(queryString)){
					response.setHeader("Location", undecodeUri + "/?" + queryString );
				}
				else{
					response.setHeader("Location", undecodeUri +"/" );

				}
				response.setStatus(301);
				closeDbSilently();
				return;
			} else {
				if(UtilMethods.isSet(rewrite)) {

					rewrite = rewrite + CMS_INDEX_PAGE;
				} else {
					rewrite = uri + CMS_INDEX_PAGE;
				}
				if(urlUtil.isPageAsset(rewrite, host, languageId)){
					iAm = IAm.PAGE;
				}
			}
		}

		if(iAm == IAm.PAGE){
			countPageVisit(request);
			countSiteVisit(request, response);
		}

		// if we are not rewriting anything, use the uri
		rewrite = (rewrite == null) ? uri : rewrite;


		if (iAm == IAm.FILE) {
			Identifier ident = null;
			try {
				//Serving the file through the /dotAsset servlet
				StringWriter forward = new StringWriter();
				forward.append("/dotAsset/");
				
				ident = APILocator.getIdentifierAPI().find(host, rewrite);
				request.setAttribute(CMS_FILTER_IDENTITY, ident);
				RulesEngine.fireRules(request, response, Rule.FireOn.EVERY_REQUEST);
				if(response.isCommitted()) {
                /* Some form of redirect, error, or the request has already been fulfilled in some fashion by one or more of the actionlets. */
					return;
				}
				
				//If language is in session, set as query string
				if(UtilMethods.isSet(languageId)){
					forward.append('?');
					forward.append(WebKeys.HTMLPAGE_LANGUAGE + "=" + languageId);
				}
				request.getRequestDispatcher(forward.toString()).forward(request, response);
				
			} catch (DotDataException e) {
				Logger.error(CMSFilter.class, e.getMessage(), e);
				throw new IOException(e.getMessage());
			}
            return;
		}

		if (iAm == IAm.PAGE) {
			request.setAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE, rewrite);

			// Serving a page through the velocity servlet
			StringWriter forward = new StringWriter();
			forward.append("/servlets/VelocityServlet");

			if(UtilMethods.isSet(queryString)){
				if(queryString.indexOf(WebKeys.HTMLPAGE_LANGUAGE)==-1) {
					queryString = queryString + "&" + WebKeys.HTMLPAGE_LANGUAGE + "=" + languageId;
				}
				forward.append('?');
				forward.append(queryString);
			}

			// fire every_request rules
            RulesEngine.fireRules(request, response, Rule.FireOn.EVERY_REQUEST);
            if(response.isCommitted()){
                /* Some form of redirect, error, or the request has already been fulfilled in some fashion by one or more of the actionlets. */
                return;
            }
			request.getRequestDispatcher(forward.toString()).forward(request, response);
			return;
		}

		if(rewrite.startsWith("/contentAsset/")){
	        RulesEngine.fireRules(request, response, Rule.FireOn.EVERY_REQUEST);
	        if(response.isCommitted()){
	            /* Some form of redirect, error, or the request has already been fulfilled in some fashion by one or more of the actionlets. */
	            return;
	        }
		}

		// otherwise, pass
		chain.doFilter(req, res);

	}

	private void countSiteVisit(HttpServletRequest request,  HttpServletResponse response) {

		HttpSession session = request.getSession(false);
		boolean PAGE_MODE = true;

		if(session != null){
			PAGE_MODE = PageRequestModeUtil.isPageMode(session);
		}

		if (PAGE_MODE) {
			NumberOfTimeVisitedCounter.maybeCount(request, response);

		}
	}

	private void countPageVisit(HttpServletRequest request) {

		HttpSession session = request.getSession(false);
		boolean PAGE_MODE = true;

		if(session != null){
			PAGE_MODE = PageRequestModeUtil.isPageMode(session);
		}

		if (PAGE_MODE) {
			Optional<Visitor> visitor = visitorAPI.getVisitor(request);

			if (visitor.isPresent()) {
				visitor.get().addPagesViewed(request.getRequestURI());
			}

		}
	}


	public void init(FilterConfig config) throws ServletException {
		this.ASSET_PATH = APILocator.getFileAPI().getRelativeAssetsRootPath();
	}
	@Deprecated
	private static Set<String> excludeList = null;
	@Deprecated
	private static final Integer mutex = new Integer(0);


	@Deprecated
	private static void buildExcludeList() {
		// not needed anymore
	}

	@Deprecated
	public static void addExclude(String URLPattern) {

		// not needed anymore
	}

	@Deprecated
	public static void removeExclude(String URLPattern) {
		// not needed anymore
	}

	public static boolean excludeURI(String uri) {

		return true;
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

	private String xssCheck(String uri, String queryString) throws ServletException{

		String rewrite=null;
		if (Xss.URIHasXSS(uri)) {
			Logger.warn(this, "XSS Found in request URI: " +uri );
			try {
				rewrite = Xss.encodeForURL(uri);
			} catch (Exception e) {
				Logger.error(this, "Encoding failure. Unable to encode URI " + uri);
				throw new ServletException(e.getMessage(), e);
			}
		}

		else if (queryString != null && !UtilMethods.decodeURL(queryString).equals(null)) {
			if (Xss.ParamsHaveXSS(queryString)) {
				Logger.warn(this, "XSS Found in Query String: " +queryString);
				rewrite=uri;
			}
		}

		return rewrite;
	}



}
