# Health Monitoring System

## Health Check Endpoints

These are served by `HealthProbeServlet` on the separate **management port** (default `8090`,
see `management.port` in `parent/pom.xml`), not the main application port — see
`com.dotcms.health.config.HealthEndpointConstants`.

### Kubernetes Probes (Public)
```bash
# Liveness probe - minimal text response
curl http://localhost:8090/dotmgt/livez

# Readiness probe - minimal text response  
curl http://localhost:8090/dotmgt/readyz
```

### Detailed Health API (Authenticated)
```bash
# Comprehensive health information (requires authentication)
curl -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  http://localhost:8090/dotmgt/health
```

## Health Check Implementation

### Creating Custom Health Checks
`HealthStatus` has four values — `UP`, `DOWN`, plus `DEGRADED` (functional but impaired;
doesn't fail liveness/readiness probes) and `UNKNOWN` (not yet determined). The example
below only uses `UP`/`DOWN` for simplicity.
```java
@ApplicationScoped
public class MyCustomHealthCheck implements HealthCheck {
    
    @Override
    public String getName() {
        return "my-custom-check";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            // Perform health check logic
            boolean isHealthy = performHealthCheck();
            
            if (isHealthy) {
                return HealthCheckResult.builder()
                    .name(getName())
                    .status(HealthStatus.UP)
                    .message("Service is running normally")
                    .build();
            } else {
                return HealthCheckResult.builder()
                    .name(getName())
                    .status(HealthStatus.DOWN)
                    .message("Service is experiencing issues")
                    .build();
            }
            
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.DOWN)
                .message("Health check failed: " + e.getMessage())
                .build();
        }
    }
    
    private boolean performHealthCheck() {
        // Implement specific health check logic
        return true;
    }
}
```

### Health Check Registration
A health check marked `@ApplicationScoped` (like the example above) is discovered and
registered automatically by `HealthCheckRegistry` via CDI — no manual registration
step is needed for the common case. Multiple related checks can also be grouped
behind a `HealthCheckProvider` bean, which the registry discovers the same way.

For dynamic/conditional registration, inject `HealthCheckRegistry` directly:
```java
@Inject
private HealthCheckRegistry healthCheckRegistry;

public void registerDynamicCheck(HealthCheck check) {
    healthCheckRegistry.registerHealthCheck(check);
}
```

## Configuration

### Health Check Properties
Per-check settings follow the convention `health.check.{check-name}.{property}`
(see `HealthCheckBase`'s Javadoc) — there's no `health.checks.*.enabled` flag; setting
a check's `mode` to `DISABLED` is how you turn one off.
```properties
# Global settings (see HealthCheckConfig.java)
health.include.system-details=true
health.check.interval-seconds=30

# Per-check settings (health.check.{check-name}.{property})
health.check.database.mode=PRODUCTION
health.check.database.timeout.seconds=2
health.check.elasticsearch.mode=PRODUCTION
```

### Environment Variables
```bash
# Health check configuration via environment variables
DOT_HEALTH_INCLUDE_SYSTEM_DETAILS=true
DOT_HEALTH_CHECK_DATABASE_MODE=PRODUCTION
DOT_HEALTH_CHECK_DATABASE_TIMEOUT_SECONDS=2
```

## Dynamic Log Level Management

### Changing Log Levels on Running Server
```bash
# Change log level for specific class
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  -d '{"name": "com.dotcms.health.servlet.HealthProbeServlet", "level": "DEBUG"}' \
  "http://localhost:8080/api/v1/logger"

# Change multiple loggers (comma-separated)
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  -d '{"name": "com.dotcms.health,com.dotmarketing.util", "level": "INFO"}' \
  "http://localhost:8080/api/v1/logger"

# Get current logger levels
curl -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  "http://localhost:8080/api/v1/logger/com.dotcms.health.servlet.HealthProbeServlet"

# List all current loggers
curl -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  "http://localhost:8080/api/v1/logger"
```

### Valid Log Levels
- `ALL` - Most verbose (log4j2's broadest level)
- `TRACE` - Most verbose
- `DEBUG` - Debug information
- `INFO` - General information
- `WARN` - Warning messages
- `ERROR` - Error messages
- `FATAL` - Fatal errors
- `OFF` - Disable logging

## Health Check Types

### Database Health Check
```java
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {
    
    @Override
    public String getName() {
        return "database";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("SELECT 1");
            dotConnect.loadResults();
            
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.UP)
                .message("Database connection is healthy")
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.DOWN)
                .message("Database connection failed: " + e.getMessage())
                .build();
        }
    }
}
```

### Elasticsearch Health Check
```java
@ApplicationScoped
public class ElasticsearchHealthCheck implements HealthCheck {
    
    @Override
    public String getName() {
        return "elasticsearch";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            // Real connectivity test — see the actual implementation in
            // com.dotcms.health.checks.cdi.ElasticsearchHealthCheck for the
            // full version (it extends HealthCheckBase for timing/tolerance)
            var esAPI = APILocator.getESIndexAPI();
            var clusterStats = esAPI.getClusterStats();
            
            if (clusterStats == null) {
                return HealthCheckResult.builder()
                    .name(getName())
                    .status(HealthStatus.DOWN)
                    .message("Elasticsearch cluster is RED")
                    .build();
            }
            
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.UP)
                .message("Elasticsearch is healthy")
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.DOWN)
                .message("Elasticsearch check failed: " + e.getMessage())
                .build();
        }
    }
}
```

## Monitoring Integration

`HealthService` already aggregates and queries health checks — there's no need to
build a custom collector or aggregator on top of `HealthCheckLocator` (which doesn't
exist). Inject it directly:

```java
@Inject
private HealthService healthService;

public void collectHealthMetrics() {
    List<HealthCheckResult> results = healthService.getAllHealthChecks();
    
    // Send metrics to monitoring system (e.g., Prometheus, DataDog, etc.)
    sendMetrics(results);
}

public boolean isSystemHealthy() {
    // getOverallHealth() already aggregates every registered check into a
    // single HealthResponse — no need to hand-roll the allMatch/aggregation logic
    HealthResponse overallHealth = healthService.getOverallHealth();
    return healthService.isReady();
}
```

Hook `collectHealthMetrics()` into whatever periodic-task mechanism the caller
already uses — dotCMS doesn't use Spring's `@Scheduled`.

## Location Information
- **Health servlets**: Located in `com.dotcms.health.servlet.*`
- **Health checks**: Found in `com.dotcms.health.checks.*`
- **Logger API**: Located in `com.dotmarketing.util.Logger`
- **Configuration**: Found in `dotmarketing-config.properties`
- **Health documentation**: See `dotCMS/src/main/java/com/dotcms/health/README.md`