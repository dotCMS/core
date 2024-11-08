package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Test for the {@link PagesCollector} class
 * @author jsanca
 *
 */
public class PagesCollectorTest {

    /**
     * Method to test: PagesCollector#collect
     * Given Scenario: Fill the preconditions and check the collect bean is properly filled
     * ExpectedResult: the CollectorPayloadBean should be filled properly
     */
    @Test
    public void test_collect_easy_path() throws IOException {

        final PagesCollector pagesCollector = new PagesCollector();
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
                    case "langId":
                        return null;
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
        pagesCollector.collect(collectorContextMap, collectorPayloadBean);

        Assert.assertEquals("/test-path", collectorPayloadBean.get("url"));
        Assert.assertEquals("www.dotcms.com", collectorPayloadBean.get("host"));
        Assert.assertEquals("en", collectorPayloadBean.get("language"));
        Assert.assertEquals(EventType.PAGE_REQUEST.getType(), collectorPayloadBean.get("event_type"));
    }
}
