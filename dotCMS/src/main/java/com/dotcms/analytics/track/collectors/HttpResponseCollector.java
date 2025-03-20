package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.api.web.HttpServletResponseThreadLocal;

import javax.servlet.http.HttpServletResponse;

/**
 * Collect the HTTP Response code
 */
public class HttpResponseCollector implements Collector {
    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return FilesRequestMatcher.FILES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ||
                PagesAndUrlMapsRequestMatcher.PAGES_AND_URL_MAPS_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId());
    }

    @Override
    public CollectorPayloadBean collect(CollectorContextMap collectorContextMap, CollectorPayloadBean collectorPayloadBean) {
        final HttpServletResponse response = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        collectorPayloadBean.put(HTTP_RESPONSE_CODE, response.getStatus());
        collectorPayloadBean.put(EVENT_TYPE, EventType.HTTP_RESPONSE.getType());

        final String uri = (String)collectorContextMap.get(CollectorContextMap.URI);
        collectorPayloadBean.put(URL, uri);

        return collectorPayloadBean;
    }
}
