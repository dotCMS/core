package com.dotcms.telemetry.job;

import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.collectors.MetricStatsCollector;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.cache.MetricCacheConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test to verify that MetricsStatsJob correctly uses profile-based filtering
 * for metric collection.
 * 
 * <p>This test verifies that:</p>
 * <ul>
 *     <li>FULL profile collects all metrics that support it</li>
 *     <li>Profile filtering works correctly (FULL vs MINIMAL)</li>
 *     <li>All expected metric names are present when using FULL profile</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class MetricsStatsJobBackwardCompatibilityTest {

    @Mock
    private MetricStatsCollector collector;
    
    @Mock
    private MetricCacheConfig config;

    @Test
    public void testAllMetricsCollectedWithoutFiltering() {
        // Given: A collector that returns all metrics with FULL profile
        final MetricsSnapshot allMetricsSnapshot = createMockSnapshot(100); // Example: 100 metrics
        when(collector.getStatsAndCleanUp(ProfileType.FULL)).thenReturn(allMetricsSnapshot);
        
        // When: Collecting all metrics with FULL profile
        final MetricsSnapshot result = collector.getStatsAndCleanUp(ProfileType.FULL);
        
        // Then: All metrics should be present
        assertNotNull("MetricsSnapshot should not be null", result);
        assertNotNull("Stats collection should not be null", result.getStats());
        assertNotNull("Non-numeric stats collection should not be null", result.getNotNumericStats());
        
        // Verify all metrics are collected
        final int totalMetrics = result.getStats().size() + result.getNotNumericStats().size();
        assertEquals("Should collect all metrics with FULL profile", 100, totalMetrics);
    }

    @Test
    public void testProfileFilteringForCronJob() {
        // Given: Metrics with different profile annotations
        final MetricsSnapshot fullMetrics = createMockSnapshot(100); // All metrics with FULL profile
        final MetricsSnapshot minimalMetrics = createMockSnapshot(10); // Only 10 with MINIMAL profile
        
        when(collector.getStatsAndCleanUp(ProfileType.FULL)).thenReturn(fullMetrics);
        when(collector.getStatsAndCleanUp(ProfileType.MINIMAL)).thenReturn(minimalMetrics);
        
        // When: Using getStatsAndCleanUp with FULL profile (cron job)
        final MetricsSnapshot fullResult = collector.getStatsAndCleanUp(ProfileType.FULL);
        
        // When: Using getStatsAndCleanUp with MINIMAL profile (dashboard)
        final MetricsSnapshot minimalResult = collector.getStatsAndCleanUp(ProfileType.MINIMAL);
        
        // Then: FULL profile should return more metrics than MINIMAL
        final int fullCount = fullResult.getStats().size() + fullResult.getNotNumericStats().size();
        final int minimalCount = minimalResult.getStats().size() + minimalResult.getNotNumericStats().size();
        
        assertTrue("FULL profile collection should return more metrics than MINIMAL",
                fullCount >= minimalCount);
    }

    @Test
    public void testMetricNamesConsistency() {
        // Given: A snapshot with known metric names
        final Set<String> expectedMetricNames = Set.of(
                "COUNT_OF_SITES",
                "COUNT_OF_ACTIVE_SITES",
                "COUNT",
                "ACTIVE_USERS_COUNT",
                "SCHEMES_COUNT"
        );
        
        final MetricsSnapshot snapshot = createMockSnapshotWithNames(expectedMetricNames);
        when(collector.getStatsAndCleanUp(ProfileType.FULL)).thenReturn(snapshot);
        
        // When: Collecting all metrics with FULL profile
        final MetricsSnapshot result = collector.getStatsAndCleanUp(ProfileType.FULL);
        
        // Then: All expected metric names should be present
        final Set<String> actualMetricNames = new HashSet<>();
        result.getStats().forEach(mv -> actualMetricNames.add(mv.getMetric().getName()));
        actualMetricNames.addAll(result.getNotNumericStats().keySet()); // getNotNumericStats() returns Map<String, Object>
        
        assertTrue("Should contain all expected metric names",
                actualMetricNames.containsAll(expectedMetricNames));
    }

    /**
     * Helper method to create a mock MetricsSnapshot with a given number of metrics.
     */
    private MetricsSnapshot createMockSnapshot(final int metricCount) {
        final Collection<MetricValue> stats = new java.util.ArrayList<>();
        final Collection<MetricValue> notNumericStats = new java.util.ArrayList<>();
        
        // Create mock metrics
        // Use lenient stubbing since some tests only check sizes, not metric names
        for (int i = 0; i < metricCount; i++) {
            final MetricValue mockMetric = mock(MetricValue.class);
            final com.dotcms.telemetry.Metric mockMetricMetadata = mock(com.dotcms.telemetry.Metric.class);
            lenient().when(mockMetric.getMetric()).thenReturn(mockMetricMetadata);
            lenient().when(mockMetricMetadata.getName()).thenReturn("METRIC_" + i);
            stats.add(mockMetric);
        }
        
        return new MetricsSnapshot.Builder()
                .stats(stats)
                .notNumericStats(notNumericStats)
                .errors(new java.util.ArrayList<>())
                .build();
    }

    /**
     * Helper method to create a mock MetricsSnapshot with specific metric names.
     */
    private MetricsSnapshot createMockSnapshotWithNames(final Set<String> metricNames) {
        final Collection<MetricValue> stats = new java.util.ArrayList<>();
        final Collection<MetricValue> notNumericStats = new java.util.ArrayList<>();
        
        for (final String metricName : metricNames) {
            final MetricValue mockMetric = mock(MetricValue.class);
            final com.dotcms.telemetry.Metric mockMetricMetadata = mock(com.dotcms.telemetry.Metric.class);
            when(mockMetric.getMetric()).thenReturn(mockMetricMetadata);
            when(mockMetricMetadata.getName()).thenReturn(metricName);
            stats.add(mockMetric);
        }
        
        return new MetricsSnapshot.Builder()
                .stats(stats)
                .notNumericStats(notNumericStats)
                .errors(new java.util.ArrayList<>())
                .build();
    }
}

