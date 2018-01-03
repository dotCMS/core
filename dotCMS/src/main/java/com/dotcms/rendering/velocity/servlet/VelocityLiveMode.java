package com.dotcms.rendering.velocity.servlet;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.visitor.domain.Visitor;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BlockPageCache;
import com.dotmarketing.business.BlockPageCache.PageCacheParameters;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;

import com.liferay.portal.model.User;

public class VelocityLiveMode extends VelocityModeHandler {



    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private static final PageMode mode = PageMode.LIVE;
    private final String uri;
    private final Host host;


    
    
    public VelocityLiveMode(HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        this.request = request;
        this.response = response;
        this.uri = uri;
        this.host = host;
    }

    public VelocityLiveMode(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }


    @Override
    public final void serve() throws DotDataException, IOException, DotSecurityException {

        serve(response.getWriter());

    }
    @Override
    public final void serve(final Writer out) throws DotDataException, IOException, DotSecurityException {

        LicenseUtil.startLiveMode();
        try {

            // Find the current language
            long langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();


            // now we check identifier cache first (which DOES NOT have a 404 cache )
            Identifier id = APILocator.getIdentifierAPI().find(host, uri);
            if (id == null || id.getId() == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else {
                request.setAttribute("idInode", id);
            }

            response.setContentType(CHARSET);


            RulesEngine.fireRules(request, response);

            if (response.isCommitted()) {
                /*
                 * Some form of redirect, error, or the request has already been fulfilled in some
                 * fashion by one or more of the actionlets.
                 */
                Logger.debug(this.getClass(), "An EVERY_PAGE RuleEngine Action has committed the response.");
                return;
            }


            User user = null;
            HttpSession session = request.getSession(false);

            if (session != null) {
                user = (User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
            }

            Logger.debug(this.getClass(), "Page Permissions for URI=" + uri);



            IHTMLPage htmlPage = VelocityUtil.getPage(id, langId, mode.showLive);


            // Verify and handle the case for unauthorized access of this contentlet
            Boolean unauthorized = CMSUrlUtil.getInstance().isUnauthorizedAndHandleError(htmlPage, uri, user, request, response);
            if (unauthorized) {
                return;
            }

            // Fire the page rules until we know we have permission.
            RulesEngine.fireRules(request, response, htmlPage, Rule.FireOn.EVERY_PAGE);

            Logger.debug(this.getClass(), "Recording the ClickStream");
            if (Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
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
            String userId = (user != null) ? user.getUserId() : APILocator.getUserAPI().getAnonymousUser().getUserId();
            String language = String.valueOf(langId);
            String urlMap = (String) request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);
            String queryString = request.getQueryString();
            String persona = null;
            Optional<Visitor> v = visitorAPI.getVisitor(request, false);
            if (v.isPresent() && v.get().getPersona() != null) {
                persona = v.get().getPersona().getKeyTag();
            }


            PageCacheParameters cacheParameters =
                    new BlockPageCache.PageCacheParameters(userId, language, urlMap, queryString, persona);


            String key = VelocityUtil.getPageCacheKey(request, htmlPage);
            if (key != null) {
                String cachedPage = CacheLocator.getBlockPageCache().get(htmlPage, cacheParameters);
                if (cachedPage != null ){
                    // have cached response and are not refreshing, send it
                    response.getWriter().write(cachedPage);
                    return;
                }
            }

            Writer tmpOut = (key!=null) ? new StringWriter(4096) : new VelocityFilterWriter(out);
            Context context = VelocityUtil.getWebContext(request, response);


            this.getTemplate(htmlPage, mode).merge(context, tmpOut);


            if (key!=null) {
                String trimmedPage = tmpOut.toString().trim();
                out.write(trimmedPage);
                out.close();
                synchronized (key.intern()) {
                    CacheLocator.getBlockPageCache().add(htmlPage, trimmedPage, cacheParameters);
                }
            } else {
                out.close();
            }
        } finally {
            LicenseUtil.stopLiveMode();
        }

    }



}
