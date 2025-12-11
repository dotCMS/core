# Telemetry Implementation Design

## Overview

The dotCMS telemetry system collects and reports metrics about system usage, configuration, and features. The system has been refactored to use CDI (Contexts and Dependency Injection) for automatic discovery and management of metrics, replacing static factory patterns.

## Architecture

### Core Components

#### `MetricType` Interface
The base interface for all metrics. Implementations must provide:
- **Metadata**: Name, description, category, and feature
- **Value Calculation**: Logic to compute the metric value

All concrete `MetricType` implementations must be annotated with `@ApplicationScoped` to be discovered by CDI.

#### `MetricStatsCollector`
CDI-managed service (`@ApplicationScoped`) that collects all metrics and generates `MetricsSnapshot` objects.

**Key Features:**
- Automatically discovers all `@ApplicationScoped MetricType` implementations via `@Inject Instance<MetricType>`
- Aggregates metrics into `MetricsSnapshot` for reporting
- Handles errors gracefully, collecting failed metrics separately
- Used by telemetry endpoints and scheduled jobs

**Usage:**
```java
@Inject
private MetricStatsCollector collector;

MetricsSnapshot snapshot = collector.getStats();
```

#### `DashboardMetricsProvider`
CDI-managed service that provides dashboard-specific metrics. Only metrics annotated with `@DashboardMetric` are included.

**Key Features:**
- Filters metrics by `@DashboardMetric` annotation
- Sorts metrics by priority (from annotation)
- Supports category-based filtering
- Used by `UsageResource` for dashboard display

**Usage:**
```java
@Inject
private DashboardMetricsProvider provider;

List<MetricType> dashboardMetrics = provider.getDashboardMetrics();
List<MetricType> contentMetrics = provider.getDashboardMetricsByCategory("content");
```

### Dashboard Metrics

#### `@DashboardMetric` Annotation
Marker annotation to include metrics in the Usage API dashboard endpoint (`/v1/usage/summary`).

**Attributes:**
- `category` (String): Optional grouping for organizing metrics (e.g., "content", "site", "user", "system")
- `priority` (int): Display order (lower values appear first, default: 0)

**Purpose:** "What to show" - Declares display intent and metadata for dashboard presentation.

**Example:**
```java
@ApplicationScoped
@MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})  // Performance control
@DashboardMetric(category = "content", priority = 1)  // Display intent
public class TotalContentsDatabaseMetricType implements DBMetricType {
    // ...
}
```

**Benefits:**
- Declarative configuration (no code changes needed to add/remove metrics from dashboard)
- Automatic discovery via CDI
- No hardcoded metric lists in API code

#### `@MetricsProfile` Annotation
**Required annotation** that declares which performance profiles a metric supports (MINIMAL, STANDARD, FULL).

**Purpose:** "When to collect" - Controls performance by restricting metric collection based on active profile.

**Attributes:**
- `value` (ProfileType[]): Array of profiles this metric supports

**Example:**
```java
@ApplicationScoped
@MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})  // Supports MINIMAL, STANDARD, and FULL
@DashboardMetric(category = "site", priority = 1)
public class TotalSitesDatabaseMetricType implements DBMetricType {
    // ...
}
```

**Important:** <strong>All metrics must have `@MetricsProfile` annotation.</strong> Metrics without this annotation will be excluded from collection. An error will be logged for missing `@MetricsProfile` annotations.

#### Relationship Between Annotations

These annotations work together in a **two-stage filtering process**:

1. **Stage 1: `@DashboardMetric` Filter**
   - Filters metrics for dashboard display
   - Provides display metadata (category, priority)
   - Purpose: "What to show"

2. **Stage 2: `@MetricsProfile` Filter**
   - Filters metrics by active performance profile
   - Controls when metrics are collected
   - Purpose: "When to collect"
   - **Required:** All metrics must have this annotation

**Best Practice:** Always use both annotations together:
```java
@ApplicationScoped
@MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})  // Performance: supports all profiles
@DashboardMetric(category = "site", priority = 1)  // Display: site category, priority 1
public class TotalSitesDatabaseMetricType implements DBMetricType {
    // ...
}
```

**Profile Guidelines:**
- **MINIMAL**: Fast, simple queries (< 500ms). Core metrics only (10-15 metrics).
- **STANDARD**: Moderate complexity (~50 metrics). Includes MINIMAL plus additional valuable metrics.
- **FULL**: All metrics (128+). Background collection only.

## Creating New Metrics

### Step 1: Implement MetricType

```java
package com.dotcms.telemetry.collectors.example;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class MyNewMetricType implements DBMetricType {
    
    @Override
    public String getName() {
        return "MY_NEW_METRIC";
    }
    
    @Override
    public String getDescription() {
        return "Description of my new metric";
    }
    
    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }
    
    @Override
    public MetricFeature getFeature() {
        return MetricFeature.CONTENTLETS;
    }
    
    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(*) AS value FROM my_table";
    }
}
```

### Step 2: Add Required `@MetricsProfile` Annotation

**All metrics must have `@MetricsProfile` annotation.** To include the metric in the Usage API dashboard, add both `@MetricsProfile` and `@DashboardMetric` annotations:

```java
@ApplicationScoped
@MetricsProfile({ProfileType.STANDARD, ProfileType.FULL})  // Performance control (required)
@DashboardMetric(category = "content", priority = 5)        // Display intent (optional)
public class MyNewMetricType implements DBMetricType {
    // ...
}
```

**Important:** <strong>All metrics require `@MetricsProfile` annotation.</strong> Metrics without this annotation will be excluded from collection. For dashboard metrics, also include `@DashboardMetric` to control display.

The metric will automatically:
- Be discovered by `MetricStatsCollector` for general telemetry
- Be filtered by active profile via `ProfileFilter`
- Be included in dashboard via `DashboardMetricsProvider` (if profile matches)
- Appear in `/v1/usage/summary` endpoint response
- Be sorted by priority within its category

### Base Classes

For common patterns, extend base classes:

- **`DBMetricType`**: For database query-based metrics
- **`ApiMetricType`**: For API call count metrics
- **Abstract classes**: Various abstract base classes for specific metric families (see package structure)

## Profile System

### Overview

The profile system controls which metrics are collected based on performance requirements. This enables fast dashboard loading while still collecting comprehensive metrics for background jobs.

### Profile Types

- **MINIMAL**: 10-15 core metrics, < 5 seconds total collection time. Used for fast dashboard loading.
- **STANDARD**: ~50 metrics, < 15 seconds total collection time. Future implementation.
- **FULL**: All 128+ metrics. Background collection only. Used by scheduled cron jobs.

### Configuration

Two separate profile settings:

```properties
# Dashboard/API profile (default: MINIMAL)
telemetry.default.profile=MINIMAL

# Cron job profile (default: FULL)
telemetry.cron.profile=FULL
```

### How It Works

1. **Dashboard/API**: Uses `telemetry.default.profile` (typically MINIMAL)
   - Fast loading with core metrics only
   - Filtered via `DashboardMetricsProvider`

2. **Cron Jobs**: Uses `telemetry.cron.profile` (typically FULL)
   - Collects all metrics for persistence
   - Filtered via `MetricStatsCollector.getStatsAndCleanUp(ProfileType)`

### Annotating Metrics

**All metrics must include `@MetricsProfile` annotation.** Dashboard metrics should also include `@DashboardMetric`:

```java
@ApplicationScoped
@MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})  // Fast, core metric
@DashboardMetric(category = "site", priority = 1)
public class TotalSitesDatabaseMetricType implements DBMetricType {
    // ...
}

@ApplicationScoped
@MetricsProfile({ProfileType.STANDARD, ProfileType.FULL})  // More complex metric
@DashboardMetric(category = "content", priority = 2)
public class RecentlyEditedContentDatabaseMetricType implements DBMetricType {
    // ...
}
```

**Profile Selection Guidelines:**
- **MINIMAL**: Simple queries (< 500ms), single table, core business value
- **STANDARD**: Moderate complexity, simple joins, valuable but not critical
- **FULL**: Complex aggregations, multi-table queries, comprehensive analysis

## Configuration

### Adding Metrics to Dashboard

1. **Always annotate the metric class with `@MetricsProfile`** (required for all metrics)
2. Optionally annotate with `@DashboardMetric` to include in dashboard
3. Optionally set `category` and `priority` in `@DashboardMetric`
4. No code changes needed in `UsageResource` or other API classes

### Removing Metrics from Dashboard

Remove the `@DashboardMetric` annotation. The metric will still be available via general telemetry endpoints but won't appear in the dashboard.

### Organizing Metrics

Use the `category` attribute to group related metrics:
- `"content"`: Content-related metrics
- `"site"`: Site-related metrics
- `"user"`: User-related metrics
- `"system"`: System configuration metrics

Use `priority` to control display order within categories (lower = first).

## API Endpoints

### General Telemetry
- **`/api/v1/telemetry/stats`**: Returns all metrics (filtered by optional `metricNames` query parameter)
- Uses `MetricStatsCollector` to collect all discovered metrics
- Returns `MetricsSnapshot` with all collected metrics

### Dashboard Metrics
- **`/api/v1/usage/summary`**: Returns dashboard-specific metrics
- Uses `DashboardMetricsProvider` to collect only `@DashboardMetric` annotated metrics
- Returns `UsageSummary` optimized for dashboard consumption
- Includes content, site, user, and system metrics

## CDI Discovery

### How It Works

1. All `@ApplicationScoped` beans implementing `MetricType` are automatically discovered by CDI
2. `MetricStatsCollector` injects `Instance<MetricType>` to access all metrics
3. `DashboardMetricsProvider` filters the collection for `@DashboardMetric` annotated types
4. No manual registration or factory methods required

### Requirements

- **Concrete implementations**: Must be annotated with `@ApplicationScoped`
- **Abstract classes**: Should NOT be annotated (they're not instantiated)
- **Interface implementations**: Must be concrete classes (interfaces can't be CDI beans)

## Migration Notes

### From Static Factory Pattern

The refactoring moved from:
- Static factory methods with hardcoded metric lists
- Manual instantiation with `new`
- Required code changes to add/remove metrics

To:
- CDI-based automatic discovery
- Declarative configuration via annotations
- No code changes needed to add/remove metrics

### Call Sites

All call sites have been updated to use CDI:
- `TelemetryResource`: Uses `CDIUtils.getBeanThrows(MetricStatsCollector.class)`
- `MetricsStatsJob`: Uses `CDIUtils.getBeanThrows(MetricStatsCollector.class)`
- `UsageResource`: Uses `CDIUtils.getBeanThrows(DashboardMetricsProvider.class)`

### Backward Compatibility

No static method wrappers are needed - all call sites have been migrated to CDI.

## Package Structure

```
com.dotcms.telemetry/
├── MetricType.java                    # Base interface
├── Metric.java                        # Metric metadata
├── MetricValue.java                   # Metric value wrapper
├── MetricsSnapshot.java               # Collection of metrics
├── DashboardMetric.java               # Dashboard marker annotation
├── MetricCategory.java                # Metric categories enum
├── MetricFeature.java                 # Feature enums
├── collectors/
│   ├── MetricStatsCollector.java      # Main collector (CDI)
│   ├── DashboardMetricsProvider.java  # Dashboard provider (CDI)
│   ├── DBMetricType.java              # Database metric base class
│   ├── ApiMetricType.java             # API metric base class
│   ├── ai/                            # AI metrics
│   ├── container/                     # Container metrics
│   ├── content/                       # Content metrics
│   ├── experiment/                    # Experiment metrics
│   ├── image/                         # Image API metrics
│   ├── language/                      # Language metrics
│   ├── site/                          # Site metrics
│   ├── template/                      # Template metrics
│   ├── theme/                         # Theme metrics
│   ├── user/                          # User metrics
│   └── workflow/                      # Workflow metrics
├── rest/
│   └── TelemetryResource.java         # Telemetry REST endpoint
└── job/
    └── MetricsStatsJob.java           # Scheduled metric collection job
```

## Best Practices

### Creating Metrics

1. **Use appropriate base classes**: Extend `DBMetricType` or `ApiMetricType` when applicable
2. **Provide clear descriptions**: Help users understand what the metric measures
3. **Choose correct category/feature**: Ensures proper classification
4. **Handle errors gracefully**: Return `Optional.empty()` on failure rather than throwing

### Dashboard Metrics

1. **Use meaningful categories**: Group related metrics together
2. **Set appropriate priorities**: Lower numbers for more important metrics
3. **Keep dashboard focused**: Only annotate metrics relevant to dashboard users
4. **Document categories**: Use consistent category names across metrics

### CDI Usage

1. **Always annotate concrete classes**: `@ApplicationScoped` is required for discovery
2. **Don't annotate abstract classes**: They're not instantiated by CDI
3. **Use `Instance<T>` for collections**: Allows iteration over all beans of a type
4. **Handle empty collections**: CDI injects empty `Instance` if no beans found (never null)

## Related Documentation

- **JavaDoc**: All classes have comprehensive JavaDoc with usage examples
- **REST API Patterns**: See `docs/backend/REST_API_PATTERNS.md` for API design guidelines

## Discovering Available Metrics

All metrics are defined in the codebase under `com.dotcms.telemetry.collectors.*`. To discover available metrics:

1. **Programmatically**: Use `MetricStatsCollector` or `DashboardMetricsProvider` to get all discovered metrics
2. **In Code**: Browse the `com.dotcms.telemetry.collectors` package structure
3. **At Runtime**: Query the `/api/v1/telemetry/stats` endpoint to see all collected metrics

The code is the source of truth for metric definitions. Each `MetricType` implementation provides:
- `getName()`: The metric identifier
- `getDescription()`: What the metric measures
- `getFeature()`: The feature category
- `getCategory()`: The metric category

---

*Last Updated: 2024*  
*Issue: #33979 - Refactor telemetry API to use CDI dependency injection*

