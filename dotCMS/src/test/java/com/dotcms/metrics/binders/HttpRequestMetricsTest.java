package com.dotcms.metrics.binders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.metrics.RequestTracker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Throughput gauges must divide cumulative requests by elapsed time, not a fixed constant. */
class HttpRequestMetricsTest {

    private final MBeanServer server = mock(MBeanServer.class);
    private final RuntimeMXBean runtime = mock(RuntimeMXBean.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private ObjectName processor;
    private HttpRequestMetrics metrics;
    private MockedStatic<ManagementFactory> management;
    private MockedStatic<RequestTracker> tracker;

    /** Uses deterministic uptime and isolated Tomcat counters without a live container. */
    @BeforeEach
    void setUp() throws Exception {
        processor = new ObjectName("Catalina:type=GlobalRequestProcessor,name=http-nio-8080");
        when(server.queryNames(any(ObjectName.class), isNull())).thenReturn(Set.of(processor));
        when(server.getAttribute(processor, "requestCount")).thenReturn(600L);
        when(runtime.getUptime()).thenReturn(60000L);
        management = mockStatic(ManagementFactory.class);
        management.when(ManagementFactory::getPlatformMBeanServer).thenReturn(server);
        management.when(ManagementFactory::getRuntimeMXBean).thenReturn(runtime);
        tracker = mockStatic(RequestTracker.class);
        tracker.when(RequestTracker::getInstance).thenReturn(mock(RequestTracker.class));
        metrics = new HttpRequestMetrics();
        metrics.bindTo(registry);
    }

    /** Restores global factories and removes meters between tests. */
    @AfterEach
    void tearDown() {
        registry.close();
        tracker.close();
        management.close();
    }

    /** Six hundred requests in one minute means ten per second and six hundred per minute. */
    @Test
    void throughput_usesElapsedTimeAndFallsWhenTrafficStops() {
        assertEquals(10.0, requestsPerSecond(), 0.001);
        assertEquals(600.0, requestsPerMinute(), 0.001);

        when(runtime.getUptime()).thenReturn(120000L);

        assertEquals(5.0, requestsPerSecond(), 0.001);
        assertEquals(300.0, requestsPerMinute(), 0.001);
    }

    /** Aggregate throughput includes every processor while each tagged rate stays isolated. */
    @Test
    void throughput_aggregatesProcessors() throws Exception {
        final ObjectName other = new ObjectName(
                "Catalina:type=GlobalRequestProcessor,name=http-nio-8082");
        when(server.queryNames(any(ObjectName.class), isNull())).thenReturn(Set.of(processor, other));
        when(server.getAttribute(other, "requestCount")).thenReturn(300L);

        assertEquals(10.0, requestsPerSecond(), 0.001);
        assertEquals(900.0, requestsPerMinute(), 0.001);
    }

    /** Startup with zero elapsed time must not expose an infinite or fabricated rate. */
    @Test
    void throughput_zeroUptimeReportsZero() {
        when(runtime.getUptime()).thenReturn(0L);

        assertEquals(0.0, requestsPerSecond(), 0.001);
        assertEquals(0.0, requestsPerMinute(), 0.001);
    }

    /** An idle processor reports zero in both units. */
    @Test
    void throughput_withoutRequestsReportsZero() throws Exception {
        when(server.getAttribute(processor, "requestCount")).thenReturn(0L);

        assertEquals(0.0, requestsPerSecond(), 0.001);
        assertEquals(0.0, requestsPerMinute(), 0.001);
    }

    /** Reads the public per-processor gauge rather than a private calculation helper. */
    private double requestsPerSecond() {
        return registry.get("dotcms.http.requests.rate.rps")
                .tag("processor", "http-nio-8080").gauge().value();
    }

    /** Reads the public aggregate requests-per-minute gauge. */
    private double requestsPerMinute() {
        return registry.get("dotcms.http.performance.throughput_rpm").gauge().value();
    }
}
