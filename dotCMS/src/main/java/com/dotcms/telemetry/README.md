# Telemetry API

## Scope

**Purpose**: Database-sourced gauge metrics for product usage analytics

### What This IS
- ✅ Point-in-time database snapshots (site counts, content counts, etc.)
- ✅ Scheduled daily collection + on-demand REST API
- ✅ Shared database queries (all replicas return same results)
- ✅ Direct use in Usage Dashboard UI
- ✅ Future: Prometheus scraping for monitoring dashboards

### What This IS NOT
- ❌ Real-time event counting (use Micrometer Counters instead)
- ❌ Request timing/duration tracking (use Micrometer Timers instead)
- ❌ Per-instance operational metrics (use existing MeterBinders)

For request counting, see: `com.dotcms.metrics.binders.HttpRequestMetrics`

## Architecture

### Database-Sourced Metrics

All telemetry metrics are **Gauges** (point-in-time snapshots) that query the **shared database**. This means:

* All replica instances return **identical values** (same data source)
* Perfect for **direct REST API use** in Usage Dashboard UI
* Prometheus can scrape from **any instance** (or all instances, will see same values)
* Prometheus aggregation functions work correctly (since all instances report same state)

### Collection Methods

1. **Daily Scheduled Job**: Collects all metrics on a schedule
2. **On-Demand REST API**: `/api/v1/telemetry/stats` endpoint for immediate collection

### Future: Prometheus Integration

When Prometheus integration is implemented (Issue #33982), gauge metrics will be exposed as Prometheus Gauges for monitoring dashboards. The shared database architecture ensures that scraping from any instance provides consistent metrics.

## Request Counting Pattern (Future)

When request counting metrics are needed:

* Use **Micrometer Counter** directly in request handling code
* Tag with endpoint, method, status, mode
* Each replica tracks its **own request volume**
* Prometheus **aggregates across instances** (sum, rate, etc.)
* No database overhead, minimal performance impact

Example:
```java
@ApplicationScoped
public class ImageApiMetrics implements MeterBinder {
    @Override
    public void bindTo(MeterRegistry registry) {
        Counter.builder("dotcms.api.image.requests.total")
            .description("Total image API requests")
            .tag("endpoint", "dA")
            .tag("mode", "live")
            .register(registry);
    }
}
```

