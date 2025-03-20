package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotmarketing.beans.Host;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Test for the CustomerEventCollector class
 * @author jsanca
 *
 */
public class CustomerEventCollectorTest {

    /**
     * Method to test: CustomerEventCollector#collect
     * Given Scenario: Fill the preconditions and check the collect bean is properly filled
     * ExpectedResult: the CollectorPayloadBean should be filled properly
     */
    @Test
    public void test_collect_easy_path() throws IOException {

        final CustomerEventCollector customerEventCollector = new CustomerEventCollector();
        final Host host = new Host();
        host.setIdentifier("1");
        host.setHostname("www.dotcms.com");
        final CollectorContextMap collectorContextMap = new CollectorContextMap() {
            @Override
            public Object get(final String key) {
                switch (key) {
                    case "uri":
                        return "/test-path";
                    case "host":
                        return "www2.dotcms.com";
                    case "currentHost":
                        return host;
                    case "lang":
                        return "en";
                    case "eventType":
                        return null;
                }
                return null;
            }

            @Override
            public RequestMatcher getRequestMatcher() {
                return null;
            }
        };

        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBean();
        customerEventCollector.collect(collectorContextMap, collectorPayloadBean);

        Assert.assertEquals("/test-path", collectorPayloadBean.get(Collector.URL));
        Assert.assertEquals("www.dotcms.com", collectorPayloadBean.get(Collector.SITE_NAME));
        Assert.assertEquals("en", collectorPayloadBean.get(Collector.LANGUAGE));
        Assert.assertEquals("1", collectorPayloadBean.get(Collector.SITE_ID));
        Assert.assertEquals(EventType.CUSTOM_USER_EVENT.getType(), collectorPayloadBean.get(Collector.EVENT_TYPE));
    }
}
