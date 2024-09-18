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

        final String uri = (String) collectorContextMap.get("uri");
        final Host site = (Host) collectorContextMap.get("currentHost");
        final Long languageId = (Long) collectorContextMap.get("langId");
        final PageMode pageMode = (PageMode) collectorContextMap.get("pageMode");
        final String language = (String)collectorContextMap.get("lang");

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

            final HashMap<String, String> pageObject = new HashMap<>();
            pageObject.put("id", detailPageContent.getIdentifier());
            pageObject.put("title", detailPageContent.getTitle());
            pageObject.put("url", uri);
            pageObject.put("detail_page_url", urlMapContentType.detailPage());
            collectorPayloadBean.put("object",  pageObject);
        }

        collectorPayloadBean.put("event_type", EventType.PAGE_REQUEST.getType());
        collectorPayloadBean.put("url", uri);
        collectorPayloadBean.put("language", language);

        if (Objects.nonNull(site)) {
            collectorPayloadBean.put("host", site.getIdentifier());
        }
        return collectorPayloadBean;
    }

    private boolean isUrlMap(final CollectorContextMap collectorContextMap){

        final String uri = (String)collectorContextMap.get("uri");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final PageMode pageMode = (PageMode)collectorContextMap.get("pageMode");
        final Host site = (Host) collectorContextMap.get("currentHost");

        final UrlMapContext urlMapContext = new UrlMapContext(
                pageMode, languageId, uri, site, APILocator.systemUser());

        return Util.isUrlMap(urlMapContext);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
