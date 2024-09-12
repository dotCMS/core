package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static com.dotcms.exception.ExceptionUtil.getErrorMessage;
import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

/**
 * This collector collects the page information
 * @author jsanca
 */
public class PagesCollector implements Collector {

    private final HTMLPageAssetAPI pageAPI;
    private final HostAPI hostAPI;
    private final URLMapAPIImpl urlMapAPI;
    private final IdentifierAPI identifierAPI;

    public PagesCollector() {
        this(APILocator.getHTMLPageAssetAPI(), APILocator.getHostAPI(), APILocator.getURLMapAPI(), APILocator.getIdentifierAPI());
    }

    public PagesCollector(final HTMLPageAssetAPI pageAPI,
                          final HostAPI hostAPI, URLMapAPIImpl urlMapAPI, IdentifierAPI identifierAPI) {
        this.pageAPI = pageAPI;
        this.hostAPI = hostAPI;
        this.urlMapAPI = urlMapAPI;
        this.identifierAPI = identifierAPI;
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return PagesAndUrlMapsRequestMatcher.PAGES_AND_URL_MAPS_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()); // should compare with the id
    }

    @Override
    public CollectionCollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectionCollectorPayloadBean collectionCollectorPayloadBean) {

        // we use the same event just collect more information async
        final CollectorPayloadBean collectorPayloadBean = collectionCollectorPayloadBean.first();
        final String uri = (String)collectorContextMap.get("uri");
        final String host = (String)collectorContextMap.get("host");
        final String siteId = (String)collectorContextMap.get("siteId");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final PageMode pageMode = (PageMode)collectorContextMap.get("pageMode");
        final HashMap<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {

            final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
            final UrlMapContext urlMapContext = new UrlMapContext(
                    pageMode, languageId, uri, site, APILocator.systemUser());
            final boolean isUrlMap = this.isUrlMap(urlMapContext);
            if (isUrlMap) {
                final Optional<URLMapInfo> urlMappedContent =
                        Try.of(() -> this.urlMapAPI.processURLMap(urlMapContext)).get();
                if (urlMappedContent.isPresent()) {
                    final URLMapInfo urlMapInfo = urlMappedContent.get();
                    final Contentlet urlMapContentlet = urlMapInfo.getContentlet();
                    final ContentType urlMapContentType = urlMapContentlet.getContentType();
                    pageObject.put("id", urlMapContentlet.getIdentifier());
                    pageObject.put("title", urlMapContentlet.getTitle());
                    pageObject.put("content_type_id", urlMapContentType.id());
                    pageObject.put("content_type_var_name", urlMapContentType.variable());
                    collectorPayloadBean.put("event_type", EventType.URL_MAP.getType());
                    final CollectorPayloadBean detailPageCollectorBean = new ConcurrentCollectorPayloadBean();
                    detailPageCollectorBean.put("detail_page_url", urlMapContentType.detailPage());
                    final IHTMLPage detailPageContent =
                            Try.of(() ->
                                    this.pageAPI.findByIdLanguageFallback(urlMapContentType.detailPage(), languageId, true, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES))
                                    .onFailure(e -> Logger.error(this, String.format("Error finding detail page '%s': %s", urlMapContentType.detailPage(), getErrorMessage(e)), e)).getOrNull();
                    detailPageCollectorBean.put("detail_page_id", detailPageContent.getIdentifier());
                    final Identifier detailPageId =
                            Try.of(() -> this.identifierAPI.find(detailPageContent.getIdentifier()))
                                    .onFailure(e -> Logger.error(this, String.format("Error finding detail page ID '%s': %s", detailPageContent.getIdentifier(), getErrorMessage(e)), e)).getOrElse(new Identifier());
                    detailPageCollectorBean.put("detail_page_url", detailPageId.getPath());
                    detailPageCollectorBean.put("detail_page_title", detailPageContent.getTitle());
                    collectionCollectorPayloadBean.add(detailPageCollectorBean);
                }
            } else {
                final IHTMLPage page = Try.of(() ->
                        this.pageAPI.getPageByPath(uri, site, languageId, true)).get();
                pageObject.put("id", page.getIdentifier());
                pageObject.put("title", page.getTitle());
                collectorPayloadBean.put("event_type", EventType.PAGE_REQUEST.getType());
            }
            pageObject.put("url", uri);
        }

        collectorPayloadBean.put("object",  pageObject);
        collectorPayloadBean.put("url", uri);
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("host", host);
        collectorPayloadBean.put("site", siteId);

        return collectionCollectorPayloadBean;
    }

    /**
     * Based on the specified URL Map Context, determines whether a given incoming URL maps to a URL
     * Mapped content or not.
     *
     * @param pageMode   The {@link PageMode} used to display/render an HTML Page.
     * @param languageId The language ID used to display an HTML Page.
     * @param uri        The URI of the incoming request.
     * @param site       The {@link Host} where the HTML Page lives.
     *
     * @return If the URL maps to URL Mapped content, returns {@code true}.
     */
    private boolean isUrlMap(final UrlMapContext urlMapContext) {
        return Try.of(() -> this.urlMapAPI.isUrlPattern(urlMapContext))
                .onFailure(e -> Logger.error(this, String.format("Failed to check for URL Mapped content for page '%s': %s",
                        urlMapContext.getUri(), getErrorMessage(e)), e))
                .getOrElse(false);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
