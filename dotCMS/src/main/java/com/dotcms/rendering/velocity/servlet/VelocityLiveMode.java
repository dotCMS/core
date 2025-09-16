package com.dotcms.rendering.velocity.servlet;

import static com.dotmarketing.filters.Constants.VANITY_URL_OBJECT;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.concurrent.DotConcurrentException;
import com.dotcms.concurrent.lock.DotKeyLockManager;
import com.dotcms.concurrent.lock.DotKeyLockManagerBuilder;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.security.ContentSecurityPolicyUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BlockPageCache;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PageCacheParameters;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
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
import com.liferay.portal.util.PortalUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.context.Context;

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

    final static Lazy<DotKeyLockManager<String>> lockManagerLazy = Lazy.of(() -> {
        int pageCacheStripes = Config.getIntProperty("PAGE_CACHE_STRIPES", 64);
        Duration pageCacheTimeout = Duration.ofMillis(Config.getIntProperty("PAGE_CACHE_TIMEOUT_MILLIS", 2000));
        return DotKeyLockManagerBuilder.newLockManager("PAGE_CACHE_LOCK", pageCacheStripes,
                pageCacheTimeout);
    });



    BlockPageCache pageCache = CacheLocator.getBlockPageCache();
    
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
        boolean unauthorized = CMSUrlUtil.getInstance().isUnauthorizedAndHandleError(htmlPage, uri, user, request, response);
        if (unauthorized) {
            return;
        }

        // Fire the page rules until we know we have permission.
        RulesEngine.fireRules(request, response, htmlPage, Rule.FireOn.EVERY_PAGE);


        //Validate if template is publish, if not remove page from cache
        if (!UtilMethods.isSet(APILocator.getTemplateAPI().findLiveTemplate(htmlPage.getTemplateId(),APILocator.systemUser(),false).getInode())) {
            CacheLocator.getVeloctyResourceCache()
                    .remove(new VelocityResourceKey((HTMLPageAsset) htmlPage, PageMode.LIVE,
                            htmlPage.getLanguageId()));
        }

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
        Date modDate = htmlPage.getModDate()!=null ? htmlPage.getModDate() : new Date(0);



        List<String> pageCacheKeys = new ArrayList<>();
        // we only add the originalUrl if the response is a success, allowing us to cache 404 effectively
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            final String originalUrl = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
            pageCacheKeys.add("originalUrl:" + originalUrl);
        }
        pageCacheKeys.add("user:" + userId);
        pageCacheKeys.add("lang:" + language);
        pageCacheKeys.add("urlMap:" + urlMap);
        pageCacheKeys.add("query:" + queryString);
        pageCacheKeys.add("persona:" + persona);
        pageCacheKeys.add("pageInode:" + htmlPage.getInode());
        pageCacheKeys.add("modDate:" + modDate.getTime());
        pageCacheKeys.add("vanity:" + vanityUrl);
        pageCacheKeys.add("variant:" + WebAPILocator.getVariantWebAPI().currentVariantId());
        pageCacheKeys.add("status:" + response.getStatus());

        final PageCacheParameters cacheParameters =
                new PageCacheParameters(pageCacheKeys.toArray(new String[0]));

        final boolean shouldCache = VelocityUtil.shouldPageCache(request, htmlPage);

        if(response.getHeader("Cache-Control")==null) {
            // set cache control headers based on page cache
            final String cacheControl = htmlPage.getCacheTTL() >= 0 ? "max-age=" +  htmlPage.getCacheTTL() : "no-cache";
            response.setHeader("Cache-Control",  cacheControl);
        }
        if (ContentSecurityPolicyUtil.isConfig()) {
            ContentSecurityPolicyUtil.init(request);
            ContentSecurityPolicyUtil.addHeader(response);
        }

        if (shouldCache) {
            String cachedPage = pageCache.get(htmlPage, cacheParameters);
            if (cachedPage != null) {
                // have cached response and are not refreshing, send it
                out.write(cachedPage.getBytes());
                return;
            }
            try {
                lockManagerLazy.get().tryLock(cacheParameters.getKey(), () -> {
                    String cachedPage2 = pageCache.get(htmlPage, cacheParameters);
                    if (cachedPage2 != null) {
                        // have cached response and are not refreshing, send it
                        Try.run(() -> out.write(cachedPage2.getBytes())).onFailure( e-> {
                            throw new DotRuntimeException(e);
                        });

                        return;
                    }

                    final Writer tmpOut = stringWriterLocal.get();
                    final Context context = VelocityUtil.getInstance().getContext(request, response);

                    writePage(tmpOut, htmlPage, mode, context);

                    final String trimmedPage = tmpOut.toString().trim();
                    Try.run(() -> {
                        out.write(trimmedPage.getBytes());
                        out.flush();
                    }).onFailure( e-> {
                        throw new DotRuntimeException(e);
                    });

                    if(response.getStatus() == 200) {
                        pageCache.add(htmlPage, trimmedPage, cacheParameters);
                    }


                });
                return;
            }catch(DotConcurrentException t){
                Logger.debug(this.getClass(), "Timeout while trying to cache page, proceeding" );
                Logger.debug(this.getClass(), "--- " + t.getMessage(),t);
            }
            catch(Throwable t){
                Logger.error(this.getClass(), "Error while trying to render page:" + cacheParameters.getKey()   + t.getMessage() );
                Logger.debug(this.getClass(), "--- " ,t);
            }
            finally{
                stringWriterLocal.get().getBuffer().setLength(0);
            }


        }


        final Context context = VelocityUtil.getInstance().getContext(request, response);
        writePage(new OutputStreamWriter(out), htmlPage, mode, context);

    }


    private void writePage(final Writer out, final IHTMLPage htmlPage, final PageMode mode, Context context)  {
        this.getTemplate(htmlPage, mode).merge(context, out);
    }



    User getUser() {
        return PortalUtil.getUser(request);
    }
}
