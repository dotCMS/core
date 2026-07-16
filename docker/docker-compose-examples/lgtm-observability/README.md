# dotCMS with Grafana LGTM Observability Stack

> **⚠️ Development Example**: This is a development and testing implementation designed to help develop and validate observability support in dotCMS. It demonstrates the integration of micrometer metrics and OpenTelemetry instrumentation with enterprise observability tooling.

This setup integrates dotCMS with the [Grafana LGTM Stack](https://github.com/grafana/docker-otel-lgtm), providing a complete observability solution with **L**oki (logs), **G**rafana (visualization), **T**empo (traces), and **M**imir/Prometheus (metrics).

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  dotCMS Container                                                   │
│  ├── Micrometer Metrics → Prometheus HTTP Scraping                 │
│  ├── OpenTelemetry Java Agent                                      │
│  │   ├── Traces → OTLP → Tempo                                     │
│  │   ├── Metrics → OTLP → Prometheus/Mimir                         │
│  │   └── Logs → OTLP → Loki                                        │
│  └── Pyroscope Agent → Continuous Profiling                        │
└─────────────────────────────────────────────────────────────────────┘
│
├── PostgreSQL (Database with health monitoring)
├── OpenSearch (Search Engine) 
└── Grafana LGTM Stack
    ├── Grafana (UI & Dashboards + dotCMS-specific dashboards)
    ├── Tempo (Distributed Tracing)
    ├── Loki (Log Aggregation) 
    ├── Prometheus (Metrics Collection & Storage)
    ├── Pyroscope (Continuous Profiling)
    └── OpenTelemetry Collector
```

## Quick Start

1. **Start the observability stack:**
   ```bash
   cd docker/docker-compose-examples/lgtm-observability
   docker-compose up -d
   ```

2. **Wait for services to start:**
   ```bash
   # Check service health
   docker-compose ps
   
   # Follow dotCMS startup logs
   docker-compose logs -f dotcms
   ```

3. **Access the services:**
   - **Grafana**: http://localhost:3000 (admin/admin)
   - **dotCMS**: http://localhost:8082
   - **dotCMS Management**: http://localhost:8090/dotmgt/livez

## Benefits of LGTM Stack

### ✅ **Advantages over Custom Setup**
- **Production-Ready**: Battle-tested configurations
- **Integrated**: All components work together seamlessly  
- **Maintained**: Official Grafana project with regular updates
- **Simplified**: Single container for entire observability backend
- **Consistent**: Standardized setup across environments

### 📊 **What You Get**
- **Tempo**: Modern distributed tracing (Jaeger alternative)
- **Loki**: Efficient log aggregation
- **Prometheus/Mimir**: Scalable metrics storage
- **Grafana**: Rich dashboards and explore interface
- **OpenTelemetry Collector**: Proper telemetry pipeline

## Infrastructure Configuration

### Services Architecture

#### dotCMS Container
- **Base Image**: `dotcms/dotcms-test:1.0.0-SNAPSHOT`
- **Management Port**: 8090 (health checks, metrics)
- **Application Ports**: 8082 (HTTP), 8443 (HTTPS)
- **Agents**: OpenTelemetry, Pyroscope via init container

#### Observability Instrumentation

**1. Micrometer Metrics (Native dotCMS)**
```yaml
METRICS_ENABLED: 'true'
METRICS_PROMETHEUS_ENABLED: 'true'
METRICS_JVM_ENABLED: 'true'
METRICS_SYSTEM_ENABLED: 'true'
METRICS_DATABASE_ENABLED: 'true'
METRICS_CACHE_ENABLED: 'true'
METRICS_HTTP_ENABLED: 'true'
METRICS_TOMCAT_ENABLED: 'true'
# Endpoint: http://dotcms:8090/dotmgt/metrics
```

**2. OpenTelemetry Java Agent**
```yaml
OTEL_SERVICE_NAME: 'dotcms'
OTEL_EXPORTER_OTLP_ENDPOINT: 'http://lgtm:4318'  # HTTP protocol
OTEL_EXPORTER_OTLP_PROTOCOL: 'http/protobuf'
OTEL_TRACES_EXPORTER: 'otlp'
OTEL_METRICS_EXPORTER: 'otlp'
OTEL_LOGS_EXPORTER: 'otlp'
OTEL_RESOURCE_ATTRIBUTES: 'service.name=dotcms,service.version=latest,deployment.environment=demo'
```

**3. Pyroscope Continuous Profiling**
```yaml
PYROSCOPE_SERVER_ADDRESS: 'http://lgtm:4040'
PYROSCOPE_APPLICATION_NAME: 'dotcms'
PYROSCOPE_FORMAT: 'jfr'
PYROSCOPE_PROFILING_INTERVAL: '10ms'
PYROSCOPE_PROFILER_ALLOC: '512k'
PYROSCOPE_PROFILER_LOCK: '10ms'
```

### Custom LGTM Configuration

#### Prometheus Configuration
Custom scraping configuration extends LGTM's default setup:
- **File**: `prometheus-config.yml`
- **dotCMS Metrics Job**: Scrapes `http://dotcms:8090/dotmgt/metrics` every 30s
- **dotCMS Health Job**: Monitors `http://dotcms:8090/dotmgt/livez` for uptime
- **Custom Startup Script**: `run-prometheus-custom.sh` replaces LGTM's default to use our config

#### Grafana Dashboard Provisioning
```yaml
# provisioning/dashboards/dashboards.yaml
providers:
  - name: 'dotcms-dashboards'
    orgId: 1
    folder: 'dotCMS'
    type: file
    path: /otel-lgtm/grafana/dashboards/dotcms
```

## Grafana Dashboards

### LGTM Built-in Dashboards
The LGTM stack includes pre-configured dashboards for:
- **APM Overview**: Service maps, request rates, latency
- **Infrastructure**: JVM metrics, system resources
- **Logs**: Log volume, error rates, search
- **Traces**: Distributed request tracing
- **Pyroscope Profiling**: CPU profiling, allocation tracking

### Custom dotCMS Dashboards
Located in the **dotCMS** folder in Grafana:

#### 1. dotCMS Database Monitoring (`dotcms-database-monitoring.json`)
- **Database Health Overview**: Availability, factory status, query test status, health score
- **HikariCP Connection Pool Metrics**: Total connections, active, idle, waiting threads
- **Connection Pool Utilization**: Real-time pool usage percentage
- **Time Series Graphs**: Active/idle connections over time
- **Configuration Table**: Connection timeout, idle timeout, max lifetime settings

#### 2. dotCMS JVM & System Metrics (`dotcms-jvm-system.json`)
- JVM memory usage (heap, non-heap, garbage collection)
- CPU utilization and system load
- Thread management and JVM performance

#### 3. dotCMS Cache Performance (`dotcms-cache-performance.json`)
- Cache hit/miss ratios
- Cache eviction metrics
- Memory usage by cache type

#### 4. dotCMS HTTP & Tomcat (`dotcms-http-tomcat.json`)
- HTTP request metrics (rate, latency, status codes)
- Tomcat connector and thread pool metrics
- Request processing performance

#### 5. dotCMS Logs Dashboard (`dotcms-logs-dashboard.json`)
- Log volume analysis
- Error rate tracking
- Log level distribution

## Querying Data

### Micrometer Metrics (Prometheus)
**dotCMS Native Metrics** (scraped from `/dotmgt/metrics`):

```promql
# Database Health
dotcms_database_health_available

# HikariCP Connection Pool
dotcms_database_hikari_connections{pool="jdbc/dotCMSPool"}
dotcms_database_hikari_connections_active{pool="jdbc/dotCMSPool"}
dotcms_database_hikari_connections_idle{pool="jdbc/dotCMSPool"}
dotcms_database_hikari_threads_awaiting{pool="jdbc/dotCMSPool"}

# Pool Utilization Rate
(dotcms_database_hikari_connections_active{pool="jdbc/dotCMSPool"} / dotcms_database_hikari_connections{pool="jdbc/dotCMSPool"}) * 100

# JVM Memory
dotcms_jvm_memory_used_bytes{area="heap"}
dotcms_jvm_gc_collection_seconds_count

# HTTP Metrics
dotcms_http_server_requests_seconds_count
dotcms_http_server_requests_seconds_sum

# Cache Metrics
dotcms_cache_size{cache="IDENTIFIER_CACHE"}
dotcms_cache_hit_ratio{cache="IDENTIFIER_CACHE"}
```

### OpenTelemetry Metrics (OTLP → Prometheus)
```promql
# Request Rate
rate(http_server_request_duration_seconds_count{service_name="dotcms"}[5m])

# JVM Memory Usage  
jvm_memory_used_bytes{service_name="dotcms", area="heap"}

# Database Query Duration
rate(db_client_operation_duration_seconds_count{service_name="dotcms"}[5m])
```

### Traces (Tempo)
```
# Find slow database queries
{service.name="dotcms" && db.statement=~"SELECT.*" && duration > 100ms}

# HTTP errors
{service.name="dotcms" && http.status_code >= 400}

# Specific operation traces
{service.name="dotcms" && operation="/api/v1/content"}
```

### Logs (Loki)
```
# Application errors
{service_name="dotcms"} |= "ERROR"

# Database queries
{service_name="dotcms"} |= "SELECT" | json

# Specific log levels
{service_name="dotcms"} | json | level="WARN"
```

## Troubleshooting

### Service Health Checks
```bash
# All services status
docker-compose ps

# Individual service health
docker-compose logs lgtm
docker-compose logs dotcms
docker-compose logs db
docker-compose logs opensearch

# dotCMS specific health
curl http://localhost:8090/dotmgt/livez
curl http://localhost:8090/dotmgt/readyz
```

### Verify Metrics Collection
```bash
# Check dotCMS micrometer metrics endpoint
curl http://localhost:8090/dotmgt/metrics

# Verify Prometheus is scraping dotCMS
curl http://localhost:3000/api/v1/targets  # Should show dotcms:8090 target

# Test Prometheus query API
curl 'http://localhost:3000/api/v1/query?query=dotcms_database_health_available'
```

### Verify Telemetry Flow
```bash
# Test OTLP endpoints
curl http://localhost:4317  # gRPC (should show connection refused)
curl http://localhost:4318  # HTTP (should return 404 or method not allowed)

# Check Grafana datasources
curl -u admin:admin http://localhost:3000/api/datasources

# Verify OpenTelemetry agent startup
docker-compose logs dotcms | grep -i "opentelemetry\|otel"

# Check Pyroscope profiling
curl http://localhost:4040/api/apps  # Should show dotcms application
```

### Dashboard Issues
```bash
# Restart to reload dashboard changes
docker-compose down && docker-compose up -d

# Clean volumes if dashboards not appearing
docker-compose down -v && docker-compose up -d

# Check dashboard provisioning
docker-compose exec lgtm ls -la /otel-lgtm/grafana/dashboards/dotcms/
```

### Common Issues

**1. "No data" in Database Health panels**
- Ensure dotCMS is fully started and metrics endpoint is accessible
- Check if `METRICS_DATABASE_ENABLED: 'true'` is set
- Verify Prometheus is scraping: `curl http://localhost:3000/api/v1/targets`

**2. Missing dotCMS dashboards in Grafana**
- Check dashboard provisioning configuration in `provisioning/dashboards/dashboards.yaml`
- Verify dashboard JSON files are mounted correctly
- Clean volumes and restart: `docker-compose down -v && docker-compose up -d`

**3. OpenTelemetry traces not appearing**
- Check OTLP endpoint configuration: `OTEL_EXPORTER_OTLP_ENDPOINT: 'http://lgtm:4318'`
- Verify OpenTelemetry agent loaded: Look for agent startup logs
- Generate traffic to create traces: Access dotCMS admin interface

**4. High memory usage**
- Adjust OpenTelemetry sampling: `OTEL_TRACES_SAMPLER_ARG: '0.1'`
- Reduce metrics cardinality: `OTEL_EXPERIMENTAL_METRICS_CARDINALITY_LIMIT: '2000'`
- Limit log capture: `OTEL_LOGS_INCLUDE_LEVEL: 'WARN'`

## Development vs Production Considerations

### Resource Requirements (Development)
- **dotCMS**: 1GB+ RAM, 2+ CPU cores
- **LGTM Stack**: 2GB+ RAM, 2+ CPU cores  
- **PostgreSQL**: 512MB+ RAM
- **OpenSearch**: 1GB+ RAM
- **Total**: ~5GB RAM, 6+ CPU cores

### Key Configuration Files
```
docker/docker-compose-examples/lgtm-observability/
├── docker-compose.yml              # Main services configuration
├── prometheus-config.yml           # Custom Prometheus config with dotCMS scraping
├── run-prometheus-custom.sh        # Custom startup script for Prometheus
└── provisioning/
    └── dashboards/
        ├── dashboards.yaml         # Dashboard provisioning config
        ├── dotcms-database-monitoring.json
        ├── dotcms-jvm-system.json
        ├── dotcms-cache-performance.json
        ├── dotcms-http-tomcat.json
        └── dotcms-logs-dashboard.json
```

### Security (Production Considerations)
```yaml
# Add authentication (for production)
lgtm:
  environment:
    GF_SECURITY_ADMIN_PASSWORD_FILE: /run/secrets/grafana_password
    GF_SECURITY_SECRET_KEY_FILE: /run/secrets/grafana_secret_key
  secrets:
    - grafana_password
    - grafana_secret_key
```

### Data Retention Configuration
```yaml
# LGTM default retention (configurable)
lgtm:
  environment:
    # Tempo (traces)
    TEMPO_RETENTION: "24h"
    # Loki (logs) 
    LOKI_RETENTION: "168h"    # 7 days
    # Prometheus (metrics)
    PROMETHEUS_RETENTION: "360h"  # 15 days
```

### Scaling for Production

#### Resource Scaling
- **dotCMS**: 4GB+ RAM, 4+ CPU cores
- **LGTM Stack**: 8GB+ RAM, 4+ CPU cores (or separate services)
- **Database**: Dedicated PostgreSQL cluster
- **Search**: Dedicated Elasticsearch/OpenSearch cluster

#### High Availability Setup
```yaml
# Example production considerations
services:
  dotcms:
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 4G
          cpus: '2.0'
    healthcheck:
      test: ["CMD", "curl", "-f", "http://127.0.0.1:8090/dotmgt/livez"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 120s
```

## Development Workflow

### Testing Metrics Changes
1. **Modify micrometer configuration** in dotCMS
2. **Update docker-compose.yml** with new environment variables
3. **Restart dotCMS container**: `docker-compose restart dotcms`
4. **Verify new metrics**: `curl http://localhost:8090/dotmgt/metrics | grep new_metric`
5. **Update dashboards** with new metric queries
6. **Test dashboard queries** in Grafana Explore

### Adding New Dashboards
1. **Create JSON dashboard** in Grafana UI
2. **Export dashboard JSON** (remove `id` field)
3. **Place in** `provisioning/dashboards/`
4. **Restart LGTM container**: `docker-compose restart lgtm`
5. **Verify loading** in Grafana dashboards folder

### Validation Checklist
- [ ] All services start successfully: `docker-compose ps`
- [ ] dotCMS health check passes: `curl http://localhost:8090/dotmgt/livez`
- [ ] Prometheus scrapes dotCMS: Check targets in Prometheus UI
- [ ] Micrometer metrics available: `curl http://localhost:8090/dotmgt/metrics`
- [ ] OpenTelemetry traces flowing: Check Tempo in Grafana
- [ ] Custom dashboards load: Check dotCMS folder in Grafana
- [ ] Database metrics display: Verify HikariCP panels show data

## Comparison: LGTM vs Custom Stack

| Aspect | Custom Stack | LGTM Stack |
|---------|-------------|------------|
| **Setup Complexity** | High (10+ services) | Low (2 core services) |
| **Maintenance** | Manual updates | Grafana managed |
| **Configuration** | Custom configs for each service | Production defaults |
| **Integration** | Manual service wiring | Pre-integrated components |
| **Documentation** | Self-documented | Official Grafana docs |
| **Updates** | Component by component | Single image update |
| **Development Speed** | Slower (complex setup) | Faster (ready-to-use) |
| **Customization** | Full control | Limited to config overrides |

## Next Steps for Development

1. **Validate Setup**: Ensure all metrics are flowing correctly
2. **Extend Metrics**: Add business-specific micrometer metrics to dotCMS
3. **Custom Dashboards**: Create dashboards for specific use cases
4. **Alert Rules**: Define Prometheus alerting rules for key metrics
5. **Performance Testing**: Load test to validate observability overhead
6. **Documentation**: Document custom metrics and dashboard usage
7. **Production Planning**: Design production-ready observability architecture

## Integration with dotCMS Development

This observability stack serves as:
- **Development Tool**: Validate new metrics implementations
- **Testing Environment**: Ensure observability doesn't impact performance  
- **Documentation**: Demonstrate enterprise observability capabilities
- **Reference Architecture**: Guide production observability deployments

The setup bridges the gap between development and production observability requirements, providing a realistic testing environment for dotCMS observability features.