package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.Util;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
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
    private final URLMapAPIImpl urlMapAPI;

    public PagesCollector() {
        this(APILocator.getHTMLPageAssetAPI(), APILocator.getURLMapAPI());
    }

    public PagesCollector(final HTMLPageAssetAPI pageAPI,
                           final URLMapAPIImpl urlMapAPI) {
        this.pageAPI = pageAPI;
        this.urlMapAPI = urlMapAPI;
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return PagesAndUrlMapsRequestMatcher.PAGES_AND_URL_MAPS_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId());
    }

    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        final String uri = (String)collectorContextMap.get("uri");
        final Host site = (Host) collectorContextMap.get("currentHost");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final PageMode pageMode = (PageMode)collectorContextMap.get("pageMode");
        final HashMap<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(site) && Objects.nonNull(languageId)) {

            final boolean isUrlMap = isUrlMap(collectorContextMap);

            if (isUrlMap) {

                final UrlMapContext urlMapContext = new UrlMapContext(
                        pageMode, languageId, uri, site, APILocator.systemUser());

                final Optional<URLMapInfo> urlMappedContent =
                        Try.of(() -> this.urlMapAPI.processURLMap(urlMapContext)).get();

                if (urlMappedContent.isPresent()) {
                    final URLMapInfo urlMapInfo = urlMappedContent.get();
                    final Contentlet urlMapContentlet = urlMapInfo.getContentlet();
                    final ContentType urlMapContentType = urlMapContentlet.getContentType();
                    pageObject.put("id", urlMapContentlet.getIdentifier());
                    pageObject.put("title", urlMapContentlet.getTitle());
                    pageObject.put("content_type_id", urlMapContentType.id());
                    pageObject.put("content_type_name", urlMapContentType.name());
                    pageObject.put("content_type_var_name", urlMapContentType.variable());
                    collectorPayloadBean.put("event_type", EventType.URL_MAP.getType());
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

        if (Objects.nonNull(site)) {
            collectorPayloadBean.put("host",  site.getIdentifier());
        }

        return collectorPayloadBean;
    }

    private boolean isUrlMap(final CollectorContextMap collectorContextMap){

        final String uri = (String)collectorContextMap.get("uri");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final PageMode pageMode = (PageMode)collectorContextMap.get("pageMode");
        final Host currentHost = (Host) collectorContextMap.get("currentHost");

        final UrlMapContext urlMapContext = new UrlMapContext(
                pageMode, languageId, uri, currentHost, APILocator.systemUser());

        return Util.isUrlMap(urlMapContext);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
