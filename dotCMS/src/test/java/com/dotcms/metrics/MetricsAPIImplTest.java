package com.dotcms.metrics;

import com.dotcms.UnitTestBase;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AnalyticsProperties;
import com.dotcms.http.request.StringPayloadHttpRequest;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricsAPIImplTest extends UnitTestBase {

    private MetricsAPI metricsAPI;
    private MetricsSender metricsSender;

    @Before
    public void setUp() {
        metricsAPI = new MetricsAPIImpl();
        metricsSender = mock(MetricsSender.class);
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(StringPayloadHttpRequest)}.
     */
    @Test
    public void testSendMetrics() {
        metricsAPI.subscribeToMetrics("sender", metricsSender);
        final StringPayloadHttpRequest request = buildRequest();

        metricsAPI.sendMetrics(request);
        verify(metricsSender).sendMetrics(request);
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(StringPayloadHttpRequest)} from a {@link AnalyticsApp} instance.
     */
    @Test
    public void testSendMetrics_fromApp() {
        metricsAPI.subscribeToMetrics("sender", metricsSender);
        final StringPayloadHttpRequest request = buildRequestFromApp();

        metricsAPI.sendMetrics(request);
        verify(metricsSender).sendMetrics(request);
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(StringPayloadHttpRequest)} without senders subscribed.
     */
    @Test
    public void testSendMetrics_unsubscribe() {
        metricsAPI.unsubscribeFromMetrics("sender");
        final StringPayloadHttpRequest request = buildRequest();

        metricsAPI.sendMetrics(request);
        verify(metricsSender, never()).sendMetrics(request);
    }

    private static StringPayloadHttpRequest buildRequest() {
        return MetricsAPI.createMetricsRequest("www.some-url.io", "{}", "some-token");
    }

    private static StringPayloadHttpRequest buildRequestFromApp() {
        AnalyticsApp app = mock(AnalyticsApp.class);
        final String url = "www.some-url.io";
        final AnalyticsProperties properties = AnalyticsProperties.builder()
            .clientId("some-client-id")
            .clientSecret("some-secret")
            .analyticsKey("some-key")
            .analyticsConfigUrl(url)
            .analyticsWriteUrl(url)
            .analyticsReadUrl(url)
            .build();
        when(app.getAnalyticsProperties()).thenReturn(properties);
        return MetricsAPI.createMetricsRequest(app, "{}");
    }

}
