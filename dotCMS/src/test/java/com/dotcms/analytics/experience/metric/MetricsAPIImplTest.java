package com.dotcms.analytics.experience.metric;

import com.dotcms.UnitTestBase;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AnalyticsProperties;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.dotmarketing.portlets.workflows.model.WorkflowState.*;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.UNPUBLISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricsAPIImplTest extends UnitTestBase {

    private MetricsAPI metricsAPI;
    private MetricsSender metricsSender;
    private AnalyticsApp analyticsApp;

    @Before
    public void setUp() {
        metricsAPI = new MetricsAPIImpl();
        metricsSender = mock(MetricsSender.class);
        analyticsApp = mock(AnalyticsApp.class);
        final AnalyticsProperties analyticsProperties = AnalyticsProperties.builder()
                .clientId("clientId")
                .clientSecret("clientSecret")
                .analyticsKey("analyticsKey")
                .analyticsConfigUrl("http://localhost:8088/c/customer1/cluster1/keys")
                .analyticsWriteUrl("http://localhost:8081/api/v1/event")
                .analyticsReadUrl("http://localhost:5000")
                .build();
        when(analyticsApp.getAnalyticsProperties()).thenReturn(analyticsProperties);
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(AnalyticsApp, String)}.
     */
    @Test
    public void testSendMetrics() {
        metricsAPI.subscribeToMetrics("sender", metricsSender);
        metricsAPI.sendMetrics(analyticsApp, "{}");
        verify(metricsSender).sendMetrics(any(AnalyticsAppPayload.class));
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(AnalyticsApp, String)} from a {@link AnalyticsApp} instance.
     */
    @Test
    public void testSendMetrics_fromApp() {
        metricsAPI.subscribeToMetrics("sender", metricsSender);
        metricsAPI.sendMetrics(analyticsApp, "{}");
        verify(metricsSender).sendMetrics(any(AnalyticsAppPayload.class));
    }

    /**
     * Test method for {@link MetricsAPIImpl#sendMetrics(AnalyticsApp, String)} without senders subscribed.
     */
    @Test
    public void testSendMetrics_unsubscribe() {
        metricsAPI.unsubscribeFromMetrics("sender");
        metricsAPI.sendMetrics(analyticsApp, "{}");
        verify(metricsSender, never()).sendMetrics(any(AnalyticsAppPayload.class));
    }

}
