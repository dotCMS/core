package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.vanityurl.filters.VanityUrlRequestWrapper;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.filters.Constants;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Objects;

/**
 * This synchronized collector that collects the vanities
 * @author jsanca
 */
public class SyncVanitiesCollector implements Collector {


    public static final String VANITY_URL_KEY = "vanity_url";

    public SyncVanitiesCollector() {
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return VanitiesRequestMatcher.VANITIES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }


    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        if (null != collectorContextMap.get("request")) {

            final HttpServletRequest request = (HttpServletRequest)collectorContextMap.get("request");
            final String vanityUrl = (String)request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE);
            final String vanityQueryString = (String)request.getAttribute(Constants.CMS_FILTER_QUERY_STRING_OVERRIDE);
            if (request instanceof VanityUrlRequestWrapper) {
                final VanityUrlRequestWrapper vanityRequest = (VanityUrlRequestWrapper) request;
                collectorPayloadBean.put("response_code", vanityRequest.getResponseCode());
            }

            collectorPayloadBean.put(VANITY_URL_KEY, vanityUrl);
            collectorPayloadBean.put("vanity_query_string", vanityQueryString);
        }

        final String uri = (String)collectorContextMap.get("uri");
        final Host site = (Host) collectorContextMap.get("currentHost");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final CachedVanityUrl cachedVanityUrl = (CachedVanityUrl)collectorContextMap.get(Constants.VANITY_URL_OBJECT);
        final HashMap<String, String> vanityObject = new HashMap<>();

        if (Objects.nonNull(cachedVanityUrl)) {

            vanityObject.put("id", cachedVanityUrl.vanityUrlId);
            vanityObject.put("forward_to",
                    collectorPayloadBean.get(VANITY_URL_KEY)!=null?(String)collectorPayloadBean.get(VANITY_URL_KEY):cachedVanityUrl.forwardTo);
            vanityObject.put("url", uri);
            vanityObject.put("response", String.valueOf(cachedVanityUrl.response));
        }

        collectorPayloadBean.put("object",  vanityObject);
        collectorPayloadBean.put("url", uri);
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("language_id", languageId);
        collectorPayloadBean.put("site", site.getIdentifier());
        collectorPayloadBean.put("event_type", EventType.VANITY_REQUEST.getType());

        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

}
