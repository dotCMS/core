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
import com.liferay.util.StringPool;
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

        final String uri = (String)collectorContextMap.get(CollectorContextMap.URI);
        final Host site = (Host) collectorContextMap.get(CollectorContextMap.CURRENT_HOST);
        final Long languageId = (Long)collectorContextMap.get(CollectorContextMap.LANG_ID);
        final String language = (String)collectorContextMap.get(CollectorContextMap.LANG);
        final PageMode pageMode = (PageMode)collectorContextMap.get(CollectorContextMap.PAGE_MODE);
        final HashMap<String, String> pageObject = new HashMap<>();
        collectorPayloadBean.put(EVENT_TYPE, EventType.PAGE_REQUEST.getType());
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
                    pageObject.put(ID, urlMapContentlet.getIdentifier());
                    pageObject.put(TITLE, urlMapContentlet.getTitle());
                    pageObject.put(CONTENT_TYPE_ID, urlMapContentType.id());
                    pageObject.put(CONTENT_TYPE_NAME, urlMapContentType.name());
                    pageObject.put(CONTENT_TYPE_VAR_NAME, urlMapContentType.variable());
                    pageObject.put(BASE_TYPE, urlMapContentType.baseType().name());
                    pageObject.put(LIVE,    String.valueOf(Try.of(()->urlMapContentlet.isLive()).getOrElse(false)));
                    pageObject.put(WORKING, String.valueOf(Try.of(()->urlMapContentlet.isWorking()).getOrElse(false)));
                    collectorPayloadBean.put(EVENT_TYPE, EventType.URL_MAP.getType());
                }
            } else {
                final IHTMLPage page = Try.of(() ->
                        this.pageAPI.getPageByPath(uri, site, languageId, true)).get();
                pageObject.put(ID, page.getIdentifier());
                pageObject.put(TITLE, page.getTitle());
                final Contentlet pageContentlet = (Contentlet) page;
                pageObject.put(CONTENT_TYPE_ID, pageContentlet.getContentType().id());
                pageObject.put(CONTENT_TYPE_NAME, pageContentlet.getContentType().name());
                pageObject.put(CONTENT_TYPE_VAR_NAME, pageContentlet.getContentType().variable());
                pageObject.put(BASE_TYPE, pageContentlet.getContentType().baseType().name());
                pageObject.put(LIVE,    String.valueOf(Try.of(()->page.isLive()).getOrElse(false)));
                pageObject.put(WORKING, String.valueOf(Try.of(()->page.isWorking()).getOrElse(false)));
                collectorPayloadBean.put(EVENT_TYPE, EventType.PAGE_REQUEST.getType());
            }

            pageObject.put(URL, uri);
        }

        collectorPayloadBean.put(OBJECT,  pageObject);
        collectorPayloadBean.put(URL, uri);
        collectorPayloadBean.put(LANGUAGE, language);
        collectorPayloadBean.put(LANGUAGE_ID, languageId);

        if (Objects.nonNull(site)) {
            collectorPayloadBean.put(SITE_NAME,  site.getHostname());
            collectorPayloadBean.put(SITE_ID, site.getIdentifier());
        }

        return collectorPayloadBean;
    }

    private boolean isUrlMap(final CollectorContextMap collectorContextMap){

        final String uri = (String)collectorContextMap.get(CollectorContextMap.URI);
        final Long languageId = (Long)collectorContextMap.get(CollectorContextMap.LANG_ID);
        final PageMode pageMode = (PageMode)collectorContextMap.get(CollectorContextMap.PAGE_MODE);
        final Host currentHost = (Host) collectorContextMap.get(CollectorContextMap.CURRENT_HOST);

        final UrlMapContext urlMapContext = new UrlMapContext(
                pageMode, languageId, uri, currentHost, APILocator.systemUser());

        return Util.isUrlMap(urlMapContext);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
