# dotCMS Grafana Dashboard Collection

This directory contains **6 focused Grafana dashboards** for monitoring different aspects of your dotCMS deployment. Each dashboard concentrates on a specific category of metrics, making them easier to manage and customize than a single large dashboard.

## 📊 Available Dashboards

### 1. **Cache Performance** (`dotcms-cache-performance.json`)
**Focus**: Cache system monitoring and optimization  
**Key Metrics**: `dotcms_cache_*`

**Panels**:
- **Overview**: Overall hit rate, total objects, active providers/regions
- **Performance Trends**: Cache hit rate over time, objects by provider
- **Regional Analysis**: Hit rates by region, cache sizes, load times
- **Troubleshooting**: Slowest regions, eviction patterns

**Use Cases**:
- Identify poorly performing cache regions
- Monitor cache efficiency trends
- Optimize cache configurations
- Troubleshoot cache-related performance issues

---

### 2. **Database Monitoring** (`dotcms-database-monitoring.json`)
**Focus**: Database health and HikariCP connection pools  
**Key Metrics**: `dotcms_database_*`, `hikaricp_*`

**Panels**:
- **Health Overview**: Database availability, factory status, query tests
- **Connection Pools**: Active/idle connections, utilization, waiting threads
- **Performance**: Pool efficiency, connection trends
- **Configuration**: Pool settings and limits

**Use Cases**:
- Monitor database connectivity health
- Track connection pool utilization
- Identify connection bottlenecks
- Optimize pool configurations

---

### 3. **HTTP & Tomcat Performance** (`dotcms-http-tomcat.json`)
**Focus**: Web server performance and request processing  
**Key Metrics**: `dotcms_http_*`, `dotcms_tomcat_*`

**Panels**:
- **Request Overview**: RPS, response times, total requests, error rates
- **Request Analysis**: Processing by type, status distributions
- **Tomcat Performance**: Thread usage, connections, data transfer
- **Detailed Breakdown**: Top processors, throughput trends

**Use Cases**:
- Monitor web application performance
- Track request processing efficiency
- Identify high-traffic endpoints
- Optimize Tomcat thread pools

---

### 4. **JVM & System Performance** (`dotcms-jvm-system.json`)
**Focus**: Java Virtual Machine and system resources  
**Key Metrics**: `jvm_*`, `process_*`, `system_*`

**Panels**:
- **Memory Overview**: Heap/non-heap usage gauges, thread counts
- **Memory Trends**: Usage over time, committed vs max memory
- **System Performance**: CPU usage, load averages, file descriptors
- **Advanced**: Garbage collection, buffer pools, runtime info

**Use Cases**:
- Monitor JVM memory health
- Track system resource utilization
- Identify memory leaks or GC issues
- Optimize JVM settings

---

### 5. **Users & Sessions** (`dotcms-users-sessions.json`)
**Focus**: User activity and authentication monitoring  
**Key Metrics**: `dotcms_users_*`

**Panels**:
- **User Overview**: Active, admin, frontend, logged-in user counts
- **Activity Trends**: User activity over time, type distributions
- **Authentication**: Success rates, failure tracking, API token usage
- **Security**: Recent failures, session management

**Use Cases**:
- Monitor user engagement
- Track authentication security
- Analyze user behavior patterns
- Identify security issues

---

### 6. **System Infrastructure** (`dotcms-infrastructure.json`)
**Focus**: Infrastructure monitoring and capacity planning  
**Key Metrics**: `disk_*`, system metrics, environment info

**Panels**:
- **Storage Overview**: Disk usage gauge, free/total space
- **System Health**: CPU usage, load averages, file descriptors
- **Infrastructure Trends**: Disk space over time, resource usage
- **Environment**: Application deployment information

**Use Cases**:
- Monitor disk space and prevent outages
- Track system resource consumption
- Capacity planning and scaling decisions
- Infrastructure health monitoring

## 🚀 Quick Setup

### Import Dashboards
1. **Access Grafana**: Navigate to `http://localhost:3000`
2. **Import Process**: 
   - Go to Dashboards → Import
   - Upload each JSON file
   - Select "Prometheus" as the data source when prompted
3. **Verification**: All panels should load without errors

### Recommended Import Order
1. **Infrastructure** - Start with system-level monitoring
2. **JVM & System** - Then application runtime
3. **HTTP & Tomcat** - Web server performance
4. **Database** - Data layer monitoring  
5. **Cache** - Application-level caching
6. **Users & Sessions** - User activity tracking

## 📈 Dashboard Features

### **Visual Design**
- **Dark theme** optimized for operations centers
- **5-second refresh** for real-time monitoring
- **Color-coded thresholds**: Green (good) → Yellow (warning) → Red (critical)
- **Responsive layouts** that work on different screen sizes

### **Panel Types**
- **Stat panels**: Key metrics and KPIs with trend indicators
- **Gauges**: Percentage-based metrics with threshold visualization
- **Time series**: Historical trends and pattern analysis
- **Tables**: Detailed data with sorting and filtering
- **Pie charts**: Distribution analysis and comparisons

### **Practical Features**
- **Threshold alerts**: Visual indicators for performance issues
- **Trend analysis**: Spot patterns and predict issues
- **Drill-down capability**: From overview to detailed metrics
- **Export functionality**: Share data and create reports

## 🔧 Customization Tips

### **Adjusting Time Ranges**
- Change the default 30-minute window in each dashboard's time picker
- Modify refresh rates from 5 seconds to suit your needs
- Use relative time ranges (`now-1h`, `now-24h`) for consistency

### **Threshold Configuration**
```json
"thresholds": {
  "steps": [
    {"color": "green", "value": null},    // Good performance
    {"color": "yellow", "value": 70},     // Warning level  
    {"color": "red", "value": 90}         // Critical level
  ]
}
```

### **Query Modifications**
- Adjust Prometheus query intervals (`[5m]` → `[1m]` for higher resolution)
- Modify aggregation functions (`sum`, `avg`, `max`) based on your needs
- Add label filters to focus on specific environments or services

### **Panel Customization**
- Rename panels to match your terminology
- Adjust units (bytes, percentage, requests/sec) for clarity
- Change visualization types (line charts, bar charts, heatmaps)

## 📊 Metrics Mapping

### **Cache Metrics** (`dotcms_cache_*`)
- `dotcms_cache_hit_rate_overall` - Overall cache efficiency across all providers
- `dotcms_cache_region_size{region="X"}` - Number of objects per cache region
- `dotcms_cache_region_hits{region="X"}` - Hit count per cache region
- `dotcms_cache_region_hit_rate{region="X"}` - Hit rate percentage per region
- `dotcms_cache_region_evictions{region="X"}` - Cache pressure indicators (Caffeine/Guava)
- `dotcms_cache_region_avg_load_time_ms{region="X"}` - Cache miss penalties
- `dotcms_cache_region_memory_bytes{region="X"}` - Memory usage per region (H22)
- `dotcms_cache_region_configured_size{region="X"}` - Max configured size per region

### **Database Metrics** (`dotcms_database_*`, `hikaricp_*`)
- `dotcms_database_health` - Database connectivity status
- `hikaricp_connections_active` - Connection pool utilization
- `hikaricp_connections_pending` - Connection wait times

### **HTTP Metrics** (`dotcms_http_*`, `dotcms_tomcat_*`)
- `dotcms_http_requests_total` - Request volume and rates
- `dotcms_http_request_duration_seconds` - Response time tracking
- `dotcms_tomcat_threads_busy` - Thread pool utilization

### **JVM Metrics** (`jvm_*`, `process_*`, `system_*`)
- `jvm_memory_used_bytes` - Memory consumption tracking
- `jvm_threads_live` - Thread activity monitoring
- `system_cpu_usage` - System-level resource usage

### **User Metrics** (`dotcms_users_*`)
- `dotcms_users_active_total` - User engagement levels
- `dotcms_users_authentication_*` - Security and access patterns

## 🎯 Monitoring Best Practices

### **Performance Thresholds**
- **Cache Hit Rate**: > 80% (good), 50-80% (warning), < 50% (critical)
- **Memory Usage**: < 70% (good), 70-90% (warning), > 90% (critical)
- **Response Times**: < 500ms (good), 500-2000ms (warning), > 2000ms (critical)
- **Error Rates**: < 1% (good), 1-5% (warning), > 5% (critical)

### **Alerting Strategy**
1. **Critical Alerts**: System down, memory exhaustion, disk full
2. **Warning Alerts**: High response times, increasing error rates
3. **Info Alerts**: Unusual traffic patterns, scheduled maintenance windows

### **Dashboard Maintenance**
- **Regular Review**: Update thresholds based on historical performance
- **Seasonal Adjustments**: Account for traffic patterns and business cycles
- **Metric Evolution**: Add new metrics as dotCMS features expand
- **Performance Tuning**: Optimize query intervals and refresh rates

## 📚 Related Documentation

- **dotCMS Metrics**: See `docker-compose-examples/single-node-metrics-monitoring/README.md`
- **Prometheus Configuration**: Check the main monitoring stack documentation
- **Grafana Documentation**: [Official Grafana Guides](https://grafana.com/docs/)
- **Dashboard JSON Structure**: [Grafana Dashboard API](https://grafana.com/docs/grafana/latest/developers/http_api/dashboard/)

## 🔧 Troubleshooting

### **Common Issues**
1. **No Data Showing**: Verify Prometheus data source configuration
2. **Query Errors**: Check metric names match your dotCMS version
3. **Performance Issues**: Reduce refresh rates or query intervals
4. **Import Failures**: Ensure JSON syntax is valid

### **Query Debugging**
- Use Grafana's query inspector to examine raw Prometheus queries
- Test queries directly in Prometheus web UI (`http://localhost:9090`)
- Check metric availability with `{__name__=~"dotcms_.*"}` query

### **Performance Optimization**
- Increase query intervals for high-cardinality metrics
- Use recording rules for complex calculations
- Implement dashboard variables for filtering large datasets

---

**For additional support or customization requests, refer to the main dotCMS documentation or create an issue in the project repository.** 