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
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.*;
import io.vavr.Tuple2;
import io.vavr.control.Try;

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

    /*
    * These enums are used to determine what the current request is.
    * It is used to determine if the request is a page, a file, a folder, or nothing in the CMS.
    * In some cases we have to know if the request is a page or if it is an index page. For example, if the request is
    * an UrlMapping and ends with slash, then it needs to be treated as a page and not as an index page. To achieve this
    * we use the IAmSubType enum.
    * */
    public enum IAm {
        PAGE, FOLDER, FILE, NOTHING_IN_THE_CMS
    }

    public enum IAmSubType {
        PAGE_INDEX, PAGE_URL_MAP, DEFAULT
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
        final Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

        // Set the request/response in the thread local.
        this.requestThreadLocal.setRequest(request);
        this.responseThreadLocal.setResponse(response);

        // run rules engine for all requests
        RulesEngine.fireRules(request, response, Rule.FireOn.EVERY_REQUEST);

        //if we have committed the response, die
        if (response.isCommitted()) {
            return;
        }
        
        
        // Get the URI and query string from the request
        String uri = urlUtil.getURIFromRequest(request);
        String queryString = urlUtil.getURLQueryStringFromRequest(request);


        Logger.debug(this.getClass(), "CMS Filter URI = " + uri);

        /*
         * If someone is trying to go right to an asset without going through the cms, give them a
         * 404
         */
        if (UtilMethods.isSet(RELATIVE_ASSET_PATH) && uri.startsWith(RELATIVE_ASSET_PATH)) {
            response.sendError(403, "Forbidden");
            return;
        }


        final Tuple2<IAm,IAmSubType> iAm  =
                this.urlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS, uri, site, languageId);

        // if I am a folder without a slash

        if (iAm._1() == IAm.FOLDER && !uri.endsWith("/")) {
            response.setHeader("Location", UtilMethods.isSet(queryString) ? uri + "/?" + queryString : uri + "/");
            Try.run(()->response.setStatus(301));
            return;
        }


        // if I am a Page with a trailing slash
        if (iAm._1() == IAm.PAGE && iAm._2() == IAmSubType.PAGE_INDEX && uri.endsWith("/")) {
            uri = uri + CMS_INDEX_PAGE;
        }


        if (iAm._1() == IAm.PAGE) {
            countPageVisit(request);
            countSiteVisit(request, response);
            request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE,
                    this.urlUtil.getUriWithoutQueryString(uri));
            queryString = (null == queryString)?
                    this.urlUtil.getQueryStringFromUri (uri):queryString;
        }

        if (iAm._1() == IAm.FILE) {
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

            } catch (Exception e) {
                Logger.warnAndDebug(CMSFilter.class, e.getMessage(), e);
                throw new IOException(e.getMessage(),e);
            }
            return;
        }

        if (iAm._1() == IAm.PAGE) {

            final StringWriter forward = new StringWriter().append("/servlets/VelocityServlet");

            if (UtilMethods.isSet(queryString)) {
                if (!queryString.contains(WebKeys.HTMLPAGE_LANGUAGE)) {
                    queryString = queryString + "&" + WebKeys.HTMLPAGE_LANGUAGE + "=" +
                            Try.of(()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId()).getOrElse(languageId);
                }
                forward.append('?');
                forward.append(queryString);
            }
            request.getRequestDispatcher(forward.toString()).forward(request, response);
            return;
        }

        // nothing to do here
        if (uri.startsWith("/contentAsset/") && response.isCommitted()) {
            return;
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
