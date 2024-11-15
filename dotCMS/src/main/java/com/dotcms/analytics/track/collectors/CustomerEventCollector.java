package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotmarketing.beans.Host;

import java.util.HashMap;
import java.util.Objects;

/**
 * This event collector creator basically allows to send a message for a customer event.
 * These events are fired by rest, wf and rules.
 * @author jsanca
 */
public class CustomerEventCollector implements Collector {
    @Override
    public boolean test(final CollectorContextMap collectorContextMap) {

        return UserCustomDefinedRequestMatcher.USER_CUSTOM_EVENT_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }

    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {
        final String uri = (String)collectorContextMap.get("uri");
        final String host = (String)collectorContextMap.get("host");
        final Host site = (Host) collectorContextMap.get("currentHost");
        final String language = (String)collectorContextMap.get("lang");
        if (Objects.isNull(collectorPayloadBean.get("url"))) {

            collectorPayloadBean.put("url", uri);
        }

        collectorPayloadBean.put("host", host);
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("site", null != site?site.getIdentifier():"unknown");
        final String eventType = (String)collectorContextMap.get("eventType") == null?
                    EventType.CUSTOM_USER_EVENT.getType():(String)collectorContextMap.get("eventType");
        collectorPayloadBean.put("event_type", eventType);

        return collectorPayloadBean;
    }
}
