package com.dotcms.analytics.track.collectors;

import com.dotcms.UnitTestBase;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Test for the FilesCollector class
 * @author jsanca
 *
 */
public class FilesCollectorTest extends UnitTestBase {

    /**
     * Method to test: FilesCollector#collect
     * Given Scenario: Fill the preconditions and check the collect bean is properly filled
     * ExpectedResult: the CollectorPayloadBean should be filled properly
     */
    @Test
    public void test_collect_easy_path() throws IOException {

        final FilesCollector filesCollector = new FilesCollector() {
            @Override
            protected Optional<Contentlet> getFileAsset(String uri, Host host, Long languageId) {

                final Contentlet contentlet = Mockito.mock(Contentlet.class);
                Mockito.when(contentlet.getIdentifier()).thenReturn("1");
                Mockito.when(contentlet.getTitle()).thenReturn("Test");
                try {
                Mockito.when(contentlet.isLive()).thenReturn(true);
                Mockito.when(contentlet.isWorking()).thenReturn(true);
                } catch (Exception e) {

                }
                final ContentType contentType = Mockito.mock(ContentType.class);
                Mockito.when(contentlet.getContentType()).thenReturn(contentType);
                Mockito.when(contentType.id()).thenReturn("1");
                Mockito.when(contentType.name()).thenReturn("file");
                Mockito.when(contentType.variable()).thenReturn("file");
                final BaseContentType baseType = Mockito.mock(BaseContentType.class);
                Mockito.when(contentType.baseType()).thenReturn(baseType);
                Mockito.when(baseType.name()).thenReturn("file");
                return Optional.ofNullable(contentlet);
            }
        };
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
                        return 1L;
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
        filesCollector.collect(collectorContextMap, collectorPayloadBean);

        final Map<String, String> fileObject = (Map<String, String>) collectorPayloadBean.get("object");
        Assert.assertNotNull(fileObject);
        Assert.assertEquals("1", fileObject.get(Collector.ID));
        Assert.assertEquals("Test", fileObject.get(Collector.TITLE));
        Assert.assertEquals("/test-path", fileObject.get(Collector.URL));
        Assert.assertEquals("www.dotcms.com", collectorPayloadBean.get(Collector.SITE_NAME));
        Assert.assertEquals("en", collectorPayloadBean.get(Collector.LANGUAGE));
        Assert.assertEquals("1", collectorPayloadBean.get(Collector.SITE_ID));
        Assert.assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(Collector.EVENT_TYPE));
    }
}
