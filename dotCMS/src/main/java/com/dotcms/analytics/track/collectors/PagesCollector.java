package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * This collector collects the page information
 * @author jsanca
 */
public class PagesCollector implements Collector {

    private final HTMLPageAssetAPI pageAPI;
    private final HostAPI hostAPI;


    public PagesCollector() {
        this(APILocator.getHTMLPageAssetAPI(),
                APILocator.getHostAPI());
    }

    public PagesCollector(final HTMLPageAssetAPI pageAPI,
                          final HostAPI hostAPI) {

        this.pageAPI = pageAPI;
        this.hostAPI = hostAPI;
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
                    pageMode, languageId, uri, site, Try.of(() -> APILocator.getUserAPI().getSystemUser()).get());
            final boolean isUrlMap = this.isUrlMap(urlMapContext);
            if (isUrlMap) {
                final Optional<URLMapInfo> urlMappedContent =
                        Try.of(() -> APILocator.getURLMapAPI().processURLMap(urlMapContext)).get();
                if (urlMappedContent.isPresent()) {
                    final URLMapInfo urlMapInfo = urlMappedContent.get();
                    pageObject.put("id", urlMapInfo.getContentlet().getIdentifier());
                    pageObject.put("title", urlMapInfo.getContentlet().getTitle());
                    pageObject.put("content_type_id", urlMapInfo.getContentlet().getContentTypeId());
                    pageObject.put("content_type_var_name", urlMapInfo.getContentlet().getContentType().variable());
                    collectorPayloadBean.put("event_type", "URL_MAP"); // todo: move to enum
                }
            } else {
                final IHTMLPage page = Try.of(() ->
                        this.pageAPI.getPageByPath(uri, site, languageId, true)).get();
                pageObject.put("id", page.getIdentifier());
                pageObject.put("title", page.getTitle());
                collectorPayloadBean.put("event_type", EventType.PAGE_REQUEST.getType());
            }
            pageObject.put("path", uri);
        }

        collectorPayloadBean.put("object",  pageObject);
        collectorPayloadBean.put("url", uri);
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("host", host);
        collectorPayloadBean.put("site", siteId);

        return collectionCollectorPayloadBean;
    }

    /**
     *
     * @param pageMode
     * @param languageId
     * @param uri
     * @param site
     * @return
     */
    private boolean isUrlMap(final UrlMapContext urlMapContext) {
        return Try.of(() -> APILocator.getURLMapAPI().isUrlPattern(urlMapContext))
                .onFailure(e -> Logger.error(this, String.format("Failed to check for URL Mapped content for page '%s': %s",
                        urlMapContext.getUri(), ExceptionUtil.getErrorMessage(e)), e))
                .getOrElse(false);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
