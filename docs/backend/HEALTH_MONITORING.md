# Health Monitoring System

## Health Check Endpoints

### Kubernetes Probes (Public)
```bash
# Liveness probe - minimal text response
curl http://localhost:8080/livez

# Readiness probe - minimal text response  
curl http://localhost:8080/readyz
```

### Detailed Health API (Authenticated)
```bash
# Comprehensive health information (requires authentication)
curl -H "Authorization: Basic $(echo -n 'admin@dotcms.com:admin' | base64)" \
  http://localhost:8080/api/v1/health
```

## Health Check Implementation

### Creating Custom Health Checks
```java
@Component
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
                    .status(HealthStatus.HEALTHY)
                    .message("Service is running normally")
                    .build();
            } else {
                return HealthCheckResult.builder()
                    .name(getName())
                    .status(HealthStatus.UNHEALTHY)
                    .message("Service is experiencing issues")
                    .build();
            }
            
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.UNHEALTHY)
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
```java
@ApplicationScoped
public class HealthCheckRegistry {
    
    @PostConstruct
    public void registerHealthChecks() {
        // Register custom health checks
        HealthCheckLocator.registerHealthCheck(new MyCustomHealthCheck());
        HealthCheckLocator.registerHealthCheck(new DatabaseHealthCheck());
        HealthCheckLocator.registerHealthCheck(new ElasticsearchHealthCheck());
    }
}
```

## Configuration

### Health Check Properties
```properties
# Health check configuration
health.checks.enabled=true
health.checks.database.enabled=true
health.checks.database.timeout-seconds=30
health.checks.elasticsearch.enabled=true
health.checks.elasticsearch.timeout-seconds=10
health.monitoring.include-system-details=true
```

### Environment Variables
```bash
# Health check configuration via environment variables
DOT_HEALTH_CHECKS_ENABLED=true
DOT_HEALTH_CHECKS_DATABASE_ENABLED=true
DOT_HEALTH_CHECKS_DATABASE_TIMEOUT_SECONDS=30
DOT_HEALTH_MONITORING_INCLUDE_SYSTEM_DETAILS=true
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
@Component
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
                .status(HealthStatus.HEALTHY)
                .message("Database connection is healthy")
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.UNHEALTHY)
                .message("Database connection failed: " + e.getMessage())
                .build();
        }
    }
}
```

### Elasticsearch Health Check
```java
@Component
public class ElasticsearchHealthCheck implements HealthCheck {
    
    @Override
    public String getName() {
        return "elasticsearch";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            // Check Elasticsearch connection
            ESClient esClient = new ESClient();
            ClusterHealthResponse health = esClient.admin().cluster().health().get();
            
            if (health.getStatus() == ClusterHealthStatus.RED) {
                return HealthCheckResult.builder()
                    .name(getName())
                    .status(HealthStatus.UNHEALTHY)
                    .message("Elasticsearch cluster is RED")
                    .build();
            }
            
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.HEALTHY)
                .message("Elasticsearch is healthy")
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(getName())
                .status(HealthStatus.UNHEALTHY)
                .message("Elasticsearch check failed: " + e.getMessage())
                .build();
        }
    }
}
```

## Monitoring Integration

### Metrics Collection
```java
@Component
public class HealthMetricsCollector {
    
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void collectHealthMetrics() {
        List<HealthCheckResult> results = HealthCheckLocator.getAllHealthChecks()
            .stream()
            .map(HealthCheck::check)
            .collect(Collectors.toList());
            
        // Send metrics to monitoring system
        sendMetrics(results);
    }
    
    private void sendMetrics(List<HealthCheckResult> results) {
        // Implementation for sending metrics to monitoring system
        // (e.g., Prometheus, DataDog, etc.)
    }
}
```

### Health Status Aggregation
```java
@Component
public class HealthStatusAggregator {
    
    public OverallHealth getOverallHealth() {
        List<HealthCheckResult> results = HealthCheckLocator.getAllHealthChecks()
            .stream()
            .map(HealthCheck::check)
            .collect(Collectors.toList());
            
        boolean allHealthy = results.stream()
            .allMatch(result -> result.getStatus() == HealthStatus.HEALTHY);
            
        return OverallHealth.builder()
            .status(allHealthy ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY)
            .checks(results)
            .timestamp(new Date())
            .build();
    }
}
```

## Location Information
- **Health servlets**: Located in `com.dotcms.health.servlet.*`
- **Health checks**: Found in `com.dotcms.health.checks.*`
- **Logger API**: Located in `com.dotmarketing.util.Logger`
- **Configuration**: Found in `dotmarketing-config.properties`
- **Health documentation**: See `dotCMS/src/main/java/com/dotcms/health/README.md`