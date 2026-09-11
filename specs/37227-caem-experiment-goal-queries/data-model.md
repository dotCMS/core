# Data Model: CAEM-Backed Experiment Goal Result Queries

**Feature**: #37227 | **Branch**: `37227-caem-experiment-goal-queries`

---

## Prerequisite Rename

Before any CAEM implementation work, rename these two classes (5 production files total):

| Old name | New name | Package |
|----------|----------|---------|
| `CubeJSResultSet` | `AnalyticsResultSet` | `com.dotcms.cube` (package rename deferred to CubeJS removal) |
| `CubeJSResultSetImpl` | `AnalyticsResultSetImpl` | `com.dotcms.cube` |

Files updated by the rename: `CubeJSResultSet.java`, `CubeJSResultSetImpl.java`, `CubeJSClient.java`, `ContentAnalyticsFactoryImpl.java`, `ExperimentsAPIImpl.java`.

---

## New Interfaces & Classes

### `ExperimentGoalResultsQuery` *(new interface)*

**Package**: `com.dotcms.experiments.business.result`

```
ExperimentGoalResultsQuery
  + executeByDay(Experiment): AnalyticsResultSet
      Replaces: ExperimentResultsQueryFactory.createWithDayGranularity()
      Used by: ExperimentsAPIImpl.getSummary()
      Produces: AnalyticsResultSet with one ResultSetItem per (variant × day)
                Fields: Events.variant, Events.day, Events.totalSessions,
                        Events.*Successes, Events.*ConversionRate

  + executeAggregate(Experiment): AnalyticsResultSet
      Replaces: ExperimentResultsQueryFactory.create()
      Used by: ExperimentsAPIImpl.getTotalSessions()
      Produces: AnalyticsResultSet with one ResultSetItem per variant
                Fields: Events.variant, Events.totalSessions,
                        Events.*Successes, Events.*ConversionRate
```

**Invariant**: Both methods must produce `ResultSetItem` objects with field names matching the conventions read by `ExperimentsAPIImpl.getSuccess()` and `ExperimentsAPIImpl.getConvertionRate()`.

---

### `CubeJSGoalResultsAdapter` *(new class)*

**Package**: `com.dotcms.experiments.business.result`

Implements `ExperimentGoalResultsQuery`. Temporary shim — deleted when CubeJS is removed.

```
CubeJSGoalResultsAdapter
  - metricQuery: MetricExperimentResultsQuery    (injected per goal type)
  - cubeJSClient: CubeJSClient

  + executeByDay(experiment):
      query = metricQuery.getCubeJSQuery(experiment)
      rootQuery = ExperimentResultsQueryFactory.createRootQuery(experiment, dayGranularity=true)
      merged = CubeJSQuery.Builder.merge(query, rootQuery)
      return cubeJSClient.send(merged)

  + executeAggregate(experiment):
      query = metricQuery.getCubeJSQuery(experiment)
      rootQuery = ExperimentResultsQueryFactory.createRootQuery(experiment, dayGranularity=false)
      merged = CubeJSQuery.Builder.merge(query, rootQuery)
      return cubeJSClient.send(merged)
```

**Note**: `createRootQuery` is currently private in `ExperimentResultsQueryFactory`. It must be made package-private or the adapter inlines the equivalent logic.

---

### `ExperimentResultsQueryFactory` *(modified)*

**Package**: `com.dotcms.experiments.business.result`

Adds two public dispatch methods. The `Lazy` map is replaced with two maps — one CubeJS, one CAEM — both eagerly initialized. Flag is read per call via `ConfigExperimentUtil.INSTANCE.isCaemExperimentResultsEnabled()`.

```
ExperimentResultsQueryFactory (enum, INSTANCE)
  - cubeJSAdapters: Map<MetricType, ExperimentGoalResultsQuery>   (CubeJSGoalResultsAdapter per type)
  - caemQueries:    Map<MetricType, ExperimentGoalResultsQuery>   (CAEM implementations per type)

  + executeByDay(experiment): AnalyticsResultSet
      impl = selectImpl(experiment.goals().primary().getMetric().type())
      return impl.executeByDay(experiment)

  + executeAggregate(experiment): AnalyticsResultSet
      impl = selectImpl(experiment.goals().primary().getMetric().type())
      return impl.executeAggregate(experiment)

  - selectImpl(metricType): ExperimentGoalResultsQuery
      if ConfigExperimentUtil.INSTANCE.isCaemExperimentResultsEnabled():
          return caemQueries.get(metricType)
      else:
          return cubeJSAdapters.get(metricType)
```

**Existing methods** (`create()`, `createWithDayGranularity()`) are deprecated but retained — they are only called internally by `CubeJSGoalResultsAdapter` now. `ExperimentsAPIImpl.getSummary()` and `getTotalSessions()` are updated to call the new dispatch methods.

---

### `CaemHttpClient` *(new class)*

**Package**: `com.dotcms.experiments.business.result` (or `com.dotcms.analytics.caem`)

Handles authenticated HTTP calls to the CAEM analytics API. Returns `AnalyticsResultSet` populated from the CAEM response.

```
CaemHttpClient
  + get(url: String, queryParams: Map<String, String>, host: Host): AnalyticsResultSet
      authHeader = EventAnalyticsProxyHelper.buildAuthHeader(host)
      fullUrl = buildUrl(baseUrl(host), url, queryParams)
      response = CircuitBreakerUrl.builder()
                   .setUrl(fullUrl)
                   .setMethod(GET)
                   .setHeaders({"Authorization": authHeader})
                   .setThrowWhenError(true)
                   .setTimeout(TIMEOUT_MS)
                   .build()
                   .doResponse()
      if response.statusCode != 200: throw CaemQueryException(response.statusCode, response.body)
      return parseResponse(response.response)

  - parseResponse(json: String): AnalyticsResultSet
      rows = Json.parse(json).get("data")   // array of row objects
      mapped = rows.map(row -> mapCaemFieldsToEventFields(row))   // List<Map<String,Object>>
      return new AnalyticsResultSetImpl(mapped)                      // existing class, no new impl needed

  - mapCaemFieldsToEventFields(caemRow: Map): Map<String, Object>
      // translates CAEM response field names to the Events.* convention
      // e.g. "variant" → "Events.variant", "day" → "Events.day", etc.
```

**Error handling**: non-2xx response → throw checked `CaemQueryException` (or `DotDataException` to match existing API) — never return partial/empty data silently.

---

### CAEM Goal Query Classes *(new)*

All implement `ExperimentGoalResultsQuery`. All use `CaemHttpClient` for outbound calls.

#### `BounceRateCAEMResultQuery`

```
executeByDay(experiment):
    params = {
        experimentId: experiment.getIdentifier(),
        runningId: currentRunningId(experiment),
        metrics: "totalSessions,bounceSessions,bounceRate",
        dimensions: "variant,day"
    }
    return caemHttpClient.get("/v1/analytics/sessions", params, currentHost())

executeAggregate(experiment):
    params = {
        experimentId: experiment.getIdentifier(),
        runningId: currentRunningId(experiment),
        metrics: "totalSessions,bounceSessions,bounceRate",
        dimensions: "variant"
    }
    return caemHttpClient.get("/v1/analytics/sessions", params, currentHost())
```

#### `ExitRateCAEMResultQuery`

Same as above but adds `referencePage` from the experiment's goal condition and uses exit-specific metrics (`exitSessions`, `exitRate`).

#### `ReachPageCAEMResultQuery`

Handles `REACH_PAGE`. Calls `/v1/analytics/sessions/behavior` with `behavior=reachTarget`, `referencePage`, and `targetUrl` from the goal condition.

#### `UrlParameterCAEMResultQuery`

Handles `URL_PARAMETER`. Calls `/v1/analytics/sessions/behavior` with `behavior=urlParam`, `paramName`, and `paramValue` from the goal condition (`QueryParameter.getName()` / `QueryParameter.getValue()`).

---

### `ConfigExperimentUtil` *(modified)*

**Package**: `com.dotcms.experiments.business`

Adds the new flag following the existing `featureFlagExperiments` pattern exactly:

```
private final AtomicBoolean caemExperimentResults;

ConfigExperimentUtil() {
    ...
    caemExperimentResults = new AtomicBoolean(resolveCaemExperimentResults());
    // subscription already set up — notify() updated to handle the new key
}

public boolean isCaemExperimentResultsEnabled() {
    return caemExperimentResults.get();
}

// notify() update:
if (event.getKey().contains(FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS)) {
    caemExperimentResults.set(resolveCaemExperimentResults());
}

private boolean resolveCaemExperimentResults() {
    return Config.getBooleanProperty(FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS, false);
}
```

---

### `FeatureFlagName` *(modified)*

**Package**: `com.dotcms.featureflag`

```java
/** Routes experiment result queries to the CAEM analytics backend instead of CubeJS. Off by default. */
String FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS = "FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS";
```

---

## Unchanged Entities

| Entity | File | Notes |
|--------|------|-------|
| `MetricExperimentResultsQuery` | `result/MetricExperimentResultsQuery.java` | Not modified |
| `BounceRateResultQuery` | `result/BounceRateResultQuery.java` | Not modified |
| `ExitRateResultQuery` | `result/ExitRateResultQuery.java` | Not modified |
| `ReachTargetAfterExperimentPageResultQuery` | `result/ReachTargetAfterExperimentPageResultQuery.java` | Not modified |
| `GoalResults` / `VariantResults` | `result/GoalResults.java` etc. | Not modified |
| `ExperimentResults` / `ExperimentResults.Builder` | `result/ExperimentResults.java` | Not modified |
| `AnalyticsResultSet` | `cube/AnalyticsResultSet.java` | Not modified — reused as return type |
| `ResultSetItem` | `analytics/model/ResultSetItem.java` | Not modified — reused as row type |

---

## Field Mapping Reference

The CAEM HTTP response is parsed into `ResultSetItem` objects. The existing `ExperimentsAPIImpl` loops read these fields:

| `ResultSetItem` key | Populated from (CubeJS) | Populated from (CAEM) |
|---------------------|------------------------|-----------------------|
| `Events.variant` | CubeJS `Events.variant` dimension | CAEM `variant` dimension value |
| `Events.day` | CubeJS `Events.day` time dimension | CAEM `day` dimension (executeByDay) |
| `Events.totalSessions` | CubeJS `Events.totalSessions` measure | CAEM `totalSessions` metric |
| `Events.*Successes` (suffix) | CubeJS measure name ending in `Successes` | CAEM `bounceSessions`/`exitSessions`/`successSessions` mapped to matching key |
| `Events.*ConversionRate` (suffix) | CubeJS measure name ending in `ConversionRate` | CAEM `bounceRate`/`exitRate`/`successRate` mapped to matching key |