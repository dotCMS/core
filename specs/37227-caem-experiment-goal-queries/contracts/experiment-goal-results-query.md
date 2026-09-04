# Contract: ExperimentGoalResultsQuery

**Type**: Internal Java interface (not a REST endpoint)
**Package**: `com.dotcms.experiments.business.result`

This is the seam between `ExperimentResultsQueryFactory` and all analytics backends (CubeJS and CAEM). Any new analytics backend implements this interface. Callers never reference `AnalyticsResultSet` directly for business logic — they process it through the existing `ExperimentsAPIImpl` loops.

---

## Interface Definition

```java
/**
 * Provider-agnostic experiment result query. Replaces the two CubeJS-specific
 * factory methods (create / createWithDayGranularity) with backend-neutral equivalents.
 * Implementations must populate ResultSetItem fields using the Events.* naming convention
 * expected by ExperimentsAPIImpl processing loops.
 */
public interface ExperimentGoalResultsQuery {

    /**
     * Returns per-variant results broken down by day.
     * Replaces ExperimentResultsQueryFactory.createWithDayGranularity().
     * Used by ExperimentsAPIImpl.getSummary().
     *
     * Each ResultSetItem must contain: Events.variant, Events.day,
     * Events.totalSessions, Events.*Successes, Events.*ConversionRate.
     */
    AnalyticsResultSet executeByDay(Experiment experiment) throws DotDataException;

    /**
     * Returns aggregate (non-day) per-variant totals for ALL experiment variants.
     * Replaces ExperimentResultsQueryFactory.create().
     * Used by ExperimentsAPIImpl.getTotalSessions().
     *
     * Each ResultSetItem must contain: Events.variant, Events.totalSessions,
     * Events.*Successes, Events.*ConversionRate.
     *
     * INVARIANT: must return one row per experiment variant so that
     * ExperimentResults.getSessions().getVariants().size() >= 2 holds,
     * preserving the Bayesian calculation gate.
     */
    AnalyticsResultSet executeAggregate(Experiment experiment) throws DotDataException;
}
```

---

## Implementations

| Class | Backend | Goal types |
|-------|---------|------------|
| `CubeJSGoalResultsAdapter` | CubeJS (temporary shim) | All four |
| `BounceRateCAEMResultQuery` | CAEM | `BOUNCE_RATE` |
| `ExitRateCAEMResultQuery` | CAEM | `EXIT_RATE` |
| `ReachPageCAEMResultQuery` | CAEM | `REACH_PAGE` |
| `UrlParameterCAEMResultQuery` | CAEM | `URL_PARAMETER` |

---

## Factory Dispatch

```
ExperimentResultsQueryFactory.executeByDay(experiment)
  → if FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS: caemQueries.get(metricType).executeByDay(experiment)
  → else: cubeJSAdapters.get(metricType).executeByDay(experiment)

ExperimentResultsQueryFactory.executeAggregate(experiment)
  → if FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS: caemQueries.get(metricType).executeAggregate(experiment)
  → else: cubeJSAdapters.get(metricType).executeAggregate(experiment)
```

Flag is read per call via `ConfigExperimentUtil.INSTANCE.isCaemExperimentResultsEnabled()`.

---

## Call Sites (updated)

| Previous call | Updated call |
|---------------|--------------|
| `ExperimentResultsQueryFactory.INSTANCE.createWithDayGranularity(experiment)` → `cubeClient.send(...)` | `ExperimentResultsQueryFactory.INSTANCE.executeByDay(experiment)` |
| `ExperimentResultsQueryFactory.INSTANCE.create(experiment)` → `cubeClient.send(...)` | `ExperimentResultsQueryFactory.INSTANCE.executeAggregate(experiment)` |

Both in `ExperimentsAPIImpl` (`getSummary()` and `getTotalSessions()`).