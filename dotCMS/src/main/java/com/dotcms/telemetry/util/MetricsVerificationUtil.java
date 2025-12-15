package com.dotcms.telemetry.util;

import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotmarketing.util.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for verifying metric collection and comparing metric snapshots.
 * 
 * <p>Useful for verifying backward compatibility after refactoring.</p>
 */
public class MetricsVerificationUtil {
    
    /**
     * Extracts all metric names from a MetricsSnapshot.
     * 
     * @param snapshot the metrics snapshot
     * @return set of all metric names (from both numeric and non-numeric metrics)
     */
    public static Set<String> extractMetricNames(final MetricsSnapshot snapshot) {
        final Set<String> metricNames = new HashSet<>();
        
        if (snapshot.getStats() != null) {
            metricNames.addAll(snapshot.getStats().stream()
                    .map(mv -> mv.getMetric().getName())
                    .collect(Collectors.toSet()));
        }
        
        // getNotNumericStats() returns a Map<String, Object> where key is metric name
        // (converted from Collection<MetricValue> for JSON serialization)
        final Map<String, Object> notNumericMap = snapshot.getNotNumericStats();
        if (notNumericMap != null) {
            metricNames.addAll(notNumericMap.keySet());
        }
        
        return metricNames;
    }
    
    /**
     * Compares two MetricsSnapshots and reports differences.
     * 
     * @param before the snapshot from before refactoring
     * @param after the snapshot from after refactoring
     * @return comparison result with missing and added metrics
     */
    public static MetricComparison compareSnapshots(final MetricsSnapshot before, final MetricsSnapshot after) {
        final Set<String> beforeNames = extractMetricNames(before);
        final Set<String> afterNames = extractMetricNames(after);
        
        final Set<String> missing = new HashSet<>(beforeNames);
        missing.removeAll(afterNames);
        
        final Set<String> added = new HashSet<>(afterNames);
        added.removeAll(beforeNames);
        
        return new MetricComparison(
                beforeNames.size(),
                afterNames.size(),
                missing,
                added,
                before.getStats().size() + (before.getNotNumericStats() != null ? before.getNotNumericStats().size() : 0),
                after.getStats().size() + (after.getNotNumericStats() != null ? after.getNotNumericStats().size() : 0)
        );
    }
    
    /**
     * Logs a detailed comparison report.
     * 
     * @param comparison the comparison result
     * @param context context description (e.g., "Before/After refactoring")
     */
    public static void logComparison(final MetricComparison comparison, final String context) {
        Logger.info(MetricsVerificationUtil.class, String.format(
            "=== Metric Comparison: %s ===\n" +
            "Before: %d unique metrics, %d total metric values\n" +
            "After:  %d unique metrics, %d total metric values\n" +
            "Missing metrics (%d): %s\n" +
            "Added metrics (%d): %s\n" +
            "Match: %s",
            context,
            comparison.getBeforeCount(),
            comparison.getBeforeTotal(),
            comparison.getAfterCount(),
            comparison.getAfterTotal(),
            comparison.getMissing().size(),
            comparison.getMissing(),
            comparison.getAdded().size(),
            comparison.getAdded(),
            comparison.getMissing().isEmpty() && comparison.getAdded().isEmpty() ? "YES" : "NO"
        ));
    }
    
    /**
     * Result of comparing two MetricsSnapshots.
     */
    public static class MetricComparison {
        private final int beforeCount;
        private final int afterCount;
        private final Set<String> missing;
        private final Set<String> added;
        private final int beforeTotal;
        private final int afterTotal;
        
        public MetricComparison(final int beforeCount, final int afterCount,
                               final Set<String> missing, final Set<String> added,
                               final int beforeTotal, final int afterTotal) {
            this.beforeCount = beforeCount;
            this.afterCount = afterCount;
            this.missing = missing;
            this.added = added;
            this.beforeTotal = beforeTotal;
            this.afterTotal = afterTotal;
        }
        
        public int getBeforeCount() {
            return beforeCount;
        }
        
        public int getAfterCount() {
            return afterCount;
        }
        
        public Set<String> getMissing() {
            return missing;
        }
        
        public Set<String> getAdded() {
            return added;
        }
        
        public int getBeforeTotal() {
            return beforeTotal;
        }
        
        public int getAfterTotal() {
            return afterTotal;
        }
        
        public boolean isIdentical() {
            return missing.isEmpty() && added.isEmpty() && beforeCount == afterCount;
        }
    }
}

