package com.dotcms.analytics.metrics;

import com.dotcms.UnitTestBase;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.metrics.MetricsAPI;
import com.dotcms.analytics.metrics.MetricsAPIImpl;
import com.dotcms.analytics.metrics.MetricsPayloadRequest;
import com.dotcms.analytics.metrics.MetricsSender;
import com.dotcms.analytics.model.AnalyticsProperties;
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
     * Test method for {@link MetricsAPIImpl#sendMetrics(MetricsPayloadRequest)}.
     */
    @Test
    public void testSendMetrics() {
        metricsAPI.subscribeToMetrics("sender", metricsSender);
        final MetricsPayloadRequest request = buildRequest();

        metricsAPI.sendMetrics(request);
        verify(metricsSender).sendMetrics(request);
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(MetricsPayloadRequest)} from a {@link AnalyticsApp} instance.
     */
    @Test
    public void testSendMetrics_fromApp() {
        metricsAPI.subscribeToMetrics("sender", metricsSender);
        final MetricsPayloadRequest request = buildRequestFromApp();

        metricsAPI.sendMetrics(request);
        verify(metricsSender).sendMetrics(request);
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(MetricsPayloadRequest)} without senders subscribed.
     */
    @Test
    public void testSendMetrics_unsubscribe() {
        metricsAPI.unsubscribeFromMetrics("sender");
        final MetricsPayloadRequest request = buildRequest();

        metricsAPI.sendMetrics(request);
        verify(metricsSender, never()).sendMetrics(request);
    }

    private static MetricsPayloadRequest buildRequest() {
        return MetricsAPI.createMetricsRequest("www.some-url.io", "{}", "some-token");
    }

    private static MetricsPayloadRequest buildRequestFromApp() {
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
