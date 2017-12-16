package com.dotcms.rendering.velocity.servlet;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.velocity.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.RequestWrapper;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BlockPageCache;
import com.dotmarketing.business.BlockPageCache.PageCacheParameters;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;


import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

public class VelocityLiveServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private static VisitorAPI visitorAPI = APILocator.getVisitorAPI();
    private String CHARSET = "UTF-8";
    private final String VELOCITY_HTMLPAGE_EXTENSION = "dotpage2";


    @Override
    @CloseDBIfOpened
    protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        final String uri = URLDecoder.decode((req.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE) != null)
                ? (String) req.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE)
                : req.getRequestURI(), "UTF-8");



        RequestWrapper request = new RequestWrapper(req);
        request.setRequestUri(uri);


        Host host = hostWebAPI.getCurrentHostNoThrow(request);


        // Checking if host is active
        boolean hostlive;


        try {
            hostlive = APILocator.getVersionableAPI()
                .hasLiveVersion(host);
        } catch (Exception e1) {
            UtilMethods.closeDbSilently();
            throw new ServletException(e1);
        }
        if (!hostlive) {
            try {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        LanguageUtil.get("server-unavailable-error-message"));
            } catch (LanguageException e) {
                Logger.error(CMSFilter.class, e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            return;
        }


        if (DbConnectionFactory.isMsSql() && LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            request.getRequestDispatcher("/portal/no_license.jsp")
                .forward(request, response);
            return;
        }

        if (DbConnectionFactory.isOracle() && LicenseUtil.getLevel() < LicenseLevel.PRIME.level) {
            request.getRequestDispatcher("/portal/no_license.jsp")
                .forward(request, response);
            return;
        }
        if (!LicenseUtil.isASAllowed()) {
            request.getRequestDispatcher("/portal/no_license.jsp")
                .forward(request, response);
            return;

        }

        try {


            if (uri == null) {
                response.sendError(500, "VelocityServlet called without running through the CMS Filter");
                Logger.error(this.getClass(),
                        "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  "
                                + Constants.CMS_FILTER_URI_OVERRIDE);
                return;
            }


            // we will always need a visitor in admin mode
            if (PageMode.get(request).isAdmin) {
                visitorAPI.getVisitor(request, true);
            }
            LanguageWebAPI langWebAPI = WebAPILocator.getLanguageWebAPI();
            langWebAPI.checkSessionLocale(request);

            doLiveMode(request, response);


        } catch (ResourceNotFoundException rnfe) {
            Logger.error(this, "ResourceNotFoundException" + rnfe.toString(), rnfe);
            response.sendError(404);
            return;
        } catch (ParseErrorException pee) {
            Logger.error(this, "Template Parse Exception : " + pee.toString(), pee);
            response.sendError(500, "Template Parse Exception");
        } catch (MethodInvocationException mie) {
            Logger.error(this, "MethodInvocationException" + mie.toString(), mie);
            response.sendError(500, "MethodInvocationException Error on template");
        } catch (Exception e) {
            Logger.error(this, "Exception" + e.toString(), e);
            response.sendError(500, "Exception Error on template");

        } finally {
            DbConnectionFactory.closeSilently();
        }

    }

    public void init(ServletConfig config) throws ServletException {

        Logger.info(this.getClass(), "VelocityLiveServlet");
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
            Host host = hostWebAPI.getCurrentHostNoThrow(request);


            // Find the current language
            long currentLanguageId = VelocityUtil.getLanguageId(request);


            // now we check identifier cache first (which DOES NOT have a 404 cache )
            Identifier ident = APILocator.getIdentifierAPI()
                .find(host, uri);
            if (ident == null || ident.getId() == null) {
                throw new ResourceNotFoundException(String.format("Resource %s not found in Live mode!", uri));
            }
            response.setContentType(CHARSET);
            request.setAttribute("idInode", String.valueOf(ident.getId()));
            Logger.debug(VelocityLiveServlet.class, "VELOCITY HTML INODE=" + ident.getId());

            Optional<Visitor> visitor = visitorAPI.getVisitor(request);

            boolean newVisitor = false;
            boolean newVisit = false;

            /*
             * JIRA http://jira.dotmarketing.net/browse/DOTCMS-4659 //Set long lived cookie
             * regardless of who this is
             */
            String _dotCMSID =
                    UtilMethods.getCookieValue(request.getCookies(), com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

            if (!UtilMethods.isSet(_dotCMSID)) {
                // create unique generator engine
                Cookie idCookie = CookieUtil.createCookie();
                _dotCMSID = idCookie.getValue();
                response.addCookie(idCookie);
                newVisitor = true;

                if (visitor.isPresent()) {
                    visitor.get()
                        .setDmid(UUID.fromString(_dotCMSID));
                }

            }

            String _oncePerVisitCookie = UtilMethods.getCookieValue(request.getCookies(), WebKeys.ONCE_PER_VISIT_COOKIE);

            if (!UtilMethods.isSet(_oncePerVisitCookie)) {
                newVisit = true;
            }

            if (newVisitor) {
                RulesEngine.fireRules(request, response, Rule.FireOn.ONCE_PER_VISITOR);
                if (response.isCommitted()) {
                    /*
                     * Some form of redirect, error, or the request has already been fulfilled in
                     * some fashion by one or more of the actionlets.
                     */
                    Logger.debug(VelocityLiveServlet.class, "A ONCE_PER_VISITOR RuleEngine Action has committed the response.");
                    return;
                }
            }

            if (newVisit) {
                RulesEngine.fireRules(request, response, Rule.FireOn.ONCE_PER_VISIT);
                if (response.isCommitted()) {
                    /*
                     * Some form of redirect, error, or the request has already been fulfilled in
                     * some fashion by one or more of the actionlets.
                     */
                    Logger.debug(VelocityLiveServlet.class, "A ONCE_PER_VISIT RuleEngine Action has committed the response.");
                    return;
                }
            }

            RulesEngine.fireRules(request, response, Rule.FireOn.EVERY_PAGE);

            if (response.isCommitted()) {
                /*
                 * Some form of redirect, error, or the request has already been fulfilled in some
                 * fashion by one or more of the actionlets.
                 */
                Logger.debug(VelocityLiveServlet.class, "An EVERY_PAGE RuleEngine Action has committed the response.");
                return;
            }


            com.liferay.portal.model.User user = null;
            HttpSession session = request.getSession(false);
            try {
                if (session != null)
                    user = (com.liferay.portal.model.User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
            } catch (Exception nsue) {
                Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
            }

            Logger.debug(VelocityLiveServlet.class, "Page Permissions for URI=" + uri);


            IHTMLPage page;

            try {
                page = VelocityUtil.getPage(ident, request, true, null);
            } catch (DotDataException e) {
                Logger.info(VelocityLiveServlet.class, "Unable to find live version of page. Identifier: " + ident.getId());
                throw new ResourceNotFoundException(String.format("Resource %s not found in Live mode!", uri));
            }

            // Verify and handle the case for unauthorized access of this contentlet
            Boolean unauthorized = CMSUrlUtil.getInstance()
                .isUnauthorizedAndHandleError(page, uri, user, request, response);
            if (unauthorized) {
                return;
            }

            // Fire the page rules until we know we have permission.
            RulesEngine.fireRules(request, response, page, Rule.FireOn.EVERY_PAGE);

            Logger.debug(VelocityLiveServlet.class, "Recording the ClickStream");
            if (Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
                if (user != null) {
                    UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI()
                        .getUserProxy(user, APILocator.getUserAPI()
                            .getSystemUser(), false);
                    if (!userProxy.isNoclicktracking()) {
                        ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response), host);
                    }
                } else {
                    ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response), host);
                }
            }

            // Begin page caching
            String userId = (user != null) ? user.getUserId()
                    : APILocator.getUserAPI()
                        .getAnonymousUser()
                        .getUserId();
            String language = String.valueOf(currentLanguageId);
            String urlMap = (String) request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);
            String queryString = request.getQueryString();
            String persona = null;
            Optional<Visitor> v = visitorAPI.getVisitor(request, false);
            if (v.isPresent() && v.get()
                .getPersona() != null) {
                persona = v.get()
                    .getPersona()
                    .getKeyTag();
            }


            PageCacheParameters cacheParameters =
                    new BlockPageCache.PageCacheParameters(userId, language, urlMap, queryString, persona);

            boolean buildCache = false;
            String key = VelocityUtil.getPageCacheKey(request, response);
            if (key != null) {
                String cachedPage = CacheLocator.getBlockPageCache()
                    .get(page, cacheParameters);
                if (cachedPage == null || "refresh".equals(request.getParameter("dotcache"))
                        || "refresh".equals(request.getAttribute("dotcache"))
                        || (request.getSession(false) != null && "refresh".equals(request.getSession(true)
                            .getAttribute("dotcache")))) {
                    // build cached response
                    buildCache = true;
                } else {
                    // have cached response and are not refreshing, send it
                    response.getWriter()
                        .write(cachedPage);
                    return;
                }
            }

            Writer out = (buildCache) ? new StringWriter(4096) : new VelocityFilterWriter(response.getWriter());
            // get the context from the requst if possible
            Context context = VelocityUtil.getWebContext(request, response);
            request.setAttribute("velocityContext", context);
            Logger.debug(VelocityLiveServlet.class, "HTMLPage Identifier:" + ident.getId());

            try {
                    VelocityUtil.getEngine()
                        .getTemplate("/live/" + ident.getId() + "_" + page.getLanguageId() + "." + VelocityType.HTMLPAGE.fileExtension)
                        .merge(context, out);
            } catch (Throwable e) {
                Logger.warn(this, "can't do live mode merge", e);
            }
            session = request.getSession(false);
            if (buildCache) {
                String trimmedPage = out.toString()
                    .trim();
                response.getWriter()
                    .write(trimmedPage);
                response.getWriter()
                    .close();
                synchronized (key.intern()) {
                    // CacheLocator.getHTMLPageCache().remove(page);
                    CacheLocator.getBlockPageCache()
                        .add(page, trimmedPage, cacheParameters);
                }
            } else {
                out.close();
            }
        } finally {
            LicenseUtil.stopLiveMode();
        }

    }

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
