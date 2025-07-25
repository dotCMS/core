# dotCMS Metrics Performance Best Practices

## 🚨 **Critical Performance Considerations**

### **1. Cardinality Explosion Prevention**

**The #1 performance killer for metrics is high cardinality (too many unique tag combinations).**

```java
// ❌ DANGEROUS: Unbounded cardinality
Gauge.builder("request.duration", this, metrics -> getDuration())
    .tag("user_id", userId)        // Could be millions of users!
    .tag("request_id", requestId)  // Infinite combinations!
    .register(registry);

// ✅ SAFE: Bounded cardinality  
Gauge.builder("request.duration", this, metrics -> getDuration())
    .tag("endpoint", getEndpointPattern(uri))  // Limited to ~10 patterns
    .tag("method", request.getMethod())        // Limited to GET/POST/etc
    .tag("status_range", getStatusRange(code)) // Limited to 2xx/3xx/4xx/5xx
    .register(registry);
```

**Cardinality Guidelines:**
- **Total metric series < 10,000** per instance
- **Tags per metric < 10**
- **Unique values per tag < 100**

### **2. Efficient Database Queries**

```java
// ✅ OPTIMIZED: Single efficient query with proper indexes
private double getContentCountByStatus(String status) {
    String sql = "SELECT COUNT(*) FROM contentlet WHERE deleted = false AND live = ?";
    // Uses index on (deleted, live)
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setBoolean(1, "published".equals(status));
        return getCount(stmt);
    }
}

// ❌ INEFFICIENT: Multiple queries or full table scans
private double getBadContentCount() {
    // Don't do this - causes full table scan every scrape
    String sql = "SELECT COUNT(*) FROM contentlet WHERE title LIKE '%search%'";
}
```

### **3. Metric Value Caching**

```java
public class CachedMetrics {
    private volatile double cachedValue = 0.0;
    private volatile long lastUpdate = 0;
    private static final long CACHE_TTL_MS = 30_000; // 30 seconds
    
    private double getCachedExpensiveMetric() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > CACHE_TTL_MS) {
            synchronized (this) {
                if (now - lastUpdate > CACHE_TTL_MS) {
                    cachedValue = calculateExpensiveValue();
                    lastUpdate = now;
                }
            }
        }
        return cachedValue;
    }
}
```

### **4. Connection Pool Efficiency**

```java
// ✅ GOOD: Use existing connection pools efficiently
private double getMetricFromDatabase() {
    try (Connection conn = DbConnectionFactory.getConnection()) {
        // Quick query with proper timeout
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setQueryTimeout(5); // 5 second timeout
            return executeQuery(stmt);
        }
    } catch (SQLException e) {
        Logger.debug(this, "Metric query failed: " + e.getMessage());
        return 0.0; // Fail gracefully
    }
}

// ❌ BAD: Don't create new connections or long-running queries
```

## ⚡ **Performance Monitoring for Metrics**

### **Registry Size Monitoring**

```java
// Monitor your own metrics overhead
Gauge.builder("dotcms.metrics.registry.size", this, 
    metrics -> globalRegistry.getMeters().size())
    .description("Number of metrics in registry")
    .register(registry);

Gauge.builder("dotcms.metrics.memory.usage", this,
    metrics -> getMetricsMemoryUsage())
    .description("Memory used by metrics (bytes)")
    .register(registry);
```

### **Scrape Duration Monitoring**

```java
// Track how long metrics collection takes
Timer.builder("dotcms.metrics.scrape.duration")
    .description("Time to collect all metrics")
    .register(registry);
```

## 🔧 **dotCMS-Specific Optimizations**

### **1. Meter Filters Configuration**

```java
// In MetricsService.java - add cardinality protection
private void configureCardinaLimits(MeterRegistry registry) {
    registry.config()
        // Limit total metrics
        .meterFilter(MeterFilter.maximumAllowableMetrics(10000))
        
        // Deny metrics with too many tags
        .meterFilter(MeterFilter.deny(id -> {
            return id.getTags().size() > 8; // Max 8 tags per metric
        }))
        
        // Rename metrics to prevent collision
        .meterFilter(MeterFilter.renameTag("dotcms.http", "uri", "endpoint"))
        
        // Sample high-frequency metrics
        .meterFilter(MeterFilter.accept(id -> {
            // Sample 1% of request metrics to reduce overhead
            if (id.getName().startsWith("dotcms.http.requests")) {
                return Math.random() < 0.01;
            }
            return true;
        }));
}
```

### **2. Conditional Metric Registration**

```java
// Only register expensive metrics in production
if (MetricsConfig.DETAILED_METRICS_ENABLED) {
    registerExpensiveMetrics(registry);
}

// Skip metrics for certain environments
if (!"development".equals(Config.getStringProperty("environment"))) {
    registerProductionOnlyMetrics(registry);
}
```

### **3. Asynchronous Metric Collection**

```java
// For very expensive metrics, collect asynchronously
private final ScheduledExecutorService metricsExecutor = 
    Executors.newScheduledThreadPool(1, 
        new ThreadFactoryBuilder()
            .setNameFormat("metrics-collector-%d")
            .setDaemon(true)
            .build());

private volatile double asyncMetricValue = 0.0;

public void startAsyncCollection() {
    metricsExecutor.scheduleAtFixedRate(() -> {
        try {
            asyncMetricValue = calculateExpensiveMetric();
        } catch (Exception e) {
            Logger.warn(this, "Async metric collection failed", e);
        }
    }, 0, 60, TimeUnit.SECONDS); // Update every minute
}
```

## 📈 **Performance Testing Metrics**

### **Load Testing Considerations**

```bash
# Test metrics overhead during load testing
# 1. Baseline performance without metrics
curl -X POST http://localhost:8080/api/v1/disable-metrics

# 2. Performance with metrics enabled  
curl -X POST http://localhost:8080/api/v1/enable-metrics

# 3. Compare request latency, throughput, memory usage
```

### **JVM Monitoring During Metrics Collection**

```java
// Monitor GC impact of metrics
Gauge.builder("dotcms.metrics.gc.impact", this, metrics -> {
    MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    return memory.getHeapMemoryUsage().getUsed();
}).register(registry);
```

## 🎛️ **Configuration Tuning**

### **Environment-Specific Settings**

```properties
# Production: Minimal impact
metrics.collection.interval-seconds=30
metrics.max-tags=5000
metrics.detailed.enabled=false
metrics.sampling.rate=0.1

# Development: More detailed monitoring
metrics.collection.interval-seconds=10  
metrics.max-tags=10000
metrics.detailed.enabled=true
metrics.sampling.rate=1.0

# High-traffic production: Ultra-minimal
metrics.collection.interval-seconds=60
metrics.max-tags=1000  
metrics.database.enabled=false  # Disable expensive DB metrics
metrics.search.enabled=false    # Disable expensive search metrics
```

## 🚨 **Warning Signs & Troubleshooting**

### **Performance Red Flags**

```bash
# High cardinality warning signs
curl http://localhost:8090/dotmgt/metrics | wc -l  # Should be < 10,000 lines

# Memory usage growth
jstat -gc <pid> 1s | grep -E "EU|OU"  # Heap usage growing?

# Scrape duration warnings  
curl -w "%{time_total}" http://localhost:8090/dotmgt/metrics  # Should be < 5 seconds
```

### **Emergency Metric Disabling**

```java
// Circuit breaker for metrics
private volatile boolean metricsEnabled = true;

public void emergencyDisableMetrics() {
    metricsEnabled = false;
    Logger.warn(this, "Metrics disabled due to performance issues");
}

private double safeGetMetric(Supplier<Double> metricSupplier) {
    if (!metricsEnabled) return 0.0;
    
    try {
        return metricSupplier.get();
    } catch (Exception e) {
        Logger.debug(this, "Metric collection failed", e);
        return 0.0;
    }
}
```

## 📊 **Recommended Metrics Hierarchy**

### **Priority 1: Critical (Always Enabled)**
- JVM metrics (memory, GC)
- HTTP request counts and error rates
- Database connection pool
- Cache hit rates

### **Priority 2: Important (Production Enabled)**  
- Content operation counts
- User session counts
- Search availability
- Storage usage

### **Priority 3: Detailed (Development Only)**
- Individual endpoint timings
- Detailed cache statistics by region
- File operation details
- Per-user metrics

## 🔍 **Conclusion**

**The key principle: Metrics should have near-zero performance impact when done correctly.**

- **Lazy evaluation** ensures minimal CPU overhead
- **Cardinality limits** prevent memory explosion  
- **Efficient queries** minimize database impact
- **Caching** reduces repeated calculations
- **Graceful degradation** ensures metrics never break the application

By following these practices, your dotCMS metrics will provide valuable insights without impacting user experience. 