package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.HttpResponseMatcher;
import com.dotcms.api.web.HttpServletResponseThreadLocal;

import javax.servlet.http.HttpServletResponse;

/**
 * Collect the HTTP Response code
 */
public class HttpResponseCollector implements Collector {
    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return HttpResponseMatcher.HTTP_RESPONSE_MATCHER.equals(collectorContextMap.getRequestMatcher().getId()) ;
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
