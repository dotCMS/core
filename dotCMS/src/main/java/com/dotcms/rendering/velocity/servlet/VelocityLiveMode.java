package com.dotcms.rendering.velocity.servlet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.org.apache.commons.io.output.TeeOutputStream;
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
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.templates.model.TemplateVersionInfo;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.util.concurrent.Striped;
import com.liferay.portal.model.User;
import java.nio.charset.StandardCharsets;

import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import static com.dotmarketing.filters.Constants.VANITY_URL_OBJECT;
import java.io.*;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class VelocityLiveMode extends VelocityModeHandler {

    final static ThreadLocal<ByteArrayOutputStream> byteArrayLocal = ThreadLocal.withInitial(
            ByteArrayOutputStream::new);
    final static long PAGE_CACHE_TIMEOUT_MILLIS = Config.getIntProperty("PAGE_CACHE_TIMEOUT_MILLIS", 2000);


    private static final Striped<Lock> stripedLock = Striped.lazyWeakLock(
            Config.getIntProperty("PAGE_CACHE_STRIPES", 64));

    @Deprecated
    public VelocityLiveMode(final HttpServletRequest request, final HttpServletResponse response, final String uri,
                            final Host host) {
        this(
                request,
                response,
                VelocityModeHandler.getHtmlPageFromURI(PageMode.get(request), request, uri, host),
                host
        );
    }


    protected VelocityLiveMode(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final IHTMLPage htmlPage,
            final Host host) {

        super(request, response, htmlPage, host);
        this.setMode(PageMode.LIVE);
    }

    private static final StaticPageCache pageCache = CacheLocator.getStaticPageCache();

    @Override
    public final void serve() throws DotDataException, IOException, DotSecurityException {
        serve(response.getOutputStream());
    }

    @Override
    public final void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

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
        boolean unauthorized = CMSUrlUtil.getInstance()
                .isUnauthorizedAndHandleError(htmlPage, uri, user, request, response);
        if (unauthorized) {
            return;
        }

        validateTemplate(htmlPage);

        // Fire the page rules until we know we have permission.
        RulesEngine.fireRules(request, response, htmlPage, Rule.FireOn.EVERY_PAGE);


        addHeaders(htmlPage);

        if (!VelocityUtil.shouldPageCache(request, htmlPage)) {
            try (Writer tmpOut = new OutputStreamWriter(out)) {
                writePage(tmpOut, htmlPage);
                return;
            }

        }

        final PageCacheParameters cacheParameters = buildCacheParameters(langId, htmlPage);

        String cachedPage = pageCache.get(htmlPage, cacheParameters);
        if (cachedPage != null) {
            // have cached response and are not refreshing, send it
            out.write(cachedPage.getBytes(StandardCharsets.UTF_8));
            return;
        }

        final Lock lock = stripedLock.get(cacheParameters.getKey());

        boolean hasLock = false;
        try {
            hasLock = lock.tryLock(PAGE_CACHE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            String cachedPage2 = pageCache.get(htmlPage, cacheParameters);
            if (cachedPage2 != null) {
                Logger.debug(this.getClass(), "Found cached page in striped lock: " + cacheParameters.getKey());
                // have cached response and are not refreshing, send it
                out.write(cachedPage2.getBytes(StandardCharsets.UTF_8));
                return;
            }
            if (hasLock) {
                try (Writer tmpOut = new OutputStreamWriter(new TeeOutputStream(out, byteArrayLocal.get()))) {
                    int startResponse = response.getStatus();

                    writePage(tmpOut, htmlPage);
                    tmpOut.flush();
                    // if velocity has not changed the response status, we can cache it
                    if (response.getStatus() == startResponse) {
                        pageCache.add(htmlPage, byteArrayLocal.get().toString(StandardCharsets.UTF_8), cacheParameters);
                    }
                }
            } else {
                Logger.infoEvery(this.getClass(), "Timeout waiting for velocity, page:" + request.getAttribute(
                        RequestDispatcher.FORWARD_REQUEST_URI), 5000);
                try (Writer tmpOut = new OutputStreamWriter(out)) {
                    writePage(tmpOut, htmlPage);
                }
            }
        } catch (Throwable t) {
            Logger.warn(this.getClass(),
                    "Error while trying to render page:" + cacheParameters.getKey() + ":" + t.getMessage());
            Logger.debug(this.getClass(), "--- ", t);
            throw new DotRuntimeException(t);
        } finally {
            if (Thread.holdsLock(lock)) {
                lock.unlock();
            }
            byteArrayLocal.get().reset();
        }

    }


    /**
     * Builds PageCacheParameters with all necessary cache keys for page caching.
     *
     * @param langId   the language ID
     * @param htmlPage the HTML page being served
     * @return PageCacheParameters instance with all cache keys
     */
    private PageCacheParameters buildCacheParameters(final long langId, final IHTMLPage htmlPage) {
        String userId = (getUser() != null) ? getUser().getUserId() : "anonymous";
        String language = String.valueOf(langId);
        String urlMap = (String) request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);
        String vanityUrl = request.getAttribute(VANITY_URL_OBJECT) != null
                ? ((CachedVanityUrl) request.getAttribute(VANITY_URL_OBJECT)).vanityUrlId
                : "";
        String queryString = PageCacheParameters.filterQueryString(request.getQueryString());
        String persona = Try.of(() -> visitorAPI.getVisitor(request, false).get().getPersona().getKeyTag())
                .getOrElse("");

        final String pageUrl = Try.of(() -> htmlPage.getURI())
                .getOrElse((String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI));


        Date modDate = htmlPage.getModDate() != null ? htmlPage.getModDate() : new Date(0);

        return new PageCacheParameters(
                "pageUrl:" + pageUrl,
                "site:" + htmlPage.getHost(),
                "user:" + userId,
                "lang:" + language,
                "urlmap:" + urlMap,
                "query:" + queryString,
                "persona:" + persona,
                "pageInode:" + htmlPage.getInode(),
                "modDate:" + modDate.getTime(),
                "vanity:" + vanityUrl,
                "variant:" + WebAPILocator.getVariantWebAPI().currentVariantId()
        );
    }

    /**
     * Writes the page to the output stream.
     * @param out
     * @param htmlPage
     */
    private void writePage(final Writer out, final IHTMLPage htmlPage) {
        final Context context = VelocityUtil.getInstance().getContext(request, response);
        this.getTemplate(htmlPage, mode).merge(context, out);
    }


    User getUser() {
        return PortalUtil.getUser(request);
    }

    /**
     * Add headers to the response
     * @param page
     */
    private void addHeaders(IHTMLPage page) {

        if (response.getHeader("Cache-Control") == null) {
            // set cache control headers based on page cache
            final String cacheControl = htmlPage.getCacheTTL() >= 0 ? "max-age=" + htmlPage.getCacheTTL() : "no-cache";
            response.setHeader("Cache-Control", cacheControl);
        }
        if (ContentSecurityPolicyUtil.isConfig()) {
            ContentSecurityPolicyUtil.init(request);
            ContentSecurityPolicyUtil.addHeader(response);
        }

    }


    /**
     * Validate if template is published, if not remove page from cache
     *
     * @param htmlPage
     */
    private void validateTemplate(IHTMLPage htmlPage) {

        TemplateVersionInfo liveTemplate = (TemplateVersionInfo) Try.of(
                () -> APILocator.getVersionableAPI().getVersionInfo(htmlPage.getTemplateId())).getOrNull();

        if (UtilMethods.isEmpty(() -> liveTemplate.getLiveInode())) {
            CacheLocator.getVeloctyResourceCache()
                    .remove(new VelocityResourceKey((HTMLPageAsset) htmlPage, PageMode.LIVE,
                            htmlPage.getLanguageId()));
        }


    }
}
