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
 * This synchronous collector collects information from the Vanity URL that has been processed.
 *
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
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {
        if (null != collectorContextMap.get(CollectorContextMap.REQUEST)) {

            final HttpServletRequest request = (HttpServletRequest)collectorContextMap.get(CollectorContextMap.REQUEST);
            final String vanityUrl = (String)request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE);
            final String vanityQueryString = (String)request.getAttribute(Constants.CMS_FILTER_QUERY_STRING_OVERRIDE);
            if (request instanceof VanityUrlRequestWrapper) {
                final VanityUrlRequestWrapper vanityRequest = (VanityUrlRequestWrapper) request;
                collectorPayloadBean.put(RESPONSE_CODE, vanityRequest.getResponseCode());
            }

            collectorPayloadBean.put(VANITY_URL_KEY, vanityUrl);
            collectorPayloadBean.put(VANITY_QUERY_STRING, vanityQueryString);
        }

        final String uri = (String)collectorContextMap.get(CollectorContextMap.URI);
        final Host site = (Host) collectorContextMap.get(CollectorContextMap.CURRENT_HOST);
        final Long languageId = (Long)collectorContextMap.get(CollectorContextMap.LANG_ID);
        final String language = (String)collectorContextMap.get(CollectorContextMap.LANG);
        final CachedVanityUrl cachedVanityUrl = (CachedVanityUrl)collectorContextMap.get(Constants.VANITY_URL_OBJECT);
        final HashMap<String, String> vanityObject = new HashMap<>();

        if (Objects.nonNull(cachedVanityUrl)) {
            vanityObject.put(ID, cachedVanityUrl.vanityUrlId);
            vanityObject.put(FORWARD_TO, collectorPayloadBean.get(VANITY_URL_KEY) != null
                    ? (String) collectorPayloadBean.get(VANITY_URL_KEY)
                    : cachedVanityUrl.forwardTo);
            vanityObject.put(URL, cachedVanityUrl.url);
            vanityObject.put(RESPONSE, String.valueOf(cachedVanityUrl.response));
        }

        collectorPayloadBean.put(OBJECT,  vanityObject);
        collectorPayloadBean.put(URL, uri);
        collectorPayloadBean.put(LANGUAGE, language);
        collectorPayloadBean.put(LANGUAGE_ID, languageId);
        collectorPayloadBean.put(SITE_ID, site.getIdentifier());
        collectorPayloadBean.put(EVENT_TYPE, EventType.VANITY_REQUEST.getType());

        return collectorPayloadBean;
    }

}
