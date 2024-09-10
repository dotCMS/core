package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import io.vavr.control.Try;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        final String siteId = (String)collectorContextMap.get("host");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final HashMap<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {

            final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
            final IHTMLPage page = Try.of(()->this.pageAPI.getPageByPath(uri, site, languageId, true)).get();
            pageObject.put("object_id", page.getIdentifier());
            pageObject.put("title", page.getTitle());
            pageObject.put("path", uri);
        }

        collectorPayloadBean.put("object",  pageObject);
        collectorPayloadBean.put("path", uri);
        collectorPayloadBean.put("event_type", "PAGE_REQUEST"); // todo: move to enum
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("site", siteId);

        return collectionCollectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
