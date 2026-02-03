package com.dotcms.telemetry.collectors;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Integration test to verify that MetricStatsCollector does not leak database connections.
 *
 * <p>Tests the fix for issue #34480 where executor thread connections were never returned
 * to the HikariCP pool, causing connection exhaustion over time.</p>
 *
 * <p>This test verifies that:
 * <ul>
 *   <li>Database connections are properly closed after metric collection</li>
 *   <li>Active connection count returns to baseline after collection</li>
 *   <li>Multiple metric collections don't cause connection accumulation</li>
 *   <li>DbConnectionFactory.wrapConnection() properly manages connection lifecycle</li>
 * </ul>
 * </p>
 *
 * @see MetricStatsCollector
 * @see DBMetricType
 */
public class MetricStatsCollectorConnectionLeakTest {

    private static HikariPoolMXBean poolProxy;

    @BeforeClass
    public static void prepare() throws Exception {
        // Initialize integration test environment
        IntegrationTestInitService.getInstance().init();

        // Initialize HikariCP pool proxy for monitoring
        poolProxy = initializeHikariPoolProxy();

        if (poolProxy == null) {
            Assert.fail("HikariCP pool proxy could not be initialized - cannot run connection leak test");
        }
    }

    /**
     * Method to test: {@link MetricStatsCollector#getStats()}
     * Given Scenario: Collects metrics multiple times and monitors HikariCP connection pool
     * ExpectedResult: Active connections return to baseline, no connection accumulation
     *
     * <p>This test validates the fix for issue #34480 by ensuring that DBMetricType.getValue()
     * properly wraps database queries in DbConnectionFactory.wrapConnection(), which guarantees
     * connection cleanup even when queries run on executor threads.</p>
     */
    @Test
    public void test_getStats_doesNotLeakConnections() throws Exception {
        final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);

        // Get baseline connection counts before any metric collection
        final int baselineTotal = poolProxy.getTotalConnections();
        final int baselineIdle = poolProxy.getIdleConnections();
        final int baselineActive = poolProxy.getActiveConnections();

        Logger.info(this, String.format(
            "Baseline connections - Total: %d, Active: %d, Idle: %d",
            baselineTotal, baselineActive, baselineIdle));

        // Collect metrics multiple times to verify no accumulation
        final int iterations = 5;
        for (int i = 0; i < iterations; i++) {
            Logger.info(this, String.format("Metric collection iteration %d of %d", i + 1, iterations));

            // Collect metrics
            collector.getStats();

            // Allow brief settling time for connections to return to pool
            Thread.sleep(500);

            // Check connection counts after collection
            final int totalAfter = poolProxy.getTotalConnections();
            final int idleAfter = poolProxy.getIdleConnections();
            final int activeAfter = poolProxy.getActiveConnections();

            Logger.info(this, String.format(
                "After iteration %d - Total: %d, Active: %d, Idle: %d",
                i + 1, totalAfter, activeAfter, idleAfter));

            // Verify active connections return to baseline (allow small variance for concurrent activity)
            Assert.assertTrue(
                String.format(
                    "Active connections did not return to baseline after iteration %d. " +
                    "Baseline: %d, Current: %d. This indicates a connection leak.",
                    i + 1, baselineActive, activeAfter),
                activeAfter <= baselineActive + 2  // Allow 2 connection variance for concurrent activity
            );

            // Verify total connections don't grow unbounded
            Assert.assertTrue(
                String.format(
                    "Total connections increased beyond acceptable threshold after iteration %d. " +
                    "Baseline: %d, Current: %d. This indicates connections are not being returned to pool.",
                    i + 1, baselineTotal, totalAfter),
                totalAfter <= baselineTotal + 5  // Allow some pool growth but not unbounded
            );
        }

        // Final verification - connection counts should be stable
        final int finalTotal = poolProxy.getTotalConnections();
        final int finalIdle = poolProxy.getIdleConnections();
        final int finalActive = poolProxy.getActiveConnections();

        Logger.info(this, String.format(
            "Final connections after %d iterations - Total: %d, Active: %d, Idle: %d",
            iterations, finalTotal, finalActive, finalIdle));

        // Assert no significant connection accumulation occurred
        Assert.assertTrue(
            String.format(
                "Connection leak detected: Active connections accumulated over %d iterations. " +
                "Baseline: %d, Final: %d",
                iterations, baselineActive, finalActive),
            finalActive <= baselineActive + 2
        );

        Assert.assertTrue(
            String.format(
                "Connection pool growth detected: Total connections increased significantly. " +
                "Baseline: %d, Final: %d",
                baselineTotal, finalTotal),
            finalTotal <= baselineTotal + 5
        );
    }

    /**
     * Method to test: {@link MetricStatsCollector#getStats()}
     * Given Scenario: Collects metrics with cache bypass multiple times
     * ExpectedResult: Connections are properly managed even with cache bypass
     *
     * <p>Cache bypass forces fresh metric calculation on every call, making this a more
     * aggressive test of connection management under high query load.</p>
     */
    @Test
    public void test_getStats_withCacheBypass_doesNotLeakConnections() throws Exception {
        final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);

        // Get baseline
        final int baselineTotal = poolProxy.getTotalConnections();
        final int baselineActive = poolProxy.getActiveConnections();

        Logger.info(this, String.format(
            "Baseline (cache bypass test) - Total: %d, Active: %d",
            baselineTotal, baselineActive));

        // Collect with cache bypass (forces fresh calculation every time)
        final int iterations = 3;
        for (int i = 0; i < iterations; i++) {
            Logger.info(this, String.format(
                "Cache bypass iteration %d of %d", i + 1, iterations));

            // Bypass cache to force fresh metric calculation
            collector.getStats(java.util.Set.of(), null, true);

            Thread.sleep(500);

            final int activeAfter = poolProxy.getActiveConnections();
            final int totalAfter = poolProxy.getTotalConnections();

            Logger.info(this, String.format(
                "After cache bypass iteration %d - Total: %d, Active: %d",
                i + 1, totalAfter, activeAfter));

            Assert.assertTrue(
                String.format(
                    "Connection leak with cache bypass at iteration %d. " +
                    "Baseline active: %d, Current active: %d",
                    i + 1, baselineActive, activeAfter),
                activeAfter <= baselineActive + 3  // Slightly higher tolerance for cache bypass
            );
        }

        final int finalActive = poolProxy.getActiveConnections();
        Logger.info(this, String.format(
            "Final connections after cache bypass test - Active: %d (baseline: %d)",
            finalActive, baselineActive));

        Assert.assertTrue(
            "Connection leak detected in cache bypass test",
            finalActive <= baselineActive + 3
        );
    }

    /**
     * Method to test: {@link MetricStatsCollector#getStats()}
     * Given Scenario: Monitors waiting threads metric to detect pool exhaustion
     * ExpectedResult: No threads waiting for connections (would indicate pool exhaustion)
     */
    @Test
    public void test_getStats_noThreadsAwaitingConnection() throws Exception {
        final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);

        // Collect metrics multiple times
        for (int i = 0; i < 3; i++) {
            collector.getStats();
            Thread.sleep(500);

            final int threadsWaiting = poolProxy.getThreadsAwaitingConnection();

            Logger.info(this, String.format(
                "Iteration %d - Threads awaiting connection: %d", i + 1, threadsWaiting));

            // If threads are waiting, the pool is exhausted (likely due to leaked connections)
            Assert.assertEquals(
                String.format(
                    "Threads are waiting for connections at iteration %d. " +
                    "This indicates connection pool exhaustion due to leaked connections.",
                    i + 1),
                0, threadsWaiting
            );
        }
    }

    /**
     * Initialize HikariCP pool proxy for JMX monitoring.
     *
     * @return HikariPoolMXBean proxy for monitoring connection pool metrics
     */
    private static HikariPoolMXBean initializeHikariPoolProxy() {
        try {
            final HikariDataSource dataSource = (HikariDataSource) DbConnectionFactory.getDataSource();
            if (dataSource == null) {
                Logger.warn(MetricStatsCollectorConnectionLeakTest.class,
                    "HikariDataSource not available for connection leak test");
                return null;
            }

            String poolName = dataSource.getPoolName();
            if (poolName == null) {
                poolName = "HikariPool-1"; // Default pool name
            }

            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");

            Logger.info(MetricStatsCollectorConnectionLeakTest.class,
                "Initialized HikariCP pool proxy for monitoring: " + poolName);

            return JMX.newMXBeanProxy(mBeanServer, objectName, HikariPoolMXBean.class);

        } catch (Exception e) {
            Logger.error(MetricStatsCollectorConnectionLeakTest.class,
                "Failed to initialize HikariCP pool proxy: " + e.getMessage(), e);
            return null;
        }
    }
}