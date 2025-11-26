# dotCMS Health Check System - Developer Guide

A production-ready, Kubernetes-compatible health check system with safety-first configuration and comprehensive monitoring capabilities.

## 🎯 **Quick Start**

### For Developers Adding New Health Checks
1. **Extend HealthCheckBase** (recommended) or implement HealthCheck directly
2. **Use explicit safety configuration** with isLivenessCheck() and isReadinessCheck() methods
3. **Follow naming convention**: `health.check.{check-name}.{property}` for configuration
4. **Test with production mode configuration** first
5. **Override buildStructuredData()** to provide machine-parsable information for monitoring tools

### For Operations Teams
1. **Start with MONITOR_MODE for new checks** to prevent probe failures
2. **Use /livez and /readyz for Kubernetes probes** (minimal text responses, common endpoint names in k8s)
3. **Monitor /health endpoint** for detailed JSON status (restrict network access)
4. **Enable event-driven monitoring** for database and Elasticsearch (faster failure detection)
5. **Progressively enable stricter checking** after validating behavior
6. **Use structured data for automated monitoring** and alerting systems

---

## 🏗️ **Architecture Overview**

### Health Check Categorization Strategy

The system uses **explicit interface methods** to categorize health checks instead of naming conventions:

```java
public interface HealthCheck {
    /**
     * Indicates if this health check is safe for Kubernetes liveness probes.
     * DEFAULT: false (safe default - assume it checks dependencies)
     */
    default boolean isLivenessCheck() { return false; }
    
    /**
     * Indicates if this health check should be included in readiness probes.
     * DEFAULT: true (most checks are readiness checks)
     */
    default boolean isReadinessCheck() { return true; }
}
```

### Health Check Response Structure

Each health check result provides both **human-readable** and **machine-parsable** information:

```json
{
  "name": "database",
  "status": "pass",
  "message": "Database connection successful (connection time: 45ms, DB version: 13.2)",
  "data": {
    "dbVersion": 13.2,
    "connectionTimeMs": 45
  },
  "time": "2024-01-15T10:30:00Z",
  "duration": "50ms"
}
```

**Key Features:**
- **`message`**: Human-readable status for logs and dashboards
- **`data`**: Structured information for monitoring tools and automated systems
- **`status`**: Standard pass/fail/warn status for compatibility
- **Dual Purpose**: Serves both human operators and machine automation

### Kubernetes Health Check Strategy

**CRITICAL DISTINCTION:**
- **LIVENESS PROBES** → Only check core application health (failure = pod restart)
- **READINESS PROBES** → Check application + dependencies (failure = remove from load balancer)

```
┌─────────────────────────────────────────────────────────────┐
│ LIVENESS CHECKS (Core Application Only)                    │
├─────────────────────────────────────────────────────────────┤
│ ✅ ApplicationHealthCheck    - JVM memory usage             │
│ ✅ SystemHealthCheck        - OS resources, disk space     │
│ ✅ ServletContainerHealthCheck - Web container health      │
│ ✅ ThreadHealthCheck        - JVM deadlock detection       │
│ ✅ GarbageCollectionHealthCheck - GC pressure monitoring   │
│                                                             │
│ ❌ DatabaseHealthCheck      - External dependency          │
│ ❌ CacheHealthCheck         - External dependency          │
│ ❌ ElasticsearchHealthCheck - External dependency          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ READINESS CHECKS (Application + Dependencies)              │
├─────────────────────────────────────────────────────────────┤
│ ✅ All liveness checks PLUS:                               │
│ ✅ CdiInitializationHealthCheck - CDI container ready      │
│ ✅ DatabaseHealthCheck       - Database connectivity       │
│ ✅ CacheHealthCheck          - Cache operations            │
│ ✅ ElasticsearchHealthCheck  - Search functionality        │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 **Startup Phase Management**

### Overview

The health check system implements **intelligent startup phase detection** to provide accurate health reporting during application initialization. This prevents false failures during legitimate startup operations and ensures proper Kubernetes probe behavior.

### Independent Liveness and Readiness Startup Logic

The system uses **separate startup phases** for liveness and readiness probes, reflecting their different purposes:

#### **Liveness Startup Phase**
- **Purpose**: Detect when the process is responsive (not deadlocked)
- **Behavior**: **Optimistic** - assumes `UP` unless proven `DOWN`
- **Rationale**: If the health check endpoint can respond, the process is alive
- **Ends**: When liveness health checks first succeed
- **Duration**: Typically 10-30 seconds (servlet container becomes responsive)

#### **Readiness Startup Phase**  
- **Purpose**: Detect when the system is fully operational and ready for traffic
- **Behavior**: **Conservative** - assumes `DOWN` until proven `UP`
- **Rationale**: Don't serve traffic until all dependencies are verified
- **Ends**: When both liveness AND readiness health checks succeed
- **Duration**: Variable (depends on plugin upgrades, database migrations, etc.)

### Startup Phase Behavior Matrix

```
┌─────────────────────┬──────────────────┬──────────────────┐
│ System State        │ Liveness Probe   │ Readiness Probe  │
├─────────────────────┼──────────────────┼──────────────────┤
│ Process Starting    │ UP (optimistic)  │ DOWN (waiting)   │
│ Servlet Responsive  │ UP (first success│ DOWN (waiting)   │
│ Plugin Upgrades     │ UP (responsive)  │ DOWN (not ready) │
│ DB Migrations       │ UP (responsive)  │ DOWN (not ready) │
│ Fully Operational   │ UP (operational) │ UP (operational) │
│ Process Deadlocked  │ DOWN (timeout)   │ DOWN (timeout)   │
└─────────────────────┴──────────────────┴──────────────────┘
```

### Real-World Startup Scenarios

#### **Normal Startup (30 seconds)**
```
0s:  Pod starts
5s:  First liveness probe → UP (process responsive)
10s: Servlet container ready → Liveness first success
15s: Database connects → Readiness dependencies verified  
30s: All systems ready → Readiness first success
30s: Exit startup phase → System fully operational
```

#### **Plugin Upgrade Startup (10 minutes)**
```
0s:   Pod starts
5s:   First liveness probe → UP (process responsive)
10s:  Servlet container ready → Liveness first success
1m:   Plugin upgrades begin → Liveness UP, Readiness DOWN
10m:  Plugin upgrades complete → All dependencies ready
10m:  Readiness first success → Exit startup phase
```

#### **Database Migration Startup (30 minutes)**
```
0s:   Pod starts
5s:   First liveness probe → UP (process responsive) 
10s:  Servlet container ready → Liveness first success
2m:   Database migration begins → Liveness UP, Readiness DOWN
30m:  Database migration complete → All dependencies ready
30m:  Readiness first success → Exit startup phase
```

#### **Deadlocked Process (failure case)**
```
0s:   Pod starts
5s:   First liveness probe → UP (process responsive)
10s:  Process deadlocks → Health check requests timeout
10s:  Liveness probe → DOWN (timeout)
15s:  Kubernetes restarts pod (correct behavior)
```

### Configuration and Kubernetes Integration

#### **No Application-Level Timeouts**
The system **does not impose arbitrary time limits** on startup. Instead, Kubernetes probe configuration controls all timeout behavior:

```yaml
livenessProbe:
  httpGet:
    path: /livez
    port: 8080
  initialDelaySeconds: 30     # Wait before first probe
  periodSeconds: 30           # Probe every 30s
  timeoutSeconds: 10          # Individual probe timeout
  failureThreshold: 6         # Restart after 6 failures (3 minutes)

readinessProbe:
  httpGet:
    path: /readyz
    port: 8080
  initialDelaySeconds: 10     # Start checking earlier
  periodSeconds: 15           # Check more frequently
  timeoutSeconds: 10          # Individual probe timeout
  failureThreshold: 20        # Allow 5 minutes of failures
```

#### **Plugin Upgrade Considerations**
For environments with frequent plugin upgrades, adjust Kubernetes probe settings:

```yaml
# Extended timeouts for plugin upgrade environments
livenessProbe:
  failureThreshold: 20        # Allow 10 minutes (20 × 30s)
readinessProbe:
  failureThreshold: 40        # Allow 10 minutes (40 × 15s)
```

### Benefits of This Approach

#### ✅ **Prevents Unnecessary Restarts**
- Long plugin upgrades don't trigger pod restarts
- Database migrations are handled gracefully
- Complex initialization sequences are supported

#### ✅ **Accurate Traffic Routing**
- No traffic served until system is truly ready
- Load balancer correctly excludes initializing pods
- Clear distinction between "alive" and "ready"

#### ✅ **Kubernetes Best Practices**
- Liveness: "Is the process responsive?" (restart if not)
- Readiness: "Is the service ready for traffic?" (exclude if not)
- Platform controls lifecycle decisions, not application

#### ✅ **Operational Clarity**
- Clear logging of startup progression
- Structured data for monitoring startup times
- Diagnostic information for troubleshooting

### Startup Phase API

#### **Programmatic Access**
```java
HealthStateManager healthManager = HealthStateManager.getInstance();

// Check overall startup phase
boolean inStartup = healthManager.isInStartupPhase();

// Check specific probe startup phases
boolean livenessStartup = healthManager.isInLivenessStartupPhase();
boolean readinessStartup = healthManager.isInReadinessStartupPhase();

// Check first success status
boolean livenessSucceeded = healthManager.hasLivenessEverSucceeded();
boolean readinessSucceeded = healthManager.hasReadinessEverSucceeded();

// Get startup timing
String startupAge = healthManager.getStartupAge(); // "2m 30s"
```

#### **Monitoring Integration**
```bash
# Check startup status via API
curl -s http://localhost:8080/api/v1/health | jq '.checks[] | select(.name=="servlet-container") | .data.startupComplete'

# Monitor startup progression
curl -s http://localhost:8080/livez   # "alive" or "unhealthy"
curl -s http://localhost:8080/readyz  # "ready" or "not ready"
```

### Troubleshooting Startup Issues

#### **Common Patterns**
```bash
# Liveness succeeds but readiness fails for extended period
# → Check database connectivity, plugin status, external dependencies

# Both liveness and readiness fail
# → Check servlet container, JVM memory, core application issues

# Liveness intermittent during startup
# → Check GC pressure, memory allocation, thread contention

# Very long startup times
# → Check plugin upgrade logs, database migration status
```

#### **Diagnostic Commands**
```bash
# Check current startup phase
curl -s http://localhost:8080/api/v1/health | jq '.checks[] | select(.name=="servlet-container") | {startupComplete: .data.startupComplete, startupTime: .data.startupTimeMs}'

# Monitor startup progression
watch -n 5 'echo "Liveness: $(curl -s http://localhost:8080/livez)" && echo "Readiness: $(curl -s http://localhost:8080/readyz)"'

# Check detailed health status
curl -s http://localhost:8080/api/v1/health | jq '.status, .checks[].name, .checks[].status'
```

This startup phase management ensures reliable health reporting throughout the entire application lifecycle, from initial startup through complex operational scenarios.

---

## 📊 **Structured Data in Health Checks**

### Overview

**All health checks now include structured data** for machine-parsable monitoring and automation. The structured data provides the same information that appears in human-readable messages, ensuring consistency between human operators and automated systems.

**✅ Complete Implementation Status:**
- **Database Health Check** - `dbVersion`, `connectionTimeMs`, error types
- **Memory Health Check** - Memory usage percentages, thresholds, error types  
- **Thread Health Check** - Thread counts, thresholds, deadlock information
- **System Health Check** - Processors, Java version, OS, disk space
- **Garbage Collection Health Check** - GC metrics, thresholds, startup phase detection
- **Servlet Container Health Check** - Startup time, server info, connector counts
- **Cache Health Check** - Operation timing, timeout configuration
- **Elasticsearch Health Check** - API availability, response times, timeouts
- **CDI Initialization Health Check** - Initialization status and timing

### Design Principles

1. **Message-Data Consistency**: All data used in human-readable messages is included in the structured data field
2. **Optional Enhancement**: Structured data is optional - existing functionality remains unchanged
3. **Error Context**: Include `errorType` field when health checks fail for automated categorization
4. **Performance Metrics**: Include timing and threshold information for monitoring trends
5. **Configuration Transparency**: Expose relevant configuration values (thresholds, timeouts) for operational visibility

### Structured Data Examples

#### **Database Health Check**
```json
{
  "name": "database",
  "status": "pass", 
  "message": "Database connection OK (DB version: 250113)",
  "data": {
    "dbVersion": 250113,
    "connectionTimeMs": 0
  }
}
```

When errors occur:
```json
{
  "name": "database",
  "status": "fail",
  "message": "Database connection failed: timeout after 2000ms",
  "data": {
    "dbVersion": 250113,
    "connectionTimeMs": 2000,
    "errorType": "database_connection"
  }
}
```

#### **Memory Health Check**
```json
{
  "name": "application", 
  "status": "pass",
  "message": "Application healthy: Memory usage: 33.8% (346MB used / 1024MB max)",
  "data": {
    "memoryUsagePercent": 33.8,
    "memoryUsedMB": 346,
    "memoryMaxMB": 1024,
    "warningThreshold": 80,
    "criticalThreshold": 90
  }
}
```

When critical:
```json
{
  "name": "application",
  "status": "fail", 
  "message": "Critical memory usage: 94.2%",
  "data": {
    "memoryUsagePercent": 94.2,
    "memoryUsedMB": 1476,
    "memoryMaxMB": 1568,
    "warningThreshold": 80,
    "criticalThreshold": 90,
    "errorType": "memory_pressure"
  }
}
```

#### **Thread Health Check**
```json
{
  "name": "threads",
  "status": "pass",
  "message": "Thread system healthy: 45 threads (threshold: 5000), no deadlocks",
  "data": {
    "currentThreadCount": 45,
    "threshold": 5000,
    "hasDeadlocks": false
  }
}
```

When deadlocks detected:
```json
{
  "name": "threads", 
  "status": "fail",
  "message": "Thread system issues: Deadlock detected (3 threads); High thread count (520 > 500)",
  "data": {
    "currentThreadCount": 520,
    "threshold": 500, 
    "hasDeadlocks": true,
    "deadlockedThreadCount": 3,
    "errorType": "thread_system"
  }
}
```

#### **System Health Check**
```json
{
  "name": "system",
  "status": "pass",
  "message": "System OK - Processors: 10, Java: 21.0.7, OS: Linux, Free disk: 52162 MB",
  "data": {
    "processors": 10,
    "javaVersion": "21.0.7",
    "osName": "Linux",
    "freeDiskMB": 52162
  }
}
```

When errors occur:
```json
{
  "name": "system",
  "status": "fail",
  "message": "System health check failed",
  "data": {
    "processors": 10,
    "javaVersion": "21.0.7",
    "osName": "Linux",
    "freeDiskMB": 52162,
    "errorType": "system_resources"
  }
}
```

#### **Garbage Collection Health Check**
```json
{
  "name": "gc",
  "status": "pass",
  "message": "GC performance healthy - Time: 0.0%, Frequency: 0.0/min, Collections: 14 [STARTUP - higher thresholds]",
  "data": {
    "gcTimePercent": 0.0,
    "gcFrequencyPerMin": 0.0,
    "totalCollections": 72,
    "isStartupPhase": true,
    "timeThresholdPercent": 90,
    "frequencyThreshold": 500
  }
}
```

When GC pressure detected:
```json
{
  "name": "gc",
  "status": "fail",
  "message": "GC performance issues: High GC time (35.2% > 30%); High GC frequency (120.5 > 100 per min)",
  "data": {
    "gcTimePercent": 35.2,
    "gcFrequencyPerMin": 120.5,
    "totalCollections": 1250,
    "isStartupPhase": false,
    "timeThresholdPercent": 30,
    "frequencyThreshold": 100,
    "errorType": "gc_pressure"
  }
}
```

#### **Servlet Container Health Check**
```json
{
  "name": "servlet-container",
  "status": "pass",
  "message": "Servlet container responsive, HTTP connectors ready, self-check passed, dotCMS startup complete (18177ms)",
  "data": {
    "startupTimeMs": 18177,
    "startupComplete": true,
    "servletApiVersion": "4.0",
    "serverInfo": "Apache Tomcat/9.0.85",
    "httpConnectorCount": 4
  }
}
```

When startup incomplete:
```json
{
  "name": "servlet-container",
  "status": "fail",
  "message": "dotCMS startup process not yet complete",
  "data": {
    "startupComplete": false,
    "servletApiVersion": "4.0",
    "serverInfo": "Apache Tomcat/9.0.85",
    "errorType": "servlet_container"
  }
}
```

#### **Cache Health Check**
```json
{
  "name": "cache",
  "status": "pass",
  "message": "Cache write/read/delete successful",
  "data": {
    "operationTimeMs": 20,
    "timeoutMs": 1000,
    "testGroup": "health.check"
  }
}
```

When cache operations fail:
```json
{
  "name": "cache",
  "status": "fail",
  "message": "Cache operations failed: timeout after 2000ms",
  "data": {
    "operationTimeMs": 2000,
    "timeoutMs": 2000,
    "testGroup": "health.check",
    "errorType": "cache_operations"
  }
}
```

#### **Elasticsearch Health Check**
```json
{
  "name": "elasticsearch",
  "status": "pass",
  "message": "Elasticsearch API available",
  "data": {
    "apiAvailable": true,
    "responseTimeMs": 437,
    "timeoutMs": 2000
  }
}
```

When Elasticsearch unavailable:
```json
{
  "name": "elasticsearch",
  "status": "fail",
  "message": "Elasticsearch API not available",
  "data": {
    "apiAvailable": false,
    "responseTimeMs": 2000,
    "timeoutMs": 2000,
    "errorType": "elasticsearch_connectivity"
  }
}
```

#### **CDI Initialization Health Check**
```json
{
  "name": "cdi-initialization",
  "status": "pass",
  "message": "CDI container fully initialized",
  "data": {
    "cdiInitialized": true,
    "initializationTimeMs": 0
  }
}
```

When CDI initialization fails:
```json
{
  "name": "cdi-initialization",
  "status": "fail",
  "message": "CDI not ready: CDI container not available",
  "data": {
    "cdiInitialized": false,
    "initializationTimeMs": 1500,
    "initializationError": "CDI container not available",
    "errorType": "cdi_initialization"
  }
}
```

### Using Structured Data in Monitoring

#### **Prometheus/Grafana Alerting**
```yaml
# Alert when memory usage exceeds threshold
- alert: DotCMSHighMemoryUsage
  expr: dotcms_health_data{check="application",field="memoryUsagePercent"} > 85
  annotations:
    summary: "dotCMS memory usage above {{ $value }}%"
    
# Alert when database version mismatches expected
- alert: DotCMSDatabaseVersionMismatch  
  expr: dotcms_health_data{check="database",field="dbVersion"} != 250113
  annotations:
    summary: "Unexpected database version: {{ $value }}"

# Alert when GC pressure is high
- alert: DotCMSHighGCPressure
  expr: dotcms_health_data{check="gc",field="gcTimePercent"} > 30
  annotations:
    summary: "dotCMS GC time above {{ $value }}%"

# Alert when startup time is excessive
- alert: DotCMSSlowStartup
  expr: dotcms_health_data{check="servlet-container",field="startupTimeMs"} > 30000
  annotations:
    summary: "dotCMS startup took {{ $value }}ms (>30s)"

# Alert when Elasticsearch response time is slow
- alert: DotCMSSlowElasticsearch
  expr: dotcms_health_data{check="elasticsearch",field="responseTimeMs"} > 1000
  annotations:
    summary: "Elasticsearch response time {{ $value }}ms (>1s)"
```

#### **Automated Response Scripts**
```bash
#!/bin/bash
# Extract structured data for automated responses
MEMORY_USAGE=$(curl -s /api/v1/health | jq '.checks[] | select(.name=="application") | .data.memoryUsagePercent')
THRESHOLD=$(curl -s /api/v1/health | jq '.checks[] | select(.name=="application") | .data.criticalThreshold')

if (( $(echo "$MEMORY_USAGE > $THRESHOLD" | bc -l) )); then
    echo "Triggering memory cleanup - usage: ${MEMORY_USAGE}% > ${THRESHOLD}%"
    # Trigger cleanup actions
fi

# Check GC pressure
GC_TIME=$(curl -s /api/v1/health | jq '.checks[] | select(.name=="gc") | .data.gcTimePercent')
GC_THRESHOLD=$(curl -s /api/v1/health | jq '.checks[] | select(.name=="gc") | .data.timeThresholdPercent')

if (( $(echo "$GC_TIME > $GC_THRESHOLD" | bc -l) )); then
    echo "High GC pressure detected: ${GC_TIME}% > ${GC_THRESHOLD}%"
    # Trigger GC optimization actions
fi

# Check system resources
FREE_DISK=$(curl -s /api/v1/health | jq '.checks[] | select(.name=="system") | .data.freeDiskMB')
if (( $(echo "$FREE_DISK < 1000" | bc -l) )); then
    echo "Low disk space: ${FREE_DISK}MB remaining"
    # Trigger cleanup actions
fi
```

#### **Log Analysis and Correlation**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "WARN", 
  "message": "dotCMS health check warning",
  "check": "database",
  "status": "warn",
  "data": {
    "dbVersion": 250113,
    "connectionTimeMs": 1800,
    "errorType": "slow_connection"
  }
}
```

---

## ⚡ **Event-Driven Health Monitoring**

### Faster Failure Detection Without Polling Overhead

The health check system supports **event-driven monitoring** for critical external dependencies like Database and Elasticsearch. This provides **1-2 second failure detection** without the overhead of frequent polling.

#### How Event-Driven Monitoring Works

**Database Monitoring (HikariCP Integration):**
```
Traditional Polling: Query database every 30 seconds
├─ High database load from health check queries
├─ 30-60 second delay to detect failures
└─ Resource waste during normal operations

Event-Driven Monitoring: Monitor connection pool via JMX
├─ Monitor pool metrics every 5 seconds (lightweight)
├─ Detect failures in 1-2 seconds
├─ Reduce health check queries by 3x (180s intervals)
└─ Exponential backoff recovery testing
```

**Elasticsearch Monitoring (Client Integration):**
```
Traditional Polling: Test cluster health every 30 seconds
├─ Network overhead from frequent health requests
├─ 30-60 second delay to detect node failures
└─ May miss transient connection issues

Event-Driven Monitoring: Monitor client connection events
├─ Detect connection failures immediately
├─ Track client-side error patterns
├─ Reduce cluster health requests by 3x
└─ Real-time status change callbacks
```

#### Configuration

```properties
# Enable event-driven monitoring for database
health.check.database.event-driven.enabled=true

# Enable event-driven monitoring for Elasticsearch  
health.check.elasticsearch.event-driven.enabled=true
```

#### Benefits

- **⚡ Faster Detection**: 1-2 seconds vs 30-60 seconds
- **📉 Reduced Load**: 3x fewer database/ES health queries
- **🔄 Real-time Updates**: Immediate status change notifications
- **🛡️ Automatic Fallback**: Falls back to polling if event monitoring fails
- **📊 Better Observability**: Detailed connection pool and client metrics

---

## 🔌 **Standard Endpoints**

### Kubernetes Standard Endpoints (Minimal Responses)
- **`/livez`** - Liveness probe (text: "alive" or "unhealthy")
- **`/readyz`** - Readiness probe (text: "ready" or "not ready")  
- **`/healthz`** - Simple server check (text: "ok")

### REST API Endpoints (Admin Only)
- **`/api/v1/health`** - Overall health (requires CMS Admin role)
- **`/api/v1/health/liveness`** - Liveness status (public)
- **`/api/v1/health/readiness`** - Readiness status (public)
- **`/api/v1/health/check/{name}`** - Individual check (requires admin)

---

## 🛠️ **Creating Health Checks**

### Method 1: Extend HealthCheckBase (Recommended)

```java
import com.dotcms.health.util.HealthCheckBase;

/**
 * Example health check that monitors custom service status.
 * Safe for both liveness and readiness since it only checks internal state.
 */
public class MyServiceHealthCheck extends HealthCheckBase {
    
    @Override
    protected CheckResult performCheck() throws Exception {
        // Use utility for timing and measurement
        return measureExecution(() -> {
            // Your health check logic here
            boolean isHealthy = checkMyInternalService();
            
            if (!isHealthy) {
                throw new Exception("MyService is not responding");
            }
            
            return "MyService is operational"; // Success message
        });
    }
    
    /**
     * Override to provide structured data for monitoring tools.
     * Focus on error-related information and threshold data.
     */
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // IMPORTANT: Include all data referenced in human-readable messages
        // This ensures consistency between human and machine interfaces
        
        // Include service-specific metrics when relevant
        if (result.error != null) {
            data.put("errorType", "service_unresponsive");
            data.put("lastSuccessfulCheck", getLastSuccessfulCheckTime());
        }
        
        // Include threshold-related data that appears in messages
        long responseTimeMs = getLastResponseTime();
        if (responseTimeMs > 0) {
            data.put("responseTimeMs", responseTimeMs);
            data.put("responseTimeThreshold", 2000);
        }
        
        return data;
    }
    
    @Override
    public String getName() {
        return "my-service";
    }
    
    @Override
    public int getOrder() {
        return 50; // 0-10: core checks, 10-50: app checks, 50+: dependency checks
    }
    
    /**
     * EXPLICIT: Safe for liveness - only checks internal service state
     */
    @Override
    public boolean isLivenessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    /**
     * EXPLICIT: Essential for readiness
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        return "Monitors internal MyService component health and responsiveness";
    }
}
```

**Configuration for this check:**
```properties
health.check.my-service.mode=PRODUCTION
health.check.my-service.timeout-ms=2000
health.check.my-service.custom-property=value
```

### Method 2: Database Health Check Example (External Dependency)

```java
import com.dotcms.health.util.HealthCheckBase;
import com.dotcms.health.util.HealthCheckUtils;

/**
 * Database connectivity health check - READINESS ONLY.
 * External dependency checks should NEVER be used for liveness probes.
 */
@ApplicationScoped
public class MyDatabaseHealthCheck extends HealthCheckBase {
    
    @Override
    protected CheckResult performCheck() throws Exception {
        int timeoutMs = getConfigProperty("timeout-ms", 2000);
        int retryCount = getConfigProperty("retry-count", 2);
        
        return measureExecution(() -> {
            // Use utility for consistent database testing
            return HealthCheckUtils.testDatabaseConnectivity(timeoutMs);
        });
    }
    
    /**
     * Provide database-specific structured data for monitoring
     */
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Include database version for compatibility analysis
        if (Config.DB_VERSION > 0) {
            data.put("dbVersion", Config.DB_VERSION);
        }
        
        // Include connection timing data
        if (result.durationMs != null) {
            data.put("connectionTimeMs", result.durationMs);
        }
        
        // Include error classification
        if (result.error != null) {
            data.put("errorType", "database_connection");
            if (result.durationMs != null && result.durationMs > getConfigProperty("timeout-ms", 2000)) {
                data.put("timeoutMs", getConfigProperty("timeout-ms", 2000));
            }
        }
        
        return data;
    }
    
    @Override
    public String getName() {
        return "my-database";
    }
    
    @Override
    public int getOrder() {
        return 60; // Dependency checks have higher order numbers
    }
    
    /**
     * EXPLICIT: NOT safe for liveness - external dependency
     */
    @Override
    public boolean isLivenessCheck() {
        return false; // Never check external deps for liveness
    }
    
    /**
     * EXPLICIT: Essential for readiness
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        return "Verifies database connectivity and basic query operations";
    }
}
```

### Method 3: Implement HealthCheck Directly (Advanced)

```java
import com.dotcms.health.api.HealthCheck;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class CustomHealthCheck implements HealthCheck {
    
    @Override
    public HealthCheckResult check() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Your health check logic
            boolean isHealthy = performCustomCheck();
            
            String message = isHealthy ? "Custom service operational" : "Custom service failed";
            HealthStatus status = isHealthy ? HealthStatus.UP : HealthStatus.DOWN;
            
            // Build structured data
            Map<String, Object> data = new HashMap<>();
            if (!isHealthy) {
                data.put("errorType", "custom_service_failure");
            }
            data.put("serviceVersion", getServiceVersion());
            
            return HealthCheckResult.builder()
                .name(getName())
                .status(status)
                .message(message)
                .data(Optional.of(data))
                .lastChecked(Instant.now())
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
                
        } catch (Exception e) {
            Logger.error(this, "Custom health check failed", e);
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "check_execution_failure");
            errorData.put("exceptionClass", e.getClass().getSimpleName());
            
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.DOWN)
                .error("Health check failed: " + e.getMessage())
                .data(Optional.of(errorData))
                .lastChecked(Instant.now())
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }
    
    @Override
    public String getName() {
        return "custom-check";
    }
    
    @Override
    public boolean isLivenessCheck() {
        return true; // Only if this checks internal state
    }
    
    @Override
    public boolean isReadinessCheck() {
        return true;
    }
    
    private boolean performCustomCheck() {
        // Your implementation
        return true;
    }
}
```

---

## 📋 **CDI Integration**

### Automatic Discovery with CDI

```java
import com.dotcms.health.api.HealthCheckProvider;

@ApplicationScoped
public class MyHealthCheckProvider implements HealthCheckProvider {
    
    // CDI will automatically discover this provider
    
    @Override
    public List<HealthCheck> getHealthChecks() {
        return Arrays.asList(
            new MyServiceHealthCheck(),
            new MyDatabaseHealthCheck()
        );
    }
    
    @Override 
    public String getProviderName() {
        return "my-module-health-checks";
    }
}
```

### Configuration Management in Health Checks

```java
public class MyHealthCheck extends HealthCheckBase {
    
    @Override
    protected CheckResult performCheck() throws Exception {
        // Get configuration using the base class utility
        int timeoutMs = getConfigProperty("timeout-ms", 2000);
        String endpoint = getConfigProperty("endpoint", "http://localhost:8080");
        boolean enableRetries = getConfigProperty("enable-retries", true);
        
        // Configuration property name will be: health.check.{getName()}.timeout-ms
        // Use the configuration...
    }
}
```

---

## 🛡️ **Safe Deployment Strategy**

### Step 1: Initial Deployment (Memory/GC tuning focus)

Start with memory and GC checks in MONITOR_MODE for tuning, but database/ES can be PRODUCTION since they're stable and readiness-only:

```properties
# Memory and GC checks in MONITOR_MODE for tuning - can be aggressive during startup
health.check.application.mode=MONITOR_MODE
health.check.garbage-collection.mode=MONITOR_MODE
health.check.threads.mode=MONITOR_MODE

# Database and Elasticsearch can use PRODUCTION - they're stable and readiness-only
health.check.database.mode=PRODUCTION
health.check.elasticsearch.mode=PRODUCTION
health.check.cache.mode=MONITOR_MODE

# Conservative thresholds for memory checks
health.check.application.critical-threshold-percent=98
health.check.garbage-collection.cpu-threshold-percent=50
health.check.threads.pool-threshold-multiplier=1000

# Generous timeouts
health.check.database.timeout-ms=5000
health.check.cache.timeout-ms=3000
```

### Step 2: Progressive Enablement

```properties
# Phase 1: Tune memory thresholds based on observed MONITOR_MODE warnings
health.check.application.critical-threshold-percent=95  # Tighten if no issues seen
health.check.garbage-collection.cpu-threshold-percent=40  # Tighten if appropriate

# Phase 2: Enable memory checks after tuning (database/ES already in PRODUCTION)
health.check.application.mode=PRODUCTION
health.check.garbage-collection.mode=PRODUCTION

# Phase 3: Enable thread checks after stability verified
health.check.threads.mode=PRODUCTION

# Phase 4: Enable cache checks last (most variable)
health.check.cache.mode=PRODUCTION
```

### Step 3: Monitoring Integration

```properties
# Enable detailed monitoring data for operations
health.include.system-details=true
health.include.performance-metrics=true

# Configure structured data export (if using external monitoring)
health.monitoring.export.prometheus.enabled=true
health.monitoring.export.json.enabled=true
```

---

## 🧰 **Utility Methods**

The `HealthCheckUtils` class provides common functionality for health checks:

### Database and Cache Utilities

```java
public class MyHealthCheck extends HealthCheckBase {
    
    @Override
    protected CheckResult performCheck() throws Exception {
        int timeoutMs = getConfigProperty("timeout-ms", 2000);
        
        return measureExecution(() -> {
            // Use utilities for common operations
            String dbResult = HealthCheckUtils.testDatabaseConnectivity(timeoutMs);
            
            // Test cache operations
            String cacheResult = HealthCheckUtils.testCacheOperations(
                "health-test-key", "health-group", timeoutMs);
            
            // Execute with timeout
            String apiResult = HealthCheckUtils.executeWithTimeout(() -> {
                return callExternalAPI();
            }, timeoutMs, "external-api-call");
            
            return "All checks passed: " + dbResult + ", " + cacheResult + ", " + apiResult;
        });
    }
}
```

### Memory and System Utilities

```java
// Get memory usage percentage
double memoryUsage = HealthCheckUtils.getMemoryUsagePercent();

// Get system load information
String systemLoad = HealthCheckUtils.getSystemLoadInfo();

// Check disk space
boolean diskOk = HealthCheckUtils.checkDiskSpace("/tmp", 100 * 1024 * 1024); // 100MB minimum
```

---

## 📊 **Monitoring and Observability**

### Health Status Hierarchy

```
HealthStatus.UP        → All checks passing
HealthStatus.DEGRADED  → Non-critical issues (does NOT fail probes)
HealthStatus.DOWN      → Critical issues (WILL fail probes)
HealthStatus.UNKNOWN   → Check hasn't run or status unclear
```

### Log Message Examples

**MONITOR_MODE:**
```log
WARN: Database health check: MONITOR_MODE - would normally be DOWN but converted to DEGRADED for safe deployment
INFO: Database connection failed (timeout after 2000ms) but not blocking traffic due to MONITOR_MODE
```

**PRODUCTION Mode:**
```log
INFO: GC health check: High GC pressure detected (35% CPU time) - status DOWN
INFO: Thread health check: Elevated thread count (1500 threads) - status DOWN
```

### JSON Response Format (RFC-Compliant with Structured Data)

```json
{
  "status": "UP",
  "version": "1.0.0-SNAPSHOT",
  "releaseId": "578f6ed",
  "serviceId": "dotcms-health",
  "description": "dotCMS Application Health Status",
  "timestamp": 1749722097342,
  "checks": [
    {
      "name": "application",
      "status": "UP",
      "message": "Application healthy: Memory usage: 33.8% (346MB used / 1024MB max) [0ms]",
      "data": {
        "memoryUsagePercent": 33.8,
        "memoryUsedMB": 346,
        "memoryMaxMB": 1024,
        "warningThreshold": 80,
        "criticalThreshold": 90
      },
      "durationMs": 0,
      "lastChecked": 1749722083199,
      "monitorModeApplied": false
    },
    {
      "name": "database",
      "status": "UP",
      "message": "Database connection OK (DB version: 250113) [0ms]",
      "data": {
        "dbVersion": 250113,
        "connectionTimeMs": 0
      },
      "durationMs": 0,
      "lastChecked": 1749722083203,
      "monitorModeApplied": true
    },
    {
      "name": "system",
      "status": "UP",
      "message": "System OK - Processors: 10, Java: 21.0.7, OS: Linux, Free disk: 52162 MB [0ms]",
      "data": {
        "processors": 10,
        "javaVersion": "21.0.7",
        "osName": "Linux",
        "freeDiskMB": 52162
      },
      "durationMs": 0,
      "lastChecked": 1749722083200,
      "monitorModeApplied": false
    },
    {
      "name": "threads",
      "status": "UP", 
      "message": "Thread system healthy: 45 threads (threshold: 5000), no deadlocks [0ms]",
      "data": {
        "currentThreadCount": 45,
        "threshold": 5000,
        "hasDeadlocks": false
      },
      "durationMs": 0,
      "lastChecked": 1749722083201,
      "monitorModeApplied": true
    },
    {
      "name": "gc",
      "status": "UP",
      "message": "GC performance healthy - Time: 0.0%, Frequency: 0.0/min, Collections: 72 [STARTUP - higher thresholds] [0ms]",
      "data": {
        "gcTimePercent": 0.0,
        "gcFrequencyPerMin": 0.0,
        "totalCollections": 72,
        "isStartupPhase": true,
        "timeThresholdPercent": 90,
        "frequencyThreshold": 500
      },
      "durationMs": 0,
      "lastChecked": 1749722083202,
      "monitorModeApplied": false
    },
    {
      "name": "servlet-container",
      "status": "UP",
      "message": "Servlet container responsive, HTTP connectors ready, self-check passed, dotCMS startup complete (18177ms) [0ms]",
      "data": {
        "startupTimeMs": 18177,
        "startupComplete": true,
        "servletApiVersion": "4.0",
        "serverInfo": "Apache Tomcat/9.0.85",
        "httpConnectorCount": 4
      },
      "durationMs": 0,
      "lastChecked": 1749722083203,
      "monitorModeApplied": false
    },
    {
      "name": "cache",
      "status": "UP",
      "message": "Cache write/read/delete successful [0ms]",
      "data": {
        "operationTimeMs": 0,
        "timeoutMs": 1000,
        "testGroup": "health.check"
      },
      "durationMs": 0,
      "lastChecked": 1749722083202,
      "monitorModeApplied": true
    },
    {
      "name": "elasticsearch",
      "status": "UP",
      "message": "Elasticsearch API available [437ms]",
      "data": {
        "apiAvailable": true,
        "responseTimeMs": 437,
        "timeoutMs": 2000
      },
      "durationMs": 437,
      "lastChecked": 1749722083209,
      "monitorModeApplied": true
    },
    {
      "name": "cdi-initialization",
      "status": "UP",
      "message": "CDI container fully initialized [0ms]",
      "data": {
        "cdiInitialized": true,
        "initializationTimeMs": 0
      },
      "durationMs": 0,
      "lastChecked": 1749722083201,
      "monitorModeApplied": false
    }
  ]
}
```

---

## 🚨 **Troubleshooting Guide**

### Common Issues

#### 1. Health Check Always Returns DOWN
```java
// Check the mode configuration
HealthCheckMode mode = getMode();
if (mode == HealthCheckMode.DISABLED) {
    // Check is disabled
}

// Verify configuration naming
String configKey = "health.check." + getName() + ".mode";
String configValue = Config.getStringProperty(configKey, "PRODUCTION");
```

#### 2. CDI Health Checks Not Discovered
```log
# Look for these log messages:
INFO: CDI integration enabled for health checks
INFO: Discovered 3 CDI health checks from provider: my-module

# If missing:
WARN: CDI not available for health checks (this is normal during early startup)
```

#### 3. Kubernetes Probes Failing
```bash
# Test endpoints directly:
curl -v http://localhost:8080/livez     # Should return "alive" or "unhealthy"
curl -v http://localhost:8080/readyz    # Should return "ready" or "not ready"

# Check detailed status:
curl -v http://localhost:8080/api/v1/health    # Detailed JSON response

# Check structured data:
curl -s http://localhost:8080/api/v1/health | jq '.checks[].data'
```

#### 4. Structured Data Missing or Incorrect
```bash
# Test individual health check structured data
curl -s http://localhost:8080/api/v1/health | jq '.checks[] | select(.name=="database") | .data'

# Verify data field types
curl -s http://localhost:8080/api/v1/health | jq '.checks[] | select(.data) | {name, data}'
```

#### 5. Performance Issues
```properties
# Reduce background check frequency
health.interval-seconds=120

# Reduce thread pool size
health.thread-pool-size=1

# Disable expensive checks temporarily
health.check.expensive-check.mode=DISABLED
```

### Emergency Procedures

**Disable All Advanced Checks:**
```properties
health.check.threads.mode=DISABLED
health.check.garbage-collection.mode=DISABLED
health.check.database.mode=DISABLED
health.check.cache.mode=DISABLED
health.check.elasticsearch.mode=DISABLED
```

**Emergency Safe Mode:**
```properties
# Convert all to safe mode
health.check.threads.mode=MONITOR_MODE
health.check.garbage-collection.mode=MONITOR_MODE
health.check.database.mode=MONITOR_MODE
health.check.cache.mode=MONITOR_MODE
```

**Bypass Kubernetes Probes (Emergency Only):**
```yaml
# Use always-succeeding command temporarily
livenessProbe:
  exec:
    command: ["/bin/true"]
readinessProbe:
  exec:
    command: ["/bin/true"]
```

---

## 🧰 **Programmatic HealthService API**

### CDI Injection for Business Logic

The `HealthService` interface provides a convenient CDI-injectable API for programmatic health checking in business logic, circuit breakers, and administrative interfaces.

#### **Basic Usage**

```java
import com.dotcms.health.api.HealthService;

@ApplicationScoped
public class MyBusinessService {
    
    @Inject
    private HealthService healthService;
    
    public void performOperation() {
        // Quick health checks before operations
        if (!healthService.isDatabaseHealthy()) {
            throw new ServiceUnavailableException("Database is not available");
        }
        
        if (!healthService.isSearchServiceHealthy()) {
            // Use structured data for decision making
            Optional<HealthCheckResult> dbHealth = healthService.getDatabaseHealth();
            if (dbHealth.isPresent() && dbHealth.get().data().isPresent()) {
                Map<String, Object> data = dbHealth.get().data().get();
                Long connectionTime = (Long) data.get("connectionTimeMs");
                if (connectionTime != null && connectionTime > 1000) {
                    // Use optimized queries for slow database
                    return performOptimizedOperation();
                }
            }
            
            // Proceed with full functionality
            return performAdvancedOperation();
        }
        
        // Proceed with full functionality
        return performAdvancedOperation();
    }
}
```

#### **Database Convenience Methods**

```java
// Quick boolean checks
boolean isDatabaseHealthy();                    // Returns true if database is UP
boolean refreshAndCheckDatabaseHealth(boolean blocking);  // Force refresh and check

// Detailed information
Optional<HealthCheckResult> getDatabaseHealth();          // Get full database health details
```

#### **Search Service Convenience Methods**

```java
// Quick boolean checks  
boolean isSearchServiceHealthy();               // Returns true if Elasticsearch is UP
boolean refreshAndCheckSearchServiceHealth(boolean blocking); // Force refresh and check

// Detailed information
Optional<HealthCheckResult> getSearchServiceHealth();     // Get full search service health details
```

#### **General Health Service Methods**

```java
// Overall system health
HealthResponse getOverallHealth();              // All health checks
HealthResponse getLivenessHealth();             // Liveness checks only
HealthResponse getReadinessHealth();            // Readiness checks only
boolean isAlive();                              // Quick liveness check
boolean isReady();                              // Quick readiness check

// Individual health checks
Optional<HealthCheckResult> getHealthCheck(String name);   // Get specific health check
boolean isHealthCheckUp(String name);                     // Quick check for specific service
List<String> getHealthCheckNames();                       // List all available checks

// Force refresh operations
void refreshHealthChecks();                     // Refresh all checks
boolean refreshHealthCheck(String name);        // Refresh specific check
```

### **Circuit Breaker Integration Example**

```java
@ApplicationScoped
public class DatabaseCircuitBreaker {
    
    @Inject
    private HealthService healthService;
    
    public <T> T executeWithCircuitBreaker(Callable<T> operation) throws Exception {
        // Check health before executing
        Optional<HealthCheckResult> dbHealth = healthService.getDatabaseHealth();
        
        if (dbHealth.isPresent() && dbHealth.get().data().isPresent()) {
            Map<String, Object> data = dbHealth.get().data().get();
            
            // Use structured data for circuit breaker decisions
            String errorType = (String) data.get("errorType");
            if ("database_connection".equals(errorType)) {
                throw new CircuitBreakerOpenException("Database connection failures detected");
            }
            
            Long connectionTime = (Long) data.get("connectionTimeMs");
            if (connectionTime != null && connectionTime > 5000) {
                throw new CircuitBreakerOpenException("Database response time too slow: " + connectionTime + "ms");
            }
        }
        
        return operation.call();
    }
}
```

---

## 🔧 **Developer Reference**

### Core Interfaces

#### HealthCheck Interface
```java
public interface HealthCheck {
    HealthCheckResult check();                    // Perform the check
    String getName();                             // Unique check name
    default int getOrder() { return 100; }       // Execution order
    default boolean isLivenessCheck() { return false; }    // Liveness safety
    default boolean isReadinessCheck() { return true; }    // Readiness inclusion
    default String getDescription() { return "..."; }      // Human description
}
```

### HealthCheckBase Abstract Class
```java
public abstract class HealthCheckBase implements HealthCheck {
    protected abstract CheckResult performCheck() throws Exception;
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode);
    protected final CheckResult measureExecution(Callable<String> operation);
    protected final HealthCheckMode getMode();
    protected final <T> T getConfigProperty(String key, T defaultValue);
    // ... full template method implementation
}
```

### HealthCheckUtils Static Methods
```java
public final class HealthCheckUtils {
    public static String testDatabaseConnectivity(long timeoutMs);
    public static String testCacheOperations(String key, String group, long timeoutMs);
    public static <T> T executeWithTimeout(Callable<T> operation, long timeoutMs, String name);
    public static double getMemoryUsagePercent();
    public static String getSystemLoadInfo();
    public static boolean checkDiskSpace(String path, long minimumBytes);
}
```

---

## 🎓 **Best Practices Summary**

### Development Guidelines
1. **Extend HealthCheckBase** for automatic safety mode handling
2. **Use explicit isLivenessCheck() and isReadinessCheck()** methods
3. **Never check external dependencies in liveness probes**
4. **Follow configuration naming convention**: `health.check.{name}.{property}`
5. **Provide clear, actionable error messages**
6. **Use HealthCheckUtils for common operations**
7. **Override buildStructuredData()** to provide machine-parsable monitoring information
8. **Focus structured data on error/threshold information**, not comprehensive metrics
9. **Ensure message-data consistency**: All data referenced in human-readable messages must be available in the structured data field

### Deployment Guidelines
1. **Start with MONITOR_MODE for all new checks**
2. **Use /livez and /readyz for Kubernetes probes**
3. **Monitor logs for DEGRADED conditions**
4. **Progressively enable stricter checking**
6. **Use structured data for automated monitoring and alerting**

### Configuration Guidelines
1. **Use MONITOR_MODE for risky checks initially**
2. **Set conservative timeouts and thresholds**
3. **Document all configuration properties**
4. **Test configuration changes in non-production first**
5. **Have emergency disable procedures ready**

### Monitoring Guidelines
1. **Use structured data for automated systems**
2. **Keep human-readable messages for operators**
3. **Design monitoring alerts based on data fields**
4. **Correlate health data with application metrics**
5. **Test monitoring automation with different health states**

This health check system provides production-ready monitoring with built-in safety mechanisms to prevent service disruption while enabling comprehensive observability and gradual hardening as confidence grows. The dual approach of human-readable messages and machine-parsable structured data ensures the system serves both operational teams and automated monitoring infrastructure effectively.

## 🔒 **Filter Chain Configuration**

The infrastructure monitoring system uses a specialized filter chain to ensure both health check and management endpoints remain fast, reliable, and resilient. The `InfrastructureFilter` manages which filters are allowed to process infrastructure monitoring requests.

#### Infrastructure Endpoints

The filter bypass applies to these endpoint types:

**Health Check Endpoints:**
- `/livez` - Kubernetes liveness probe
- `/readyz` - Kubernetes readiness probe  
- `/api/v1/health` - Health check API

**Management Endpoints:**
- `/dotmgt/livez` - Management port liveness probe
- `/dotmgt/readyz` - Management port readiness probe
- `/dotmgt/health` - Management port health status
- All other `/dotmgt/*` endpoints

#### Essential Filters (Always Processed)

The following filters are essential for infrastructure endpoints and are always processed:

1. **NormalizationFilter**
   - Validates and normalizes request URIs
   - Prevents path traversal attacks
   - Blocks malicious characters

2. **HttpHeaderSecurityFilter**
   - Adds security headers (HSTS, X-Frame-Options, etc.)
   - Prevents XSS and clickjacking attacks
   - Ensures secure response headers

3. **CookiesFilter**
   - Manages secure cookie settings
   - Sets HttpOnly and Secure flags
   - Ensures proper cookie security

#### Excluded Filters

The following filters are excluded from infrastructure endpoint requests to prevent unnecessary processing and potential failures:

- **CharsetEncodingFilter** - Character encoding (not needed for infrastructure endpoints)
- **ThreadNameFilter** - Thread naming (adds overhead)
- **InterceptorFilter** - Various interceptors (expensive)
- **TimeMachineFilter** - Time machine functionality (expensive)
- **UrlRewriteFilter** - URL rewriting rules (not needed)
- **VanityURLFilter** - URL rewriting and vanity URLs (expensive)
- **VisitorFilter** - Visitor tracking and analytics (expensive)
- **CMSFilter** - CMS content processing (expensive)
- **AutoLoginFilter** - Authentication handling (not needed)
- **LoginRequiredFilter** - Authentication checks (not needed)
- **ManagementPortFilter** - Port restriction (bypassed via forwarding)