package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.vanityurl.filters.VanityUrlRequestWrapper;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.filters.Constants;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This synchronized collector that collects the vanities
 * @author jsanca
 */
public class SyncVanitiesCollector implements Collector {


    public SyncVanitiesCollector() {
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return VanitiesRequestMatcher.VANITIES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }


    @Override
    public CollectionCollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectionCollectorPayloadBean collectionCollectorPayloadBean) {

        final CollectorPayloadBean collectorPayloadBean = collectionCollectorPayloadBean.first();
        if (null != collectorContextMap.get("request")) {

            final HttpServletRequest request = (HttpServletRequest)collectorContextMap.get("request");
            final String vanityUrl = (String)request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE);
            final String vanityQueryString = (String)request.getAttribute(Constants.CMS_FILTER_QUERY_STRING_OVERRIDE);
            if (request instanceof VanityUrlRequestWrapper) {
                final VanityUrlRequestWrapper vanityRequest = (VanityUrlRequestWrapper) request;
                collectorPayloadBean.put("response_code", vanityRequest.getResponseCode());
            }

            if (Objects.nonNull(vanityUrl)) {

                collectorPayloadBean.put("vanity_url", vanityUrl);
            }

            if (Objects.nonNull(vanityQueryString)) {

                collectorPayloadBean.put("vanity_query_string", vanityQueryString);
            }

        }

        final String uri = (String)collectorContextMap.get("uri");
        final String siteId = (String)collectorContextMap.get("host");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final CachedVanityUrl cachedVanityUrl = (CachedVanityUrl)collectorContextMap.get(Constants.VANITY_URL_OBJECT);
        final HashMap<String, String> vanityObject = new HashMap<>();

        if (Objects.nonNull(cachedVanityUrl)) {

            vanityObject.put("id", cachedVanityUrl.vanityUrlId);
            vanityObject.put("vanity_url", cachedVanityUrl.url);
            vanityObject.put("path", uri);
        }

        collectorPayloadBean.put("object",  vanityObject);
        collectorPayloadBean.put("path", uri);
        collectorPayloadBean.put("event_type", EventType.VANITY_REQUEST.getType());
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("site", siteId);

        return collectionCollectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
