package com.dotmarketing.filters;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.vanityurl.filters.VanityUrlRequestWrapper;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.*;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

public class CMSFilter implements Filter {

    private final HttpServletRequestThreadLocal  requestThreadLocal  = HttpServletRequestThreadLocal.INSTANCE;
    private final HttpServletResponseThreadLocal responseThreadLocal = HttpServletResponseThreadLocal.INSTANCE;
    private CMSUrlUtil urlUtil = CMSUrlUtil.getInstance();
    private static VisitorAPI visitorAPI = APILocator.getVisitorAPI();
    private final String RELATIVE_ASSET_PATH = APILocator.getFileAssetAPI().getRelativeAssetsRootPath();
    public static final String CMS_INDEX_PAGE = Config.getStringProperty("CMS_INDEX_PAGE", "index");

    public enum IAm {
        PAGE, FOLDER, FILE, NOTHING_IN_THE_CMS
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {

    }


    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        try {
            doFilterInternal(req, res, chain);
        } finally {
            DbConnectionFactory.closeSilently();
        }


    }

    private void doFilterInternal(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        IAm iAm = IAm.NOTHING_IN_THE_CMS;

        // Set the request in the thread local.
        this.requestThreadLocal.setRequest(request);
        this.responseThreadLocal.setResponse(response);

        // Get the URI and query string from the request
        String uri = urlUtil.getURIFromRequest(request);
        final boolean overriddenURI = urlUtil.wasURIOverridden(request);
        String queryString = urlUtil.getURLQueryStringFromRequest(request);

        // Check for possible XSS hacks
        String xssRedirect = urlUtil.xssCheck(uri, queryString);
        if (xssRedirect != null) {
            response.sendRedirect(xssRedirect);
            return;
        }

        LogFactory.getLog(this.getClass()).debug("CMS Filter URI = " + uri);


        Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        /*
         * If someone is trying to go right to an asset without going through the cms, give them a
         * 404
         */
        if (UtilMethods.isSet(RELATIVE_ASSET_PATH) && uri.startsWith(RELATIVE_ASSET_PATH)) {
            response.sendError(403, "Forbidden");
            return;
        }

        // Get the user language
        final long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

        iAm = this.urlUtil.resolveResourceType(iAm, uri, site, languageId);

        if (iAm == IAm.FOLDER) {

            // if we are not rewriting anything, use the uri
            if (!uri.endsWith("/")) {

                if (UtilMethods.isSet(queryString)) {
                    response.setHeader("Location", uri + "/?" + queryString);
                } else {
                    response.setHeader("Location", uri + "/");

                }

                /*
                 * At this point if the URI was overridden is probably because a VanityURL set it,
                 * and in that case we need to respect the status code set by the VanityURL.
                 */
                if (!overriddenURI) {
                    response.setStatus(301);
                }
                DbConnectionFactory.closeSilently();
                return;
            } else {
                uri = uri + CMS_INDEX_PAGE;
                if (urlUtil.isPageAsset(uri, site, languageId)) {
                    iAm = IAm.PAGE;
                }
            }
        }

        if (iAm == IAm.PAGE) {
            countPageVisit(request);
            countSiteVisit(request, response);
            request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE,
                    this.urlUtil.getUriWithoutQueryString(uri));
            queryString = (null == queryString)?
                    this.urlUtil.getQueryStringFromUri (uri):queryString;
        }

        // run rules engine for all requests
        RulesEngine.fireRules(request, response, Rule.FireOn.EVERY_REQUEST);

        //if we have committed the response, die
        if (response.isCommitted()) {
            return;
        }

        if (iAm == IAm.FILE) {
            Identifier ident;
            try {
                // Serving the file through the /dotAsset servlet
                StringWriter forward = new StringWriter();
                forward.append("/dotAsset/");

                ident = APILocator.getIdentifierAPI().find(site, uri);
                request.setAttribute(Constants.CMS_FILTER_IDENTITY, ident);

                // If language is in session, set as query string
                forward.append('?').append(WebKeys.HTMLPAGE_LANGUAGE + "=").append(String.valueOf(languageId));

                request.getRequestDispatcher(forward.toString()).forward(request, response);

            } catch (DotDataException e) {
                Logger.error(CMSFilter.class, e.getMessage(), e);
                throw new IOException(e.getMessage(),e);
            }
            return;
        }

        if (iAm == IAm.PAGE) {

            final StringWriter forward = new StringWriter().append("/servlets/VelocityServlet");

            if (UtilMethods.isSet(queryString)) {
                if (!queryString.contains(WebKeys.HTMLPAGE_LANGUAGE)) {
                    queryString = queryString + "&" + WebKeys.HTMLPAGE_LANGUAGE + "=" + languageId;
                }
                forward.append('?');
                forward.append(queryString);
            }
            request.getRequestDispatcher(forward.toString()).forward(request, response);
            return;
        }

        if (uri.startsWith("/contentAsset/")) {
            if (response.isCommitted()) {
                /*
                 * Some form of redirect, error, or the request has already been fulfilled in some
                 * fashion by one or more of the actionlets.
                 */
                return;
            }
        }
        
        // allow vanities to forward to a dA asset
        if(request instanceof VanityUrlRequestWrapper && !response.isCommitted() && (uri.startsWith("/dA/") || uri.startsWith("/contentAsset/")) ) {
            request.getRequestDispatcher(uri).forward(request, response);
            return;
        }


        chain.doFilter(req, res);

    }

    @Override
    public void destroy() {

    }

    private void countSiteVisit(HttpServletRequest request, HttpServletResponse response) {
        PageMode mode = PageMode.get(request);
        if (mode == PageMode.LIVE) {
            NumberOfTimeVisitedCounter.maybeCount(request, response);
        }
    }

    private void countPageVisit(HttpServletRequest request) {

        PageMode mode = PageMode.get(request);
        if (mode == PageMode.LIVE) {
            Optional<Visitor> visitor = visitorAPI.getVisitor(request);

            if (visitor.isPresent()) {
                visitor.get().addPagesViewed(request.getRequestURI());
            }
        }

    }



    @Deprecated
    public static void addExclude(String URLPattern) {

        // not needed anymore
    }

    @Deprecated
    public static void removeExclude(String URLPattern) {
        // not needed anymore
    }

}
