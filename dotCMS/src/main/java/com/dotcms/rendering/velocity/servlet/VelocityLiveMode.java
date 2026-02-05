package com.dotcms.rendering.velocity.servlet;

import static com.dotmarketing.filters.Constants.VANITY_URL_OBJECT;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
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
import com.dotmarketing.business.StaticPageCache;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.templates.model.TemplateVersionInfo;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.util.concurrent.Striped;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.velocity.context.Context;

public class VelocityLiveMode extends VelocityModeHandler {

    /**
     * Initial capacity for the page rendering buffer.
     * Most pages are under 64KB, so this avoids early resizing.
     * Configurable via PAGE_BUFFER_INITIAL_SIZE property.
     */
    private static final int BUFFER_INITIAL_SIZE =
            Config.getIntProperty("PAGE_BUFFER_INITIAL_SIZE", 64 * 1024);  // 64KB default

    /**
     * Maximum retained capacity for the ThreadLocal buffer.
     * If a page grows the buffer beyond this size, we replace it with a fresh one
     * after the request completes to prevent memory bloat from large pages.
     * Configurable via PAGE_BUFFER_MAX_RETAINED_SIZE property.
     */
    private static final int BUFFER_MAX_RETAINED_SIZE =
            Config.getIntProperty("PAGE_BUFFER_MAX_RETAINED_SIZE", 256 * 1024);  // 256KB default

    /**
     * ThreadLocal buffer for capturing page output during cache population.
     * Uses a custom ResettableByteArrayOutputStream that exposes capacity info.
     */
    private static final ThreadLocal<ResettableByteArrayOutputStream> byteArrayLocal =
            ThreadLocal.withInitial(() -> new ResettableByteArrayOutputStream(BUFFER_INITIAL_SIZE));

    private static final long PAGE_CACHE_TIMEOUT_MILLIS = Config.getIntProperty("PAGE_CACHE_TIMEOUT_MILLIS", 2000);

    private static final Striped<Lock> stripedLock = Striped.lazyWeakLock(
            Config.getIntProperty("PAGE_CACHE_STRIPES", 64));

    /**
     * A ByteArrayOutputStream that exposes its internal buffer capacity for memory management.
     * This allows us to detect when the buffer has grown too large and should be replaced.
     */
    static final class ResettableByteArrayOutputStream extends ByteArrayOutputStream {

        ResettableByteArrayOutputStream(final int initialCapacity) {
            super(initialCapacity);
        }

        /**
         * Returns the current capacity of the internal buffer (not the data size).
         * @return the buffer capacity in bytes
         */
        int getCapacity() {
            return buf.length;
        }

        /**
         * Checks if the buffer has grown beyond the specified threshold.
         * @param threshold the maximum acceptable capacity
         * @return true if buffer capacity exceeds threshold
         */
        boolean exceedsCapacity(final int threshold) {
            return buf.length > threshold;
        }
    }


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
        processUrlMapTags(request);
        if (!VelocityUtil.shouldPageCache(request, htmlPage)) {
            try (Writer tmpOut = new OutputStreamWriter(out)) {
                writePage(tmpOut, htmlPage);
                return;
            }

        }

        final PageCacheParameters cacheParameters = buildCacheParameters(langId, htmlPage);

        // Use byte[] cache to avoid String conversion overhead
        byte[] cachedBytes = pageCache.getBytes(htmlPage, cacheParameters);
        if (cachedBytes != null) {
            // have cached response and are not refreshing, send it directly
            out.write(cachedBytes);
            return;
        }

        final Lock lock = stripedLock.get(cacheParameters.getKey());

        boolean hasLock = false;
        try {
            hasLock = lock.tryLock(PAGE_CACHE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            // Double-check cache inside lock using byte[] for efficiency
            byte[] cachedBytes2 = pageCache.getBytes(htmlPage, cacheParameters);
            if (cachedBytes2 != null) {
                Logger.debug(this.getClass(), "Found cached page in striped lock: " + cacheParameters.getKey());
                // have cached response and are not refreshing, send it directly
                out.write(cachedBytes2);
                return;
            }
            if (hasLock) {
                try (Writer tmpOut = new OutputStreamWriter(new TeeOutputStream(out, byteArrayLocal.get()))) {
                    final int startResponse = response.getStatus();

                    writePage(tmpOut, htmlPage);
                    tmpOut.flush();
                    // if velocity has not changed the response status, we can cache it
                    if (response.getStatus() == startResponse) {
                        // Cache bytes directly - avoids toString() String allocation
                        pageCache.addBytes(htmlPage, byteArrayLocal.get().toByteArray(), cacheParameters);
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
            if (hasLock) {
                lock.unlock();
            }
            resetBuffer();
        }

    }

    /**
     * Resets the ThreadLocal buffer for reuse.
     * If the buffer has grown beyond BUFFER_MAX_RETAINED_SIZE, it is replaced with a fresh one
     * to prevent memory bloat from large pages persisting in the ThreadLocal.
     */
    private static void resetBuffer() {
        final ResettableByteArrayOutputStream buffer = byteArrayLocal.get();
        if (buffer.exceedsCapacity(BUFFER_MAX_RETAINED_SIZE)) {
            // Buffer grew too large (e.g., from a very large page), replace it
            // to release the oversized byte array back to GC
            Logger.debug(VelocityLiveMode.class, () ->
                    "Page buffer exceeded max retained size (" + buffer.getCapacity() +
                    " > " + BUFFER_MAX_RETAINED_SIZE + "), replacing with fresh buffer");
            byteArrayLocal.set(new ResettableByteArrayOutputStream(BUFFER_INITIAL_SIZE));
        } else {
            // Normal case: just reset the buffer position, keep the allocated array
            buffer.reset();
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

    private void processUrlMapTags(@NotNull HttpServletRequest request) {
        if (!Config.getBooleanProperty("ACCRUE_TAGS_IN_URLMAPS", true)) {
            return;
        }
        if (request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE) == null) {
            return;
        }
        Optional<Visitor> visitor = visitorAPI.getVisitor(request, false);
        if (visitor.isEmpty()) {
            return;
        }

        String inode = (String) request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);

        List<Tag> contentTags = Try.of(() -> APILocator.getTagAPI().getTagsByInode(inode))
                .onFailure(e -> Logger.warnAndDebug(VelocityLiveMode.class,
                        "unable to read tags for inode:" + inode + " : " + e.getMessage(), e))
                .getOrElse(List.of());
        for (Tag tag : contentTags) {
            visitor.get().addTag(tag.getTagName());
        }
    }








}
