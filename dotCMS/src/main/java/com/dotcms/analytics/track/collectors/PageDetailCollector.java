package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.Util;
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
 * This class collects the information of Detail Pages used to display URL Mapped content.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class PageDetailCollector implements Collector {

    private final HTMLPageAssetAPI pageAPI;
    private final HostAPI hostAPI;
    private final URLMapAPIImpl urlMapAPI;
    private final IdentifierAPI identifierAPI;

    public PageDetailCollector() {
        this(APILocator.getHTMLPageAssetAPI(), APILocator.getHostAPI(), APILocator.getURLMapAPI(), APILocator.getIdentifierAPI());
    }

    public PageDetailCollector(final HTMLPageAssetAPI pageAPI,
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
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {


        final String uri = (String) collectorContextMap.get("uri");
        final String host = (String) collectorContextMap.get("host");
        final String siteId = (String) collectorContextMap.get("siteId");
        final Long languageId = (Long) collectorContextMap.get("langId");
        final String language = (String) collectorContextMap.get("lang");
        final PageMode pageMode = (PageMode) collectorContextMap.get("pageMode");
        final HashMap<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {
            final Host site = Try.of(() -> this.hostAPI.find(siteId, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES)).get();
            final UrlMapContext urlMapContext = new UrlMapContext(
                    pageMode, languageId, uri, site, APILocator.systemUser());
            final boolean isUrlMap = Util.isUrlMap(urlMapContext);
            if (isUrlMap) {
                final Optional<URLMapInfo> urlMappedContent =
                        Try.of(() -> this.urlMapAPI.processURLMap(urlMapContext)).get();
                if (urlMappedContent.isPresent()) {
                    final URLMapInfo urlMapInfo = urlMappedContent.get();
                    final Contentlet urlMapContentlet = urlMapInfo.getContentlet();
                    final ContentType urlMapContentType = urlMapContentlet.getContentType();
                    pageObject.put("detail_page_url", urlMapContentType.detailPage());
                    final IHTMLPage detailPageContent = Try.of(() ->
                                    this.pageAPI.findByIdLanguageFallback(urlMapContentType.detailPage(), languageId, true, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES))
                                    .onFailure(e -> Logger.error(this, String.format("Error finding detail page " +
                                                            "'%s': %s", urlMapContentType.detailPage(), getErrorMessage(e)), e))
                            .getOrNull();
                    pageObject.put("id", detailPageContent.getIdentifier());
                    final Identifier detailPageId = Try.of(() ->
                                    this.identifierAPI.find(detailPageContent.getIdentifier()))
                                    .onFailure(e -> Logger.error(this, String.format("Error finding detail page ID " +
                                                    "'%s': %s", detailPageContent.getIdentifier(), getErrorMessage(e)), e))
                            .getOrElse(new Identifier());
                    pageObject.put("url", detailPageId.getPath());
                    pageObject.put("title", detailPageContent.getTitle());
                }
            }
            pageObject.put("url", uri);
        }

        collectorPayloadBean.put("object", pageObject);
        collectorPayloadBean.put("url", uri);
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("host", host);
        collectorPayloadBean.put("site", siteId);
        collectorPayloadBean.put("event_type", EventType.PAGE_REQUEST.getType());

        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

}
