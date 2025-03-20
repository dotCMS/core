package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.liferay.util.StringPool;

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

        final String uri = (String)collectorContextMap.get(CollectorContextMap.URI);
        final String host = (String)collectorContextMap.get(CollectorContextMap.HOST);
        final Host site = (Host) collectorContextMap.get(CollectorContextMap.CURRENT_HOST);
        final String language = (String)collectorContextMap.get(CollectorContextMap.LANG);
        final Long languageId = (Long)collectorContextMap.get(CollectorContextMap.LANG_ID);

        collectorPayloadBean.put(URL, uri);
        collectorPayloadBean.put(SITE_NAME, Objects.nonNull(site)?site.getHostname():host);
        collectorPayloadBean.put(LANGUAGE, language);
        collectorPayloadBean.put(LANGUAGE_ID, languageId);
        collectorPayloadBean.put(SITE_ID, null != site?site.getIdentifier():StringPool.UNKNOWN);
        final String eventType = collectorContextMap.get(CollectorContextMap.EVENT_TYPE) == null?
                EventType.CUSTOM_USER_EVENT.getType():(String)collectorContextMap.get(CollectorContextMap.EVENT_TYPE);
        collectorPayloadBean.put(EVENT_TYPE, eventType);

        return collectorPayloadBean;
    }
}
