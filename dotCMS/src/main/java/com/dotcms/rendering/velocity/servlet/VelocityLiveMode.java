package com.dotcms.rendering.velocity.servlet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.security.ContentSecurityPolicyUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PageCacheParameters;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.apache.velocity.context.Context;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import static com.dotmarketing.filters.Constants.VANITY_URL_OBJECT;
import java.io.*;
import java.util.Optional;

public class VelocityLiveMode extends VelocityModeHandler {

    @Deprecated
    public VelocityLiveMode(final HttpServletRequest request, final HttpServletResponse response, final String uri, final Host host) {
        this(
                request,
                response,
                VelocityModeHandler.getHtmlPageFromURI(PageMode.get(request), request, uri, host),
                host
        );
    }

    final static ThreadLocal<StringWriter> stringWriterLocal = ThreadLocal.withInitial(StringWriter::new);
    
    
    
    
    protected VelocityLiveMode(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final IHTMLPage htmlPage,
            final Host host) {

        super(request, response, htmlPage, host);
        this.setMode(PageMode.LIVE);
    }

    @Override
    public final void serve() throws DotDataException, IOException, DotSecurityException {
        serve(response.getOutputStream());
    }

    @Override
    public final void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {

        LicenseUtil.startLiveMode();
        try {

            // Find the current language
            long langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();


            // now we check identifier cache first (which DOES NOT have a 404 cache )
            final Identifier id = APILocator.getIdentifierAPI().find(this.htmlPage.getIdentifier());
            if (!host.isLive() || id == null || id.getId() == null) {
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


            User user = getUser();
            final String uri = CMSUrlUtil.getCurrentURI(request);
            Logger.debug(this.getClass(), "Page Permissions for URI=" + uri);

            // Verify and handle the case for unauthorized access of this contentlet
            boolean unauthorized = CMSUrlUtil.getInstance().isUnauthorizedAndHandleError(htmlPage, uri, user, request, response);
            if (unauthorized) {
                return;
            }

            // Fire the page rules until we know we have permission.
            RulesEngine.fireRules(request, response, htmlPage, Rule.FireOn.EVERY_PAGE);

            Logger.debug(this.getClass(), "Recording the ClickStream");
            if (Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
                if (user != null) {

                        ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response), host);
                    
                } else {
                    ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response), host);
                }
            }

            //Validate if template is publish, if not remove page from cache
            if (!UtilMethods.isSet(APILocator.getTemplateAPI().findLiveTemplate(htmlPage.getTemplateId(),APILocator.systemUser(),false).getInode())) {
                CacheLocator.getVeloctyResourceCache()
                        .remove(new VelocityResourceKey((HTMLPageAsset) htmlPage, PageMode.LIVE,
                                htmlPage.getLanguageId()));
            }
            int statusCode = response.getStatus();
            // Begin page caching
            String userId = (user != null) ? user.getUserId() : APILocator.getUserAPI().getAnonymousUser().getUserId();
            String language = String.valueOf(langId);
            String urlMap = (String) request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);
            String vanityUrl =  request.getAttribute(VANITY_URL_OBJECT)!=null ? ((CachedVanityUrl)request.getAttribute(VANITY_URL_OBJECT)).vanityUrlId : "";
            String queryString = PageCacheParameters.filterQueryString(request.getQueryString());
            String persona = null;
            Optional<Visitor> v = visitorAPI.getVisitor(request, false);
            if (v.isPresent() && v.get().getPersona() != null) {
                persona = v.get().getPersona().getKeyTag();
            }
            final String originalUrl = (String)  request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) ;
            
            Date modDate = htmlPage.getModDate()!=null ? htmlPage.getModDate() : new Date(0);
            
            final Context context = VelocityUtil.getInstance().getContext(request, response);

            final PageCacheParameters cacheParameters =
                    new PageCacheParameters("user:" + userId,
                                    "lang:" + language,
                                    "urlmap:" + urlMap,
                                    "query:" + queryString,
                                    "persona:" + persona,
                                    "pageInode:" + htmlPage.getInode(),
                                    "modDate:" + modDate.getTime(),
                                    "vanity:" + vanityUrl,
                                    "variant:" + WebAPILocator.getVariantWebAPI().currentVariantId()
                                    );
            
            final boolean shouldCache = VelocityUtil.shouldPageCache(request, htmlPage);

            if(response.getHeader("Cache-Control")==null) {
                // set cache control headers based on page cache
                final String cacheControl = htmlPage.getCacheTTL() >= 0 ? "max-age=" +  htmlPage.getCacheTTL() : "no-cache";
                response.setHeader("Cache-Control",  cacheControl);
            }
            
            if (shouldCache) {

                final String cachedPage = CacheLocator.getBlockPageCache().get(htmlPage, cacheParameters);
                if (cachedPage != null) {
                    // have cached response and are not refreshing, send it
                    out.write(cachedPage.getBytes());
                    return;
                }
            }
            
            try (final Writer tmpOut = shouldCache ? stringWriterLocal.get() : new BufferedWriter(new OutputStreamWriter(out))) {

                if (ContentSecurityPolicyUtil.isConfig()) {
                    ContentSecurityPolicyUtil.init(request);
                    ContentSecurityPolicyUtil.addHeader(response);
                }

                HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
                HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

                this.getTemplate(htmlPage, mode).merge(context, tmpOut);

                if (shouldCache) {
                    final String trimmedPage = tmpOut.toString().trim();
                    out.write(trimmedPage.getBytes());

                    if(response.getStatus() == 200) {
                        CacheLocator.getBlockPageCache()
                                .add(htmlPage, trimmedPage, cacheParameters);

                    }
                }
            }
        } finally {
            stringWriterLocal.get().getBuffer().setLength(0);
            LicenseUtil.stopLiveMode();
        }
    }



    User getUser() {
        User user = null;
        final HttpSession session = request.getSession(false);

        if (session != null) {
            user = (User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
        }

        return user;
    }
}
