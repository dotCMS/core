# dotCMS Metrics and Kubernetes Tags

This document describes the dotCMS metrics system and the new Kubernetes tags configuration feature.

## Overview

The dotCMS metrics system provides comprehensive monitoring capabilities using Micrometer. The system includes:

- **MetricsService**: Main service for managing metric registries
- **MetricsTaggingService**: Centralized tag management with K8s support
- **MetricsValidator**: Configuration validation at startup
- **MetricsConfig**: Configuration management for all metrics settings
- **ManagementMetricsServlet**: Secure metrics endpoint through management port infrastructure

## Management Port Integration

The metrics system is integrated with dotCMS's management port infrastructure for security and performance:

### Endpoints

| Endpoint | Port | Purpose | Access |
|----------|------|---------|--------|
| `/dotmgt/metrics` | 8090 (management) | Prometheus metrics scraping | Protected by InfrastructureManagementFilter |

### Security

- **Primary endpoint** (`/dotmgt/metrics`) is only accessible on the management port (8090)
- Protected by `InfrastructureManagementFilter` with port validation
- Support for Docker/proxy scenarios via `X-Forwarded-Port` headers
- No authentication required to allow monitoring system scraping

### Performance

- Minimal filter chain processing for `/dotmgt/*` endpoints
- Sub-5ms response times for metrics scraping
- Direct CDI service access without expensive middleware

### Usage Examples

```bash
# Access metrics through management port
curl http://localhost:8090/dotmgt/metrics

# Docker/Kubernetes with proxy headers
curl -H "X-Forwarded-Port: 8090" http://localhost:8080/dotmgt/metrics
```

### Monitoring Integration

For Prometheus configuration, use the management port endpoint:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'dotcms'
    static_configs:
      - targets: ['dotcms:8090']
    metrics_path: '/dotmgt/metrics'
    scrape_interval: 30s
```

## Kubernetes Tags Configuration

### Environment Variables

The system supports the following environment variables for Kubernetes deployment tagging:

| Environment Variable | Property Name | Default Value | Description |
|---------------------|---------------|---------------|-------------|
| `DOT_K8S_APP` | `k8s.tags.app` | `dotcms` | Application name |
| `DOT_K8S_ENV` | `k8s.tags.env` | `local` | Environment (prod, staging, dev) |
| `DOT_K8S_VERSION` | `k8s.tags.version` | `unknown` | Application version |
| `DOT_K8S_CUSTOMER` | `k8s.tags.customer` | `default` | Customer identifier |
| `DOT_K8S_DEPLOYMENT` | `k8s.tags.deployment` | hostname | Full deployment name |

### Backward Compatibility

The system maintains backward compatibility with existing configuration:
- `DOT_ENVIRONMENT` is used as fallback for `DOT_K8S_ENV`
- All existing metrics configuration properties remain unchanged
- Default behavior works without any K8s environment variables

## Configuration Examples

### Basic Configuration (Non-K8s)

```properties
# Basic metrics configuration
metrics.enabled=true
metrics.prometheus.enabled=true
metrics.include.common-tags=true

# Basic environment tag (legacy)
environment=production
```

### Kubernetes Deployment Configuration

```yaml
# Kubernetes deployment with environment variables
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dotcms-prod
spec:
  template:
    spec:
      containers:
      - name: dotcms
        image: dotcms/dotcms:latest
        env:
        - name: DOT_K8S_APP
          value: "dotcms"
        - name: DOT_K8S_ENV
          value: "production"
        - name: DOT_K8S_VERSION
          value: "23.10.1"
        - name: DOT_K8S_CUSTOMER
          value: "customer-123"
        - name: DOT_K8S_DEPLOYMENT
          value: "dotcms-prod-deployment"
```

### Properties File Configuration

```properties
# Kubernetes tags configuration
k8s.tags.enabled=true
k8s.tags.app=dotcms
k8s.tags.env=production
k8s.tags.version=23.10.1
k8s.tags.customer=customer-123
k8s.tags.deployment=dotcms-prod-deployment

# Metrics configuration
metrics.enabled=true
metrics.prometheus.enabled=true
metrics.include.common-tags=true
```

## Generated Tags

The system automatically generates the following tags for all metrics:

### Core Tags
- `app`: Application name (from `DOT_K8S_APP`)
- `env`: Environment name (from `DOT_K8S_ENV` or `DOT_ENVIRONMENT`)
- `version`: Application version (from `DOT_K8S_VERSION`)
- `customer`: Customer identifier (from `DOT_K8S_CUSTOMER`)
- `deployment`: Deployment name (from `DOT_K8S_DEPLOYMENT`)
- `host`: Hostname (automatically detected)

### Optional Tags
- `replica`: Replica number (extracted from hostname if matches pattern `*-{number}`)

## Usage Examples

### Using MetricsTaggingService

```java
@Inject
private MetricsTaggingService taggingService;

// Get common tags for all metrics
List<Tag> commonTags = taggingService.getCommonTags();

// Create tags with additional custom tags
List<Tag> tags = taggingService.createTags(
    "endpoint", "/api/content",
    "method", "GET",
    "status", "200"
);

// Use tags with Micrometer metrics
Timer.Sample sample = Timer.start(registry);
sample.stop(Timer.builder("http.requests")
    .tags(tags)
    .register(registry));
```

### Accessing via MetricsService

```java
@Inject
private MetricsService metricsService;

// Get the tagging service
MetricsTaggingService taggingService = metricsService.getTaggingService();

// Create custom metrics with proper tags
Counter counter = Counter.builder("custom.operations")
    .tags(taggingService.createTags("operation", "import"))
    .register(metricsService.getGlobalRegistry());
```

## Prometheus Integration

### ServiceMonitor Configuration

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: dotcms-metrics
spec:
  selector:
    matchLabels:
      app: dotcms
  endpoints:
  - port: http
    path: /metrics
    interval: 30s
    scrapeTimeout: 10s
```

### Example Prometheus Queries

```promql
# Query metrics by environment
dotcms_http_requests_total{env="production"}

# Query metrics by customer
dotcms_cache_hits_total{customer="customer-123"}

# Query metrics by deployment
dotcms_jvm_memory_used_bytes{deployment="dotcms-prod-deployment"}

# Query metrics by replica
dotcms_threads_active{replica="0"}
```

## Validation and Troubleshooting

### Startup Validation

The system validates configuration at startup and logs warnings for:
- Missing required environment variables in K8s environment
- Invalid configuration values
- Performance setting issues

### Debug Logging

Enable debug logging to see detailed configuration:

```properties
# Enable debug logging for metrics
log4j.logger.com.dotcms.metrics=DEBUG
```

### Health Checks

The system includes validation methods to check configuration health:

```java
@Inject
private MetricsValidator validator;

// Check if configuration is valid for production
boolean isValid = validator.isValidForProduction();

// Check tagging service configuration
boolean tagsValid = taggingService.validateTagConfiguration();
```

## Migration Guide

### From Legacy Configuration

If you're using legacy environment variables:

1. **Keep existing configuration**: No changes required for basic functionality
2. **Add K8s variables gradually**: Start with `DOT_K8S_ENV` and `DOT_K8S_VERSION`
3. **Validate metrics**: Check Prometheus queries work with new tags
4. **Update monitoring**: Adjust dashboards and alerts for new tag names

### Docker Compose Example

```yaml
version: '3.8'
services:
  dotcms:
    image: dotcms/dotcms:latest
    environment:
      # Legacy (still supported)
      - DOT_ENVIRONMENT=production
      
      # New K8s tags
      - DOT_K8S_APP=dotcms
      - DOT_K8S_ENV=production
      - DOT_K8S_VERSION=23.10.1
      - DOT_K8S_CUSTOMER=my-company
      - DOT_K8S_DEPLOYMENT=dotcms-prod
      
      # Metrics configuration
      - DOT_METRICS_ENABLED=true
      - DOT_METRICS_PROMETHEUS_ENABLED=true
```

## Best Practices

1. **Use consistent naming**: Follow your organization's naming conventions
2. **Set version tags**: Always set `DOT_K8S_VERSION` for deployment tracking
3. **Customer identification**: Use `DOT_K8S_CUSTOMER` for multi-tenant deployments
4. **Monitor tag cardinality**: Avoid too many unique tag values
5. **Test configuration**: Use validation methods to ensure proper setup

## Performance Considerations

- Tags are cached after initialization for performance
- Maximum tag limit is configurable via `metrics.max-tags`
- Replica detection uses regex pattern matching
- Fallback values prevent metric export failures

## Security Notes

- No sensitive information should be included in metric tags
- Tags are visible in Prometheus metrics endpoint
- Use appropriate access controls for metrics endpoints
- Customer identifiers should be sanitized/anonymized if needed