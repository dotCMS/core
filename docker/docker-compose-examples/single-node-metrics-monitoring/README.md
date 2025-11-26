# dotCMS Single Node with Metrics Monitoring

A complete dotCMS monitoring stack including Prometheus metrics collection and Grafana dashboards, demonstrating the comprehensive metrics system implementation.

## 🎯 **Overview**

This docker-compose setup provides a complete monitoring solution for dotCMS including:

- **dotCMS**: Full dotCMS instance with metrics enabled
- **PostgreSQL**: Database with pgvector support
- **OpenSearch**: Search engine for content indexing
- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization and dashboarding

## 📊 **Metrics Coverage**

### ✅ **Working Metrics** (Real Data)
- **System Health**: dotCMS uptime status (`up` metric)
- **Cache Performance**: Overall hit rate (~36% in demo)
- **Storage**: File storage utilization percentage (~24% in demo)  
- **Database**: Connection availability and health checks
- **JVM Performance**: Heap memory usage, thread counts, GC statistics
- **Tomcat**: Thread pool usage, HTTP request processing, connectors
- **HTTP Performance**: Request rates, response times, error counts per processor
- **Kubernetes Tags**: App, environment, version, customer, deployment labels
- **Pod-Level Tags**: Pod name, namespace, pod UID, and node name for K8s deployments

### ⚠️ **Limited/Developing Metrics**
- **Content Counts**: Shows 0 (no content created in fresh installation)
- **User Sessions**: Minimal activity (anonymous sessions only)
- **Search Cluster**: May show 0 initially (requires time to initialize)
- **Cache Regions**: Individual regions may show NaN (cache needs warming)
- **File Operations**: Shows 0 (no uploads/downloads in fresh installation)

### 🎯 **Dashboard Focus**
The included Grafana dashboard shows **only working metrics with real values** rather than placeholder queries. Perfect for monitoring a running dotCMS instance with actual traffic and content.

## 🚀 **Quick Start**

### **1. Start the Stack**
```bash
# Clone the repository and navigate to this directory
cd docker/docker-compose-examples/single-node-metrics-monitoring

# Start all services
docker-compose up -d

# Check service status
docker-compose ps
```

### **2. Wait for Services to Initialize**
```bash
# Watch dotCMS logs until ready
docker-compose logs -f dotcms

# Wait for "Server startup completed" message
# This typically takes 2-3 minutes on first startup
```

### **3. Access the Services**

| Service | URL | Credentials              |
|---------|-----|--------------------------|
| **dotCMS** | http://localhost:8082 | admin / admin            |
| **dotCMS Health** | http://localhost:8090/dotmgt/health | N/A (health endpoint)    |
| **Prometheus** | http://localhost:9090 | N/A                      |
| **Grafana** | http://localhost:3000 | admin@dotcms.com / admin |

### **4. Validate the Setup**
```bash
# Run the comprehensive health check test
./scripts/test-health-endpoints.sh

# Or test individual endpoints manually
curl http://localhost:8090/dotmgt/livez    # Should return "alive"
curl http://localhost:8090/dotmgt/readyz   # Should return "ready" 
curl http://localhost:8090/dotmgt/health   # JSON health details

# Verify Kubernetes tags are working in metrics
curl -s http://localhost:8090/dotmgt/metrics | grep k8s_pod
# Should show metrics tagged with: k8s_pod="dotcms-demo-0"

curl -s http://localhost:8090/dotmgt/metrics | grep k8s_namespace  
# Should show metrics tagged with: k8s_namespace="dotcms-monitoring"
```

## 📈 **Monitoring Setup**

### **Prometheus Configuration**
- **Scrape Interval**: 30 seconds
- **Scrape Timeout**: 10 seconds
- **Retention**: 7 days
- **Targets**: dotCMS management port (8090)

### **Grafana Dashboards**
The setup includes a **streamlined dotCMS Overview Dashboard** focused on working metrics:

#### **System Health Section**
- dotCMS uptime status (real-time)
- Overall cache hit rate gauge (~36%)
- File storage utilization gauge (~24%)
- Database connection status indicator

#### **JVM Performance Section**
- JVM heap memory usage (ZGC Old/Young Generation)
- JVM thread counts (live threads, daemon threads)

#### **Tomcat & HTTP Performance Section**
- Tomcat thread pool usage by connector
- HTTP response times by processor
- Real-time request processing metrics

#### **Storage & Content Section**
- Available storage space (bytes)
- Content types count (system level)
- Active user count

### **Alerting Rules**
Pre-configured Prometheus alerts for:

#### **Critical Alerts**
- dotCMS service down
- Database connection pool exhaustion (>90%)
- Tomcat thread pool exhaustion (>95%)
- Search cluster unavailable

#### **Warning Alerts**
- Cache hit rate low (<70%)
- High HTTP error rate (>10%)
- Publishing queue backlog (>100 items)
- High storage utilization (>85%)
- High JVM memory usage (>90%)

## 🏥 **Health Check Integration**

This setup makes full use of dotCMS's advanced health check infrastructure with Kubernetes-compatible endpoints:

### **Health Check Endpoints**

| Endpoint | Purpose | Response | Use Case |
|----------|---------|----------|----------|
| `/dotmgt/livez` | **Liveness Probe** | `alive` \| `unhealthy` | Docker health checks, Kubernetes liveness |
| `/dotmgt/readyz` | **Readiness Probe** | `ready` \| `not ready` | Service discovery, load balancer routing |
| `/dotmgt/health` | **Detailed Health** | JSON status | Monitoring dashboards, troubleshooting |

### **Health Check Features**

#### **🎯 Kubernetes Native**
- **Liveness Probe**: Checks if the application is alive (restart if unhealthy)
- **Readiness Probe**: Checks if the application can serve traffic (remove from load balancer if not ready)
- **Fast Response**: Sub-5ms for liveness, sub-500ms for readiness
- **Startup Phase Detection**: Intelligent handling during application startup

#### **🛡️ Production Safety**
- **Lightweight Checks**: Liveness only checks core application, not dependencies
- **Comprehensive Readiness**: Readiness includes database, cache, search connectivity
- **Graceful Degradation**: DEGRADED status still allows traffic (prevents cascade failures)
- **Circuit Breaker Logic**: Automatic fallback for failing health checks

#### **🔧 Docker Integration**
```yaml
# Docker service health check (used in our compose)
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8090/dotmgt/livez"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 120s
```

#### **☸️ Kubernetes Integration**
```yaml
# Kubernetes deployment example
spec:
  containers:
  - name: dotcms
    image: dotcms/dotcms:latest
    ports:
    - containerPort: 8082  # Application port
    - containerPort: 8090  # Management port
    livenessProbe:
      httpGet:
        path: /dotmgt/livez
        port: 8090
      initialDelaySeconds: 120
      periodSeconds: 30
      timeoutSeconds: 5
      failureThreshold: 3
    readinessProbe:
      httpGet:
        path: /dotmgt/readyz
        port: 8090
      initialDelaySeconds: 30
      periodSeconds: 15
      timeoutSeconds: 5
      failureThreshold: 2
```

### **Health Check Testing**

```bash
# Test all health endpoints
curl http://localhost:8090/dotmgt/livez    # "alive" (liveness check)
curl http://localhost:8090/dotmgt/readyz   # "ready" (readiness check)  
curl http://localhost:8090/dotmgt/health   # JSON (detailed health)

# Test timing performance
curl -w "Time: %{time_total}s\n" http://localhost:8090/dotmgt/livez
curl -w "Time: %{time_total}s\n" http://localhost:8090/dotmgt/readyz

# Monitor startup sequence
watch -n 2 'echo "=== Liveness ===" && curl -s http://localhost:8090/dotmgt/livez && echo -e "\n=== Readiness ===" && curl -s http://localhost:8090/dotmgt/readyz'
```

### **Health Check Benefits in This Setup**

#### **✅ Smart Container Management**
- Docker automatically restarts unhealthy containers
- Services wait for dependencies to be ready before starting
- Proper service dependency chain with health condition checks

#### **✅ Monitoring Integration**  
- Prometheus scrapes metrics only when service is ready
- Grafana starts only after Prometheus is healthy
- Clear startup progression monitoring

#### **✅ Production Readiness**
- Real Kubernetes liveness/readiness probe configuration
- Sub-second health check responses
- Comprehensive health monitoring without performance impact
- Gradual startup with proper dependency management

#### **✅ Troubleshooting Support**
- Clear distinction between "alive" vs "ready"
- Detailed health status via JSON endpoint
- Startup phase tracking and logging
- Health state change notifications

## 🔧 **Configuration**

### **Metrics Configuration**
All metrics are enabled by default via environment variables:

```yaml
environment:
  # Core metrics
  DOT_METRICS_ENABLED: 'true'
  DOT_METRICS_PROMETHEUS_ENABLED: 'true'
  
  # Infrastructure metrics
  DOT_METRICS_DATABASE_ENABLED: 'true'
  DOT_METRICS_CACHE_ENABLED: 'true'
  DOT_METRICS_TOMCAT_ENABLED: 'true'
  DOT_METRICS_SEARCH_ENABLED: 'true'
  
  # Application metrics  
  DOT_METRICS_APPLICATION_ENABLED: 'true'
  DOT_METRICS_HTTP_ENABLED: 'true'
  DOT_METRICS_USER_SESSION_ENABLED: 'true'
  DOT_METRICS_FILE_ASSET_ENABLED: 'true'
  
  # Kubernetes tags for demo
  DOT_K8S_TAGS_ENABLED: 'true'
  DOT_K8S_APP: 'dotcms'
  DOT_K8S_ENV: 'demo'
  DOT_K8S_VERSION: '24.01'
  DOT_K8S_CUSTOMER: 'demo-customer'
  DOT_K8S_DEPLOYMENT: 'single-node-demo'
  
  # Kubernetes Environment Variables (simulating K8s pod environment)
  HOSTNAME: 'dotcms-demo-0'           # Pod name (k8s_pod tag)
  POD_NAMESPACE: 'dotcms-monitoring'  # Namespace (k8s_namespace tag)
  POD_UID: '550e8400-e29b-41d4-a716-446655440000'  # Pod UID (k8s_pod_uid tag)
  NODE_NAME: 'docker-desktop'         # Node name (k8s_node tag)
```

### **Prometheus Targets**
```yaml
scrape_configs:
  - job_name: 'dotcms'
    static_configs:
      - targets: ['dotcms:8090']
    metrics_path: '/dotmgt/metrics'
    scrape_interval: 30s
```

### **Custom Configuration**
To modify the setup:

1. **Edit Prometheus config**: `prometheus/prometheus.yml`
2. **Modify alerts**: `prometheus/rules/dotcms-alerts.yml`  
3. **Update Grafana dashboards**: `grafana/dashboards/dotcms-overview.json`
4. **Adjust docker-compose**: Environment variables in `docker-compose.yml`

## 📊 **Using the Dashboard**

### **Key Metrics to Monitor**

#### **Health Check**
- **dotCMS Status**: Should be green (1)
- **Search Health**: Should be green (2)
- **Database Pool**: Should be < 70% (yellow) or < 90% (red)
- **Cache Hit Rate**: Should be > 70% (green)

#### **Performance Indicators**
- **HTTP Request Rate**: Normal traffic patterns
- **Response Times**: Should be < 5000ms
- **Thread Pool Usage**: Should be < 95%
- **Memory Usage**: Should be < 90%

#### **Business Metrics**
- **Content Distribution**: Balance of published/draft content
- **User Activity**: Session counts and login patterns
- **Publishing Queue**: Should be < 100 items
- **Storage Usage**: Should be < 85%

### **Sample Queries**
Access Prometheus directly at http://localhost:9090 to run queries:

```promql
# Database connection pool utilization
(dotcms_database_hikari_connections_active / dotcms_database_hikari_connections_max) * 100

# Overall cache performance
dotcms_cache_hit_rate_overall

# HTTP error rate
dotcms_http_responses_error_rate

# Content publishing rate
rate(dotcms_content_count_published[5m])

# JVM memory pressure
(dotcms_jvm_memory_used_bytes / dotcms_jvm_memory_max_bytes) * 100
```

## 🔍 **Troubleshooting**

### **Services Not Starting**
```bash
# Check service logs
docker-compose logs [service-name]

# Common issues:
docker-compose logs db        # Database connection issues
docker-compose logs dotcms    # Application startup issues
docker-compose logs prometheus # Metrics collection issues
```

### **No Metrics in Prometheus**
```bash
# Test metrics endpoint directly
curl http://localhost:8090/dotmgt/metrics

# Test health endpoints
curl http://localhost:8090/dotmgt/livez    # Should return "alive"
curl http://localhost:8090/dotmgt/readyz   # Should return "ready"

# Check Prometheus targets
# Go to http://localhost:9090/targets
# dotCMS target should be "UP"
```

### **Dashboard Shows No Data**
1. **Check Prometheus**: Ensure targets are UP
2. **Verify Data Source**: Grafana → Configuration → Data Sources → Prometheus
3. **Check Time Range**: Ensure dashboard time range includes data
4. **Validate Queries**: Test queries in Prometheus directly

### **Performance Issues**
```bash
# Check metrics overhead
curl -w "%{time_total}" http://localhost:8090/dotmgt/metrics

# Check health endpoints (should be very fast)
curl -w "%{time_total}" http://localhost:8090/dotmgt/livez   # Should be < 100ms
curl -w "%{time_total}" http://localhost:8090/dotmgt/readyz  # Should be < 500ms

# Metrics collection should be < 5 seconds
# If slow, check dotCMS logs for database connection issues
```

### **Resource Usage**
```bash
# Monitor container resources
docker stats

# Typical usage:
# dotCMS: 1-2GB RAM
# Prometheus: 200-500MB RAM  
# Grafana: 100-200MB RAM
# Database: 500MB-1GB RAM
```

## 🎯 **Production Considerations**

### **Security**
- Change default passwords in production
- Use proper network isolation
- Secure Prometheus and Grafana access
- Use HTTPS for all web interfaces

### **Performance**
- Adjust scrape intervals based on needs
- Configure appropriate retention policies
- Monitor metrics collection overhead
- Use external storage for long-term retention

### **Scaling**
- Use external Prometheus for multiple dotCMS instances
- Implement Grafana with persistent storage
- Consider Prometheus federation for large deployments
- Use external databases for production workloads

### **Backup**
- Backup Grafana dashboards and configuration
- Export Prometheus rules and configuration
- Document custom modifications

## 📚 **Additional Resources**

- **dotCMS Metrics Documentation**: See main metrics README
- **Prometheus Query Language**: https://prometheus.io/docs/prometheus/latest/querying/
- **Grafana Dashboard Guide**: https://grafana.com/docs/grafana/latest/dashboards/
- **Docker Compose Reference**: https://docs.docker.com/compose/

## 🎖️ **What You Get**

This setup demonstrates:
- ✅ **Complete observability** into dotCMS infrastructure and applications
- ✅ **Production-ready alerting** for critical system health
- ✅ **Visual dashboards** for monitoring and troubleshooting
- ✅ **Performance optimization** insights through detailed metrics
- ✅ **Kubernetes-ready** configuration with proper tagging
- ✅ **Enterprise-grade** monitoring with minimal performance impact

Perfect for development, testing, and as a foundation for production monitoring deployments! 🚀

## 🧪 **Testing & Validation**

### **Automated Testing**
```bash
# Run comprehensive endpoint tests
./scripts/test-health-endpoints.sh

# Expected output:
# ✓ Liveness Probe (/dotmgt/livez) - "alive"
# ✓ Readiness Probe (/dotmgt/readyz) - "ready"  
# ✓ Health Details (/dotmgt/health) - JSON
# ✓ Prometheus Metrics (/dotmgt/metrics)
```

### **Manual Validation**
```bash
# Health check endpoints (should be very fast)
curl -w "Time: %{time_total}s\n" http://localhost:8090/dotmgt/livez
curl -w "Time: %{time_total}s\n" http://localhost:8090/dotmgt/readyz

# Full health status with timing
curl -w "Time: %{time_total}s\n" http://localhost:8090/dotmgt/health

# Metrics endpoint (may take a few seconds)
curl -w "Time: %{time_total}s\n" http://localhost:8090/dotmgt/metrics

# Monitor service startup sequence
watch -n 2 'echo "=== Health Status ===" && curl -s http://localhost:8090/dotmgt/readyz && echo -e "\n=== Container Status ===" && docker-compose ps'
```

### **Docker Health Validation**
```bash
# Check Docker container health status
docker-compose ps

# Expected healthy status:
# dotcms     - healthy
# prometheus - healthy  
# grafana    - healthy
# db         - healthy
# opensearch - (no health check configured)
```

This complete monitoring and health check demonstration showcases enterprise-grade observability for dotCMS! 🎯 