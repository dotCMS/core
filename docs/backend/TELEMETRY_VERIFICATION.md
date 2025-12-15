# Telemetry Metrics Verification Guide

## Verifying Backward Compatibility

This guide explains how to verify that `MetricsStatsJob` produces the same set of metrics as before the profile refactoring.

## Overview

The `MetricsStatsJob` now uses `getAllStatsAndCleanUp()` which collects **ALL metrics without profile filtering** to ensure backward compatibility with pre-refactoring behavior.

## Verification Methods

### Method 1: Compare Metric Counts

**Before Refactoring:**
- Run the job on the main branch
- Count total metrics: `stats.size() + notNumericStats.size()`
- Record the count

**After Refactoring:**
- Run the job on your branch
- Count total metrics: `stats.size() + notNumericStats.size()`
- Compare counts - they should match

### Method 2: Compare Metric Names

**Extract metric names from both versions:**

```java
// Before refactoring (main branch)
Set<String> beforeMetrics = snapshot.getStats().stream()
    .map(mv -> mv.getMetric().getName())
    .collect(Collectors.toSet());
beforeMetrics.addAll(snapshot.getNotNumericStats().stream()
    .map(mv -> mv.getMetric().getName())
    .collect(Collectors.toSet()));

// After refactoring (your branch)
Set<String> afterMetrics = snapshot.getStats().stream()
    .map(mv -> mv.getMetric().getName())
    .collect(Collectors.toSet());
afterMetrics.addAll(snapshot.getNotNumericStats().stream()
    .map(mv -> mv.getMetric().getName())
    .collect(Collectors.toSet()));

// Compare
Set<String> missing = new HashSet<>(beforeMetrics);
missing.removeAll(afterMetrics);
Set<String> added = new HashSet<>(afterMetrics);
added.removeAll(beforeMetrics);

System.out.println("Missing metrics: " + missing);
System.out.println("Added metrics: " + added);
```

### Method 3: Log-Based Verification

Enable debug logging and compare:

```properties
# Enable debug logging for telemetry
com.dotcms.telemetry.collectors.MetricStatsCollector=DEBUG
com.dotcms.telemetry.job.MetricsStatsJob=DEBUG
```

**Look for these log messages:**

```
# Should see: "Collecting ALL metrics without profile filtering"
# Should see: "MetricStatsCollector discovered: X MetricType implementations"
# Should see: "Cron job collected X numeric metrics, Y non-numeric metrics, Z errors"
```

### Method 4: Database Comparison

**Query persisted metrics:**

```sql
-- Get the most recent snapshot
SELECT * FROM metrics_snapshot 
ORDER BY insert_date DESC 
LIMIT 1;

-- Count metrics in the snapshot
SELECT 
    jsonb_array_length(stats) as numeric_metrics,
    jsonb_object_keys(not_numeric_stats) as non_numeric_metrics
FROM metrics_snapshot 
ORDER BY insert_date DESC 
LIMIT 1;
```

Compare the counts between main branch and your branch.

## Implementation Details

### Current Implementation

```java
// MetricsStatsJob.execute()
metricsSnapshot = collector.getAllStatsAndCleanUp();
```

The `getAllStatsAndCleanUp()` method:
- ✅ Bypasses profile filtering
- ✅ Collects ALL discovered metrics
- ✅ Ensures backward compatibility

### Profile Filtering (Not Used by Cron Job)

The profile system is used for:
- **Dashboard**: `telemetry.default.profile=MINIMAL` (fast loading)
- **API endpoints**: Uses default profile
- **Cron job**: Uses `getAllStatsAndCleanUp()` (no filtering)

## Expected Results

### Metric Discovery

All `@ApplicationScoped MetricType` implementations should be discovered via CDI:

```
MetricStatsCollector discovered: TotalSitesDatabaseMetricType
MetricStatsCollector discovered: TotalActiveSitesDatabaseMetricType
MetricStatsCollector discovered: TotalContentsDatabaseMetricType
...
```

### Metric Collection

All discovered metrics should be collected:

```
Collecting all 128 metrics without profile filtering
Cron job collected 100 numeric metrics, 28 non-numeric metrics, 0 errors
```

## Troubleshooting

### Issue: Fewer metrics than expected

**Possible causes:**
1. CDI scanning issue - check logs for "discovered 0 MetricType implementations"
2. Metrics not annotated with `@ApplicationScoped`
3. CDI beans.xml configuration issue

**Solution:**
- Verify all metric classes have `@ApplicationScoped`
- Check CDI scanning configuration
- Review application logs for discovery warnings

### Issue: Metrics missing from snapshot

**Possible causes:**
1. Metrics throwing exceptions during collection
2. Metrics returning `Optional.empty()`
3. Database connection issues

**Solution:**
- Check `snapshot.getErrors()` for failed metrics
- Review debug logs for metric calculation errors
- Verify database connectivity

### Issue: Different metric counts between branches

**Possible causes:**
1. New metrics added/removed between branches
2. Metrics renamed
3. CDI discovery differences

**Solution:**
- Compare metric class lists between branches
- Check for renamed metrics
- Verify CDI configuration is identical

## Automated Testing

See `MetricsStatsJobBackwardCompatibilityTest.java` for automated verification:

```java
@Test
public void testAllMetricsCollectedWithoutFiltering() {
    // Verifies all metrics are collected
}

@Test
public void testProfileFilteringDoesNotAffectCronJob() {
    // Verifies profile filtering doesn't affect cron job
}

@Test
public void testMetricNamesConsistency() {
    // Verifies specific metric names are present
}
```

## Manual Verification Script

Create a simple verification script:

```java
public class VerifyMetricsJob {
    public static void main(String[] args) {
        MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);
        MetricsSnapshot snapshot = collector.getAllStatsAndCleanUp();
        
        System.out.println("Total numeric metrics: " + snapshot.getStats().size());
        System.out.println("Total non-numeric metrics: " + snapshot.getNotNumericStats().size());
        System.out.println("Total errors: " + snapshot.getErrors().size());
        
        Set<String> allMetricNames = new HashSet<>();
        snapshot.getStats().forEach(mv -> allMetricNames.add(mv.getMetric().getName()));
        snapshot.getNotNumericStats().forEach(mv -> allMetricNames.add(mv.getMetric().getName()));
        
        System.out.println("Total unique metrics: " + allMetricNames.size());
        System.out.println("Metric names: " + allMetricNames);
    }
}
```

## Success Criteria

✅ **Metric count matches** between main branch and your branch  
✅ **All metric names present** in both versions  
✅ **No unexpected errors** in metric collection  
✅ **CDI discovery working** (all metrics discovered)  

If all criteria are met, backward compatibility is verified.

