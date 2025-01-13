package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotmarketing.beans.Host;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

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

        final PagesCollector pagesCollector = new PagesCollector(Mockito.mock(HTMLPageAssetAPI.class), Mockito.mock(URLMapAPIImpl.class));
        final Host host = Mockito.mock(Host.class);
        Mockito.when(host.getIdentifier()).thenReturn("1");
        Mockito.when(host.getHostname()).thenReturn("www.dotcms.com");
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

        Assert.assertEquals("/test-path", collectorPayloadBean.get(Collector.URL));
        Assert.assertEquals("www.dotcms.com", collectorPayloadBean.get(Collector.SITE_NAME));
        Assert.assertEquals("en", collectorPayloadBean.get(Collector.LANGUAGE));
        Assert.assertEquals(EventType.PAGE_REQUEST.getType(), collectorPayloadBean.get(Collector.EVENT_TYPE));
        Assert.assertEquals("1", collectorPayloadBean.get(Collector.SITE_ID));
    }
}
