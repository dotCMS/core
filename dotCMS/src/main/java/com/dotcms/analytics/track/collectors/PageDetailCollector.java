package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.Util;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static com.dotcms.exception.ExceptionUtil.getErrorMessage;
import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

/**
 * This class collects the information of Detail Pages used to display URL Mapped content.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class PageDetailCollector implements Collector {

    private final HTMLPageAssetAPI pageAPI;
    private final URLMapAPIImpl urlMapAPI;

    public PageDetailCollector() {
        this(APILocator.getHTMLPageAssetAPI(), APILocator.getURLMapAPI());
    }

    public PageDetailCollector(final HTMLPageAssetAPI pageAPI, URLMapAPIImpl urlMapAPI) {
        this.urlMapAPI = urlMapAPI;
        this.pageAPI = pageAPI;
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return isUrlMap(collectorContextMap);
    }

    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        final String uri = (String) collectorContextMap.get(CollectorContextMap.URI);
        final Host site = (Host) collectorContextMap.get(CollectorContextMap.CURRENT_HOST);
        final Long languageId = (Long) collectorContextMap.get(CollectorContextMap.LANG_ID);
        final PageMode pageMode = (PageMode) collectorContextMap.get(CollectorContextMap.PAGE_MODE);
        final String language = (String)collectorContextMap.get(CollectorContextMap.LANG);

        final UrlMapContext urlMapContext = new UrlMapContext(
                pageMode, languageId, uri, site, APILocator.systemUser());

        final Optional<URLMapInfo> urlMappedContent =
                Try.of(() -> this.urlMapAPI.processURLMap(urlMapContext)).get();

        if (urlMappedContent.isPresent()) {
            final URLMapInfo urlMapInfo = urlMappedContent.get();
            final Contentlet urlMapContentlet = urlMapInfo.getContentlet();
            final ContentType urlMapContentType = urlMapContentlet.getContentType();

            final IHTMLPage detailPageContent = Try.of(() ->
                            this.pageAPI.findByIdLanguageFallback(urlMapContentType.detailPage(), languageId, true, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES))
                    .onFailure(e -> Logger.error(this, String.format("Error finding detail page " +
                            "'%s': %s", urlMapContentType.detailPage(), getErrorMessage(e)), e))
                    .getOrNull();
            final HTMLPageAsset detailPageAsset = (HTMLPageAsset) detailPageContent;
            final HashMap<String, String> pageObject = new HashMap<>();
            pageObject.put(ID, detailPageContent.getIdentifier());
            pageObject.put(TITLE, detailPageContent.getTitle());
            pageObject.put(URL, uri);
            pageObject.put(CONTENT_TYPE_ID, detailPageAsset.getContentTypeId());
            pageObject.put(CONTENT_TYPE_NAME, detailPageAsset.getContentType().name());
            pageObject.put(CONTENT_TYPE_VAR_NAME, detailPageAsset.getContentType().variable());
            pageObject.put(BASE_TYPE, urlMapContentlet.getContentType().baseType().name());
            pageObject.put(LIVE,    String.valueOf(Try.of(urlMapContentlet::isLive).getOrElse(false)));
            pageObject.put(WORKING, String.valueOf(Try.of(urlMapContentlet::isWorking).getOrElse(false)));
            pageObject.put(DETAIL_PAGE_URL, Try.of(detailPageContent::getURI).getOrElse(StringPool.BLANK));
            collectorPayloadBean.put(OBJECT,  pageObject);
        }

        collectorPayloadBean.put(EVENT_TYPE, EventType.PAGE_REQUEST.getType());
        collectorPayloadBean.put(URL, uri);
        collectorPayloadBean.put(LANGUAGE, language);
        collectorPayloadBean.put(LANGUAGE_ID, languageId);

        if (Objects.nonNull(site)) {
            collectorPayloadBean.put(SITE_NAME, site.getHostname());
            collectorPayloadBean.put(SITE_ID, site.getIdentifier());
        }
        return collectorPayloadBean;
    }

    private boolean isUrlMap(final CollectorContextMap collectorContextMap){

        final String uri = (String)collectorContextMap.get(CollectorContextMap.URI);
        final Long languageId = (Long)collectorContextMap.get(CollectorContextMap.LANG_ID);
        final PageMode pageMode = (PageMode)collectorContextMap.get(CollectorContextMap.PAGE_MODE);
        final Host site = (Host) collectorContextMap.get(CollectorContextMap.CURRENT_HOST);

        final UrlMapContext urlMapContext = new UrlMapContext(
                pageMode, languageId, uri, site, APILocator.systemUser());

        return Util.isUrlMap(urlMapContext);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
