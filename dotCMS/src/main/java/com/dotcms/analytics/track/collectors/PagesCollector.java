package com.dotcms.analytics.track.collectors;

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
        return "page" == collectorContextMap.getRequestMatcher(); // should compare with the id
    }

    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        final String uri = (String)collectorContextMap.get("uri");
        final String siteId = (String)collectorContextMap.get("host");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final Map<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {

            final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
            final IHTMLPage page = Try.of(()->this.pageAPI.getPageByPath(uri, site, languageId, true)).get();
            pageObject.put("object_id", page.getIdentifier());
            pageObject.put("title", page.getTitle());
            pageObject.put("path", uri);
        }

        final StringWriter writer = new StringWriter();
        Try.run(()->DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValue(writer, pageObject));
        collectorPayloadBean.put("objects",  writer.toString());
        collectorPayloadBean.put("path", uri);
        collectorPayloadBean.put("event", "page");
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("site", siteId);

        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
