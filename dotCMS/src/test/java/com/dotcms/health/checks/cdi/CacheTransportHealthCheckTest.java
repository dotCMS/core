package com.dotcms.health.checks.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.NullTransport;
import com.dotmarketing.util.Config;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link CacheTransportHealthCheck}, the probe added by issue #36803 to surface a
 * cache transport that is silently dropping cluster invalidations.
 *
 * The transport and the rewire counter are both reached through statics
 * ({@code CacheLocator.getCacheAdministrator()} and {@code ClusterFactory.getRewireFailures()}),
 * so they are mocked statically here, in the same style as {@link VelocityHealthCheckTest}.
 *
 * Tests run the check in PRODUCTION mode unless they are specifically about monitor mode, so an
 * unhealthy verdict shows up as DOWN rather than being converted to DEGRADED.
 */
public class CacheTransportHealthCheckTest {

    private static final String MODE_KEY = "health.check.cache-transport.mode";
    private static final String GRACE_KEY =
            "health.check.cache-transport.initialization-grace-period-seconds";
    private static final String THRESHOLD_KEY =
            "health.check.cache-transport.rewire-failure-threshold";

    private MockedStatic<CacheLocator> cacheLocatorMock;
    private MockedStatic<ClusterFactory> clusterFactoryMock;

    @Before
    public void setUp() {
        cacheLocatorMock = mockStatic(CacheLocator.class);
        clusterFactoryMock = mockStatic(ClusterFactory.class);
        clusterFactoryMock.when(ClusterFactory::getRewireFailures).thenReturn(0L);
        Config.setProperty(MODE_KEY, "PRODUCTION");
    }

    @After
    public void tearDown() {
        cacheLocatorMock.close();
        clusterFactoryMock.close();
        // Restored to the values the production code defaults to, rather than cleared: Config has
        // no clear-for-tests hook, and leaving these set would follow the JVM fork into other
        // test classes.
        Config.setProperty(MODE_KEY, "MONITOR_MODE");
        Config.setProperty(GRACE_KEY, "120");
        Config.setProperty(THRESHOLD_KEY, "3");
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: the node is configured with {@link NullTransport}, the deliberate no-op
     * transport for a node that is not part of a cluster.
     * Expected Result: healthy. There is nothing that could be dropping invalidations, and the
     * per-transport fields are omitted rather than reported as zeros.
     */
    @Test
    public void nullTransportReportsUp() {
        withTransport(new NullTransport());

        final HealthCheckResult result = new CacheTransportHealthCheck().check();

        assertEquals(HealthStatus.UP, result.status());
        final Map<String, Object> data = data(result);
        assertEquals("NullTransport", data.get("transport"));
        assertFalse("a no-op transport has no invalidation state worth reporting",
                data.containsKey("initialized"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: the cache layer cannot be resolved at all, which is what a probe sees when
     * it runs before the cache subsystem is up.
     * Expected Result: healthy, not an exception and not a false alarm.
     */
    @Test
    public void unresolvableCacheLayerReportsUp() {
        cacheLocatorMock.when(CacheLocator::getCacheAdministrator)
                .thenThrow(new IllegalStateException("cache layer not up yet"));

        final HealthCheckResult result = new CacheTransportHealthCheck().check();

        assertEquals(HealthStatus.UP, result.status());
        assertEquals("none", data(result).get("transport"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: a real transport that has never been initialized, polled inside the startup
     * grace period. Every node is in this state for the first seconds of its life, because the
     * transport is initialized late in boot when the cluster is wired.
     * Expected Result: healthy, flagged as awaiting initialization. Reporting DOWN here would turn
     * a normal rolling deployment into a DEGRADED overall status on every pod start.
     */
    @Test
    public void uninitializedTransportWithinGracePeriodReportsUp() {
        Config.setProperty(GRACE_KEY, "120");
        withTransport(transport(false, 7L, 2000L, 0L));

        final HealthCheckResult result = new CacheTransportHealthCheck().check();

        assertEquals(HealthStatus.UP, result.status());
        final Map<String, Object> data = data(result);
        assertEquals(false, data.get("initialized"));
        assertEquals(true, data.get("awaitingInitialization"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: the same never-initialized transport, but the grace period has elapsed.
     * Expected Result: unhealthy. This is the #36803 fault the probe exists to catch -- a node
     * that never wires its transport and drops every invalidation forever.
     */
    @Test
    public void uninitializedTransportPastGracePeriodReportsDown() {
        Config.setProperty(GRACE_KEY, "0");
        withTransport(transport(false, 7L, 2000L, 0L));

        final HealthCheckResult result = new CacheTransportHealthCheck().check();

        assertEquals(HealthStatus.DOWN, result.status());
        assertEquals(true, data(result).get("awaitingInitialization"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: the transport was initialized on an earlier poll and is not anymore, with a
     * long grace period configured.
     * Expected Result: unhealthy immediately. The grace period covers startup only; losing a
     * transport that had been working is a regression, not a node still booting.
     */
    @Test
    public void transportLostAfterInitializationReportsDownDespiteGracePeriod() {
        Config.setProperty(GRACE_KEY, "3600");
        final CacheTransport transport = transport(true, 0L, 0L, 0L);
        withTransport(transport);
        final CacheTransportHealthCheck check = new CacheTransportHealthCheck();

        assertEquals("baseline: a working transport is UP", HealthStatus.UP, check.check().status());

        when(transport.isInitialized()).thenReturn(false);

        final HealthCheckResult result = check.check();
        assertEquals(HealthStatus.DOWN, result.status());
        assertEquals("the grace period must not apply once a transport has been seen working",
                false, data(result).get("awaitingInitialization"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: an initialized transport on a node whose last rewire attempt failed once,
     * below the configured threshold.
     * Expected Result: healthy. {@code testCluster()} can throw on a momentary database hiccup
     * while invalidations keep flowing, so a single stale failure must not report DOWN.
     */
    @Test
    public void rewireFailuresBelowThresholdReportUp() {
        Config.setProperty(THRESHOLD_KEY, "3");
        clusterFactoryMock.when(ClusterFactory::getRewireFailures).thenReturn(1L);
        withTransport(transport(true, 0L, 0L, 0L));

        final HealthCheckResult result = new CacheTransportHealthCheck().check();

        assertEquals(HealthStatus.UP, result.status());
        assertEquals(1L, data(result).get("rewireFailures"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: consecutive rewire failures have reached the threshold while the transport
     * still reports itself initialized.
     * Expected Result: unhealthy -- the transport may be wired to a stale view of the cluster.
     */
    @Test
    public void rewireFailuresAtThresholdReportDown() {
        Config.setProperty(THRESHOLD_KEY, "3");
        clusterFactoryMock.when(ClusterFactory::getRewireFailures).thenReturn(3L);
        withTransport(transport(true, 0L, 0L, 0L));

        final HealthCheckResult result = new CacheTransportHealthCheck().check();

        assertEquals(HealthStatus.DOWN, result.status());
        assertEquals(3L, data(result).get("rewireFailures"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#buildStructuredData}
     * Given Scenario: a healthy transport carrying non-zero counters.
     * Expected Result: every counter monitoring is expected to consume is present, and the drops
     * from before the transport came up are reported separately from the operational ones.
     */
    @Test
    public void structuredDataExposesEveryCounter() {
        clusterFactoryMock.when(ClusterFactory::getRewireFailures).thenReturn(2L);
        withTransport(transport(true, 5L, 2841L, 11L));

        final Map<String, Object> data = data(new CacheTransportHealthCheck().check());

        assertEquals(true, data.get("initialized"));
        assertEquals(5L, data.get("droppedInvalidations"));
        assertEquals("startup drops are reported apart from operational ones, so the alertable"
                + " counter is not permanently dominated by the boot burst",
                2841L, data.get("startupDroppedInvalidations"));
        assertEquals(11L, data.get("failedInvalidations"));
        assertEquals(2L, data.get("rewireFailures"));
        assertEquals(false, data.get("awaitingInitialization"));
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#check()}
     * Given Scenario: a genuinely broken transport, with the check in its default mode.
     * Expected Result: DEGRADED rather than DOWN. The default is MONITOR_MODE precisely so a
     * broken transport cannot fail a readiness probe and drain a node that can still serve
     * traffic; this test pins that deployment-safety property.
     */
    @Test
    public void defaultModeIsMonitorModeAndConvertsDownToDegraded() {
        final CacheTransportHealthCheck check = new CacheTransportHealthCheck();
        assertEquals("the default must stay MONITOR_MODE", HealthCheckMode.MONITOR_MODE,
                check.getDefaultMode());

        Config.setProperty(MODE_KEY, HealthCheckMode.MONITOR_MODE.name());
        Config.setProperty(GRACE_KEY, "0");
        withTransport(transport(false, 12L, 0L, 0L));

        final HealthCheckResult result = check.check();

        assertEquals(HealthStatus.DEGRADED, result.status());
        assertTrue(result.monitorModeApplied());
    }

    /**
     * Method to test: {@link CacheTransportHealthCheck#isLivenessCheck()}
     * Given Scenario: the probe registration flags are read by the health framework.
     * Expected Result: never a liveness check. Restarting pods on transport failure is what
     * amplified the incident in #36544.
     */
    @Test
    public void isNeverALivenessCheck() {
        assertFalse(new CacheTransportHealthCheck().isLivenessCheck());
    }

    private void withTransport(final CacheTransport transport) {
        final DotCacheAdministrator admin = mock(DotCacheAdministrator.class);
        when(admin.getTransport()).thenReturn(transport);
        cacheLocatorMock.when(CacheLocator::getCacheAdministrator).thenReturn(admin);
    }

    private CacheTransport transport(final boolean initialized, final long dropped,
                                     final long startupDropped, final long failed) {
        final CacheTransport transport = mock(CacheTransport.class);
        when(transport.isInitialized()).thenReturn(initialized);
        when(transport.getDroppedMessages()).thenReturn(dropped);
        when(transport.getStartupDroppedMessages()).thenReturn(startupDropped);
        when(transport.getFailedMessages()).thenReturn(failed);
        return transport;
    }

    private Map<String, Object> data(final HealthCheckResult result) {
        return result.data().orElseThrow(
                () -> new AssertionError("the check must always publish structured data"));
    }
}
