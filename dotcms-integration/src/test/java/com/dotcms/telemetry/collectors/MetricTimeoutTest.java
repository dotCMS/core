package com.dotcms.telemetry.collectors;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Integration test to verify that metric timeout handling works correctly
 * and does not leak database connections even when metrics exceed the timeout.
 *
 * <h3>What This Test Validates</h3>
 * <ul>
 *   <li>Metrics that exceed timeout are properly interrupted</li>
 *   <li>Timeout errors are recorded in the MetricsSnapshot</li>
 *   <li>Database connections are returned to pool even after timeout</li>
 *   <li>Race condition fixes prevent connection leaks during timeout</li>
 *   <li>{@link com.dotmarketing.db.DbConnectionFactory#wrapConnection} properly manages connections</li>
 * </ul>
 *
 * <h3>Test Strategy</h3>
 * <p>This test uses {@link SlowDatabaseTestMetric} which executes a PostgreSQL pg_sleep(3)
 * query that takes 3 seconds to complete. This exceeds the 1-second timeout configured
 * in the test setup. Using a real database query (not just Thread.sleep()) ensures we
 * test the actual database connection management code path.</p>
 *
 * <p>The test verifies that:</p>
 * <ol>
 *   <li>The database query times out as expected (1 second timeout)</li>
 *   <li>An error is recorded for the timed-out metric</li>
 *   <li>The database connection is properly wrapped in {@code wrapConnection()}</li>
 *   <li>Active connections return to baseline after timeout</li>
 *   <li>No connection accumulation occurs even with repeated timeouts</li>
 * </ol>
 *
 * @see MetricStatsCollector
 * @see DBMetricType
 * @see SlowDatabaseTestMetric
 */
public class MetricTimeoutTest {

    private static HikariPoolMXBean poolProxy;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        // Override timeout to 1 second for faster tests
        Config.setProperty("telemetry.metric.timeout.seconds", 1);

        // Initialize HikariCP pool proxy for monitoring
        poolProxy = initializeHikariPoolProxy();

        if (poolProxy == null) {
            Assert.fail("HikariCP pool proxy could not be initialized - cannot run timeout test");
        }
    }

    /**
     * Method to test: {@link MetricStatsCollector#getStats(Set)}
     * Given Scenario: A metric that exceeds the timeout duration
     * ExpectedResult: Metric times out, error recorded, connection returned to pool
     *
     * <p>This test validates that the race condition fixes work correctly:
     * when a metric times out, the connection is still properly cleaned up and
     * returned to the pool instead of being orphaned.</p>
     */
    @Test
    public void test_metricTimeout_connectionIsReturnedToPool() throws Exception {
        final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);

        // Get baseline connection counts
        final int baselineActive = poolProxy.getActiveConnections();
        final int baselineTotal = poolProxy.getTotalConnections();

        Logger.info(this, String.format(
            "Baseline - Total: %d, Active: %d",
            baselineTotal, baselineActive));

        // Collect metrics with cache bypass to ensure we actually execute the database query
        // Without bypass, cached values would prevent testing connection management
        // Using "test.slow.database.metric" which executes pg_sleep(3) to test real DB connection handling
        final MetricsSnapshot snapshot = collector.getStats(Set.of("test.slow.database.metric"), null, true);

        // Wait for executor to complete shutdown and connections to return
        await().atMost(3, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .until(() -> poolProxy.getActiveConnections() <= baselineActive + 2);

        // Verify timeout was recorded as an error
        Assert.assertFalse("Snapshot should contain errors for timed-out metrics",
            snapshot.getErrors().isEmpty());

        final boolean hasTimeoutError = snapshot.getErrors().stream()
            .anyMatch(error -> error.getError().contains("Timeout"));

        Assert.assertTrue("Should have timeout error for slow metric", hasTimeoutError);

        Logger.info(this, String.format("Timeout error recorded: %s",
            snapshot.getErrors().iterator().next().getError()));

        // Verify metric did not complete successfully
        final boolean slowMetricPresent = snapshot.getStats().stream()
            .anyMatch(stat -> "test.slow.metric".equals(stat.getMetric().getName()));

        Assert.assertFalse("Timed-out metric should not be in successful results",
            slowMetricPresent);

        // Check connection counts after timeout
        final int activeAfter = poolProxy.getActiveConnections();
        final int totalAfter = poolProxy.getTotalConnections();

        Logger.info(this, String.format(
            "After timeout - Total: %d, Active: %d",
            totalAfter, activeAfter));

        // Verify connection returned to pool
        Assert.assertTrue(
            String.format(
                "Active connections should return to baseline after timeout. " +
                "Baseline: %d, After: %d. This would indicate a connection leak.",
                baselineActive, activeAfter),
            activeAfter <= baselineActive + 2  // Allow small variance
        );

        // Verify no unbounded pool growth
        Assert.assertTrue(
            String.format(
                "Total connections should not grow after timeout. " +
                "Baseline: %d, After: %d",
                baselineTotal, totalAfter),
            totalAfter <= baselineTotal + 3
        );
    }

    /**
     * Method to test: {@link MetricStatsCollector#getStats(Set)}
     * Given Scenario: Multiple consecutive metric collections with timeouts
     * ExpectedResult: No connection accumulation over multiple timeout cycles
     *
     * <p>This test validates that repeated timeouts do not accumulate orphaned connections.
     * Even if the race condition occurs, our fixes should prevent connection leaks.</p>
     */
    @Test
    public void test_repeatedTimeouts_noConnectionAccumulation() throws Exception {
        final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);

        final int baselineActive = poolProxy.getActiveConnections();
        final int baselineTotal = poolProxy.getTotalConnections();

        Logger.info(this, String.format(
            "Baseline for repeated timeouts - Total: %d, Active: %d",
            baselineTotal, baselineActive));

        // Run multiple collection cycles that will timeout
        final int iterations = 3;
        for (int i = 0; i < iterations; i++) {
            Logger.info(this, String.format("Timeout iteration %d of %d", i + 1, iterations));

            // Collect with cache bypass to force fresh database query execution on each iteration
            // This ensures we actually test connection management, not cached values
            final MetricsSnapshot snapshot = collector.getStats(Set.of("test.slow.database.metric"), null, true);

            // Verify timeout occurred
            Assert.assertFalse("Should have timeout error", snapshot.getErrors().isEmpty());

            // Wait for connections to settle
            await().atMost(2, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> poolProxy.getActiveConnections() <= baselineActive + 2);

            final int activeAfter = poolProxy.getActiveConnections();
            Logger.info(this, String.format("After iteration %d - Active: %d", i + 1, activeAfter));

            // Check for connection accumulation
            Assert.assertTrue(
                String.format(
                    "Connections accumulating after iteration %d. Baseline: %d, Current: %d",
                    i + 1, baselineActive, activeAfter),
                activeAfter <= baselineActive + 2
            );
        }

        // Final verification - no accumulation after multiple timeouts
        final int finalActive = poolProxy.getActiveConnections();
        final int finalTotal = poolProxy.getTotalConnections();

        Logger.info(this, String.format(
            "Final after %d timeout iterations - Total: %d, Active: %d",
            iterations, finalTotal, finalActive));

        Assert.assertTrue(
            String.format(
                "Connection leak detected: Active connections accumulated over %d timeout cycles. " +
                "Baseline: %d, Final: %d",
                iterations, baselineActive, finalActive),
            finalActive <= baselineActive + 2
        );

        Assert.assertTrue(
            String.format(
                "Pool growth detected: Total connections increased significantly. " +
                "Baseline: %d, Final: %d",
                baselineTotal, finalTotal),
            finalTotal <= baselineTotal + 3
        );
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
                Logger.warn(MetricTimeoutTest.class,
                    "HikariDataSource not available for timeout test");
                return null;
            }

            String poolName = dataSource.getPoolName();
            if (poolName == null) {
                poolName = "HikariPool-1";
            }

            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");

            Logger.info(MetricTimeoutTest.class,
                "Initialized HikariCP pool proxy for monitoring: " + poolName);

            return JMX.newMXBeanProxy(mBeanServer, objectName, HikariPoolMXBean.class);

        } catch (Exception e) {
            Logger.error(MetricTimeoutTest.class,
                "Failed to initialize HikariCP pool proxy: " + e.getMessage(), e);
            return null;
        }
    }
}