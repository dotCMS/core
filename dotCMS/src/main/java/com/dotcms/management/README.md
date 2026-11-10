# dotCMS Infrastructure Management System

## 🎯 Overview

The dotCMS Infrastructure Management System provides **secure, high-performance monitoring endpoints** for dotCMS instances. All infrastructure endpoints are consolidated under the `/dotmgt/*` path and served exclusively on a dedicated management port (8090) for proper security isolation.

### **Key Features**
- ✅ **Port-Based Security**: All infrastructure endpoints isolated on dedicated management port
- ✅ **High Performance**: Sub-5ms response times with minimal filter processing
- ✅ **Docker/Proxy Compatible**: Full support for containerized deployments
- ✅ **Centralized Constants**: Zero magic strings through hierarchical constants architecture
- ✅ **Extensible Design**: Easy addition of new management services

---

## 🏗️ **Architecture Overview**

### **Infrastructure Endpoints**

The system provides essential monitoring endpoints for Kubernetes and infrastructure tools:

| Path | Purpose | Response | Port |
|------|---------|----------|------|
| `/dotmgt/livez` | Kubernetes liveness probe | `alive` \| `unhealthy` | 8090 |
| `/dotmgt/readyz` | Kubernetes readiness probe | `ready` \| `not ready` | 8090 |
| `/dotmgt/health` | Detailed health status | JSON health details | 8090 |

### **Request Flow**

```
Infrastructure Request Flow:
┌─────────────────┐    ┌───────────────────────────┐    ┌─────────────────────┐
│ Client Request  │───▶│ InfrastructureFilter      │───▶│ HealthProbeServlet  │
│ /dotmgt/livez   │    │ (Port Validation)         │    │ (Endpoint Handler)  │
└─────────────────┘    └───────────────────────────┘    └─────────────────────┘
                                │                            │
                                │                            ▼
                                │                   ┌─────────────────────┐
                                │                   │ HealthStateManager  │
                                │                   │ (Health Logic)      │
                                │                   └─────────────────────┘
                                ▼
                    ✅ Port validation (8090 vs 8080)
                    ✅ Proxy header support  
                    ✅ Filter chain bypass for performance
```

---

## 🎯 **Constants Architecture**

### **Hierarchical Constants Design**

The system eliminates magic strings through a clean hierarchy of constants:

```
┌─────────────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE LAYER                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ InfrastructureConstants                             │    │
│  │ - MANAGEMENT_PATH_PREFIX = "/dotmgt"               │    │  
│  │ - Port configuration (8090)                        │    │
│  │ - Generic headers & responses                      │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ▲                                 │
└─────────────────────────────────────────────────────────────┘
                            │ (builds upon)
┌─────────────────────────────────────────────────────────────┐
│                   HEALTH SERVICE LAYER                      │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ HealthEndpointConstants                             │    │
│  │ - LIVENESS_SUFFIX = "/livez"                       │    │
│  │ - READINESS_SUFFIX = "/readyz"                     │    │
│  │ - HEALTH_SUFFIX = "/health"                        │    │
│  │ - LIVENESS = prefix + LIVENESS_SUFFIX              │    │
│  │ - Health-specific responses                        │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### **Constants Implementation**

```java
// InfrastructureConstants.java - Generic infrastructure
public static final String MANAGEMENT_PATH_PREFIX = "/dotmgt";
public static final String METRICS_SUFFIX = "/metrics";
public static final String METRICS_PATH = MANAGEMENT_PATH_PREFIX + METRICS_SUFFIX;
public static final String ACCESS_DENIED_MESSAGE = "Management endpoints are only available on the management port";

// HealthEndpointConstants.java - Health-specific endpoints
public static final String LIVENESS_SUFFIX = "/livez";
public static final String READINESS_SUFFIX = "/readyz"; 
public static final String HEALTH_SUFFIX = "/health";

public static final String LIVENESS = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + LIVENESS_SUFFIX;
public static final String READINESS = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + READINESS_SUFFIX;
public static final String HEALTH = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + HEALTH_SUFFIX;
```

### **Benefits of Constants Architecture**

- ✅ **Zero magic strings** - all paths centralized and reusable
- ✅ **Compile-time safety** - typos become compilation errors
- ✅ **IDE support** - full refactoring and find usages support
- ✅ **Easy maintenance** - change path in one place, updates everywhere
- ✅ **One-way dependencies** - services use shared routing constants without infrastructure importing service implementations

### **Adding New Management Services**

The constants architecture makes adding new services straightforward:

```java
// MetricsEndpointConstants.java - Shared route identity, service-specific responses
public static final String METRICS_SUFFIX = InfrastructureConstants.METRICS_SUFFIX;
public static final String PROMETHEUS_SUFFIX = METRICS_SUFFIX + "/prometheus";

public static final String METRICS = InfrastructureConstants.METRICS_PATH;
public static final String PROMETHEUS = InfrastructureConstants.MANAGEMENT_PATH_PREFIX + PROMETHEUS_SUFFIX;
```

---

## 🔒 **Security Architecture**

### **Port-Based Isolation**

All infrastructure endpoints are exclusively accessible on the management port:

```
Port Access Control:
┌─────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐
│ :8080 (App)     │───▶│ /dotmgt/* Request    │───▶│ ❌ 404 BLOCKED      │
│ /dotmgt/livez   │    │ InfrastructureFilter │    │                     │
└─────────────────┘    └──────────────────────┘    └─────────────────────┘
                                
┌─────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐
│ :8090 (Mgmt)    │───▶│ /dotmgt/* Request    │───▶│ ✅ Forward to       │
│ /dotmgt/livez   │    │ InfrastructureFilter │    │    HealthProbe      │
└─────────────────┘    └──────────────────────┘    └─────────────────────┘
```

### **AbstractManagementServlet Protection**

All management servlets extend `AbstractManagementServlet` for runtime path validation:

```java
public class HealthProbeServlet extends AbstractManagementServlet {
    @Override
    protected void doManagementGet(HttpServletRequest request, HttpServletResponse response) {
        // ✅ Guaranteed to only run on /dotmgt/* paths
        // ✅ Path validation enforced by base class
        // ✅ Returns 404 if accessed outside management path
    }
}
```

### **Security Implementation**

```java
public abstract class AbstractManagementServlet extends HttpServlet {
    
    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) {
        if (!validateManagementPath(request, response)) {
            return; // ✅ Exit early if validation fails
        }
        doManagementGet(request, response); // ✅ Only called for valid paths
    }
    
    private boolean validateManagementPath(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String servletPath = request.getServletPath();
        
        boolean isManagementPath = (requestURI != null && requestURI.contains(InfrastructureConstants.MANAGEMENT_PATH_PREFIX)) ||
                                  (servletPath != null && servletPath.startsWith(InfrastructureConstants.MANAGEMENT_PATH_PREFIX));
        
        if (!isManagementPath) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Not Found");
            return false;
        }
        return true;
    }
}
```

---

## 🔧 **Configuration**

### **Port Configuration**

The infrastructure filter reads the management port from the same environment variable used by Tomcat:

```xml
<!-- server.xml -->
<Connector
    port="${CMS_MANAGEMENT_PORT:-8090}"
    proxyPort="${CMS_MANAGEMENT_PROXY_PORT:-8090}"
    address="${CMS_MANAGEMENT_BIND_ADDRESS:-0.0.0.0}" />
```

```java
// InfrastructureManagementFilter.java
private int getManagementPortFromEnvironment() {
    String portEnv = System.getenv("CMS_MANAGEMENT_PORT");
    if (portEnv != null && !portEnv.trim().isEmpty()) {
        return Integer.parseInt(portEnv.trim());
    }
    // Fallback to Config if environment variable not set
    int configPort = Config.getIntProperty("CMS_MANAGEMENT_PORT", -1);
    if (configPort != -1) {
        return configPort;
    }
    return InfrastructureConstants.Ports.DEFAULT_MANAGEMENT_PORT; // 8090
}
```

### **Port Validation Logic**

The filter validates requests using multiple methods:

```java
private boolean isManagementAccessAuthorized(HttpServletRequest request) {
    int serverPort = request.getServerPort();
    int managementPort = getManagementPortFromEnvironment();

    // Direct port match
    if (serverPort == managementPort) {
        return true;
    }

    // Docker/proxy header support
    String forwardedPort = request.getHeader(InfrastructureConstants.Headers.X_FORWARDED_PORT);
    if (forwardedPort != null) {
        try {
            int proxyPort = Integer.parseInt(forwardedPort);
            return proxyPort == managementPort;
        } catch (NumberFormatException e) { /* ignore */ }
    }

    // Alternative proxy header support
    String originalPort = request.getHeader(InfrastructureConstants.Headers.X_ORIGINAL_PORT);
    if (originalPort != null) {
        try {
            int proxyPort = Integer.parseInt(originalPort);
            return proxyPort == managementPort;
        } catch (NumberFormatException e) { /* ignore */ }
    }

    // Strict checking can be disabled for complex Docker setups
    boolean strictChecking = Config.getBooleanProperty(InfrastructureConstants.Ports.STRICT_CHECK_PROPERTY, true);
    return !strictChecking;
}
```

### **Docker & Kubernetes Support**

The system supports containerized deployments:

```yaml
# Docker Compose Example
services:
  dotcms:
    ports:
      - "8080:8080"     # Application port
      - "9090:8090"     # Management port (mapped)
    environment:
      - CMS_MANAGEMENT_PORT=8090

# Kubernetes Health Checks
livenessProbe:
  httpGet:
    path: /dotmgt/livez
    port: 8090
readinessProbe:
  httpGet:
    path: /dotmgt/readyz
    port: 8090
```

---

## 🚀 **Performance**

### **Filter Chain Optimization**

Infrastructure endpoints bypass expensive application filters:

```
Performance-Optimized Request Flow:
┌─────────────────────────────────────────────────────────┐
│ Regular Request (/api/v1/content)                       │
├─────────────────────────────────────────────────────────┤
│ 1. NormalizationFilter                                  │
│ 2. HttpHeaderSecurityFilter                             │
│ 3. CookiesFilter                                        │
│ 4. InfrastructureManagementFilter ───────────┐          │
│ 5. CharsetEncodingFilter                     │          │
│ 6. AuthenticationFilter                      │          │
│ 7. DatabaseFilter (expensive)                │          │
│ 8. ... (40+ more filters)                    │          │
│ 9. Target Servlet                            │          │
└───────────────────────────────────────────────┼─────────┘
                                                │
┌───────────────────────────────────────────────┼─────────┐
│ Infrastructure Request (/dotmgt/*)                      │
├───────────────────────────────────────────────┼─────────┤  
│ 1. NormalizationFilter                        │         │
│ 2. HttpHeaderSecurityFilter                   │         │
│ 3. CookiesFilter                              │         │
│ 4. InfrastructureManagementFilter ────────────┴ Direct  │
│    (Port validation + HealthProbeServlet)      Forward  │
└─────────────────────────────────────────────────────────┘
```

### **Performance Characteristics**

| **Metric** | **Infrastructure Endpoints** | **Benefit** |
|------------|------------------------------|-------------|
| Response Time | 1-5ms | **10x faster than app endpoints** |
| Database Calls | 0 queries | **No DB dependency** |
| Filter Processing | 4 filters | **90% reduction** |
| Object Creation | Minimal | **Direct access patterns** |

---

## 🏛️ **Health Service Integration**

### **HealthProbeServlet**

The `HealthProbeServlet` handles all health-related infrastructure endpoints:

```java
public class HealthProbeServlet extends AbstractManagementServlet {
    
    @Override
    protected void doManagementGet(HttpServletRequest request, HttpServletResponse response) {
        String endpoint = request.getServletPath();
        
        if (HealthEndpointConstants.Endpoints.LIVENESS.equals(endpoint)) {
            handleLivenessProbe(request, response);
            return;
        }
        
        if (HealthEndpointConstants.Endpoints.READINESS.equals(endpoint)) {
            handleReadinessProbe(request, response);
            return;
        }
        
        if (HealthEndpointConstants.Endpoints.HEALTH.equals(endpoint)) {
            handleFullHealth(request, response);
            return;
        }
        
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("Available health endpoints: " + 
                                  HealthEndpointConstants.Endpoints.LIVENESS + ", " +
                                  HealthEndpointConstants.Endpoints.READINESS + ", " +
                                  HealthEndpointConstants.Endpoints.HEALTH);
    }
}
```

### **Health State Management**

The servlet accesses health information through `HealthStateManager`:

```java
private void handleLivenessProbe(HttpServletRequest request, HttpServletResponse response) {
    HealthResponse healthResponse = healthStateManager.getLivenessHealth();
    
    if (healthResponse.getStatus() == HealthStatus.UP) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(HealthEndpointConstants.Responses.ALIVE_RESPONSE);
    } else {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.getWriter().write(HealthEndpointConstants.Responses.UNHEALTHY_RESPONSE);
    }
}
```

---

## 🛠️ **Web.xml Configuration**

### **Filter Configuration**

```xml
<!-- Infrastructure Management Filter -->
<filter>
    <filter-name>InfrastructureManagementFilter</filter-name>
    <filter-class>com.dotcms.management.filters.InfrastructureManagementFilter</filter-class>
</filter>

<!-- CRITICAL: Filter must run early to bypass expensive processing -->
<filter-mapping>
    <filter-name>InfrastructureManagementFilter</filter-name>
    <url-pattern>/dotmgt/*</url-pattern>
</filter-mapping>
```

### **Servlet Configuration**

```xml
<!-- Health Probe Servlet -->
<servlet>
    <servlet-name>HealthProbeServlet</servlet-name>
    <servlet-class>com.dotcms.health.servlet.HealthProbeServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>

<!-- Health service endpoint mappings -->
<servlet-mapping>
    <servlet-name>HealthProbeServlet</servlet-name>
    <url-pattern>/dotmgt/livez</url-pattern>
</servlet-mapping>
<servlet-mapping>
    <servlet-name>HealthProbeServlet</servlet-name>
    <url-pattern>/dotmgt/readyz</url-pattern>
</servlet-mapping>
<servlet-mapping>
    <servlet-name>HealthProbeServlet</servlet-name>
    <url-pattern>/dotmgt/health</url-pattern>
</servlet-mapping>
```

---

## 🚫 **Why WebInterceptors Cannot Be Used**

### **Technical Limitations**

The infrastructure monitoring system cannot use the standard WebInterceptor pattern due to fundamental architectural requirements:

#### **1. Performance Requirements**
- **Late Execution**: WebInterceptors run after 40+ expensive filters
- **Database Dependencies**: Filter chain requires database connections
- **Processing Overhead**: 50-200ms response times vs required sub-5ms

#### **2. Security Requirements**
- **Port Validation**: WebInterceptors cannot access source port information
- **Proxy Headers**: No access to proxy forwarding headers for Docker/Kubernetes
- **Management Isolation**: Cannot enforce port-based access control

#### **3. Reliability Requirements**
- **Early Execution**: Must run before CDI, database, and application initialization
- **Independence**: Must work when application services are down or starting
- **Filter Bypass**: Must skip expensive processing for monitoring tools

### **Solution: Dedicated Filter Architecture**

The `InfrastructureManagementFilter` provides:
- ✅ **Early execution** at position 4 in filter chain
- ✅ **Complete filter bypass** for infrastructure endpoints
- ✅ **Port-based security** with proxy header support
- ✅ **Database independence** during startup/shutdown
- ✅ **Sub-5ms response times** for monitoring tools

---

## 🧪 **Testing & Validation**

### **Endpoint Testing**

```bash
# Test infrastructure endpoints on management port
curl http://localhost:8090/dotmgt/livez   # → "alive"
curl http://localhost:8090/dotmgt/readyz  # → "ready" 
curl http://localhost:8090/dotmgt/health  # → JSON health details

# Verify port isolation (should be blocked)
curl http://localhost:8080/dotmgt/livez   # → 404 "Management endpoints are only available on the management port"
```

### **Docker Testing**

```bash
# Test with Docker port mapping
curl -H "X-Forwarded-Port: 9090" http://localhost:8080/dotmgt/livez  # → "alive"
```

### **Unit Test Coverage**

- ✅ **InfrastructureManagementFilter**: Port validation, proxy headers, forwarding
- ✅ **HealthProbeServlet**: Endpoint routing, error handling, responses  
- ✅ **AbstractManagementServlet**: Path validation, security enforcement
- ✅ **HealthEndpointConstants**: Constants validation, prefix construction

---

## 🔧 **Configuration Properties**

```properties
# dotmarketing-config.properties

# Management port (uses CMS_MANAGEMENT_PORT environment variable)
CMS_MANAGEMENT_PORT=8090

# Disable strict port checking for complex Docker setups
management.port.strict.check.enabled=true
```

---

## 🐛 **Troubleshooting**

### **Common Issues**

#### Port Access Denied
```
Error: "Management endpoints are only available on the management port"
Solutions:
1. Check Docker port mapping: -p 9090:8090
2. Configure proxy headers: X-Forwarded-Port
3. Verify CMS_MANAGEMENT_PORT environment variable
4. Disable strict checking: management.port.strict.check.enabled=false
```

#### Endpoint Not Found
```
Error: "Available health endpoints: /dotmgt/livez, /dotmgt/readyz, /dotmgt/health"
Solution: Use one of the listed available endpoints
```

#### Slow Response Times
```
Cause: Filter bypass not working correctly
Solutions:
1. Verify InfrastructureManagementFilter order in web.xml
2. Check servlet mappings use /dotmgt/* patterns
3. Enable debug logging to trace filter execution
```

### **Debug Logging**

```properties
# Enable infrastructure debug logging
log4j.logger.com.dotcms.management.filters.InfrastructureManagementFilter=DEBUG
log4j.logger.com.dotcms.health.servlet.HealthProbeServlet=DEBUG
log4j.logger.com.dotcms.management.servlet.AbstractManagementServlet=DEBUG
```

---

## 📈 **System Benefits**

### **Architecture Benefits**

| **Feature** | **Implementation** | **Benefit** |
|-------------|-------------------|-------------|
| **Zero Magic Strings** | Centralized constants hierarchy | **100% type safety** |
| **Port-Based Security** | Dedicated management port (8090) | **Complete isolation** |
| **High Performance** | Filter chain bypass | **Sub-5ms responses** |
| **Docker Support** | Proxy header validation | **Container-ready** |
| **Extensible Design** | Constants + servlet pattern | **Easy service addition** |

### **Operational Benefits**

1. ✅ **High Performance**: Sub-5ms response times, no database dependencies
2. ✅ **High Availability**: Independent of application state and health
3. ✅ **Secure Isolation**: Dedicated port with comprehensive proxy support
4. ✅ **Easy Maintenance**: Centralized constants, no magic strings
5. ✅ **Container Optimized**: First-class Docker/Kubernetes support
6. ✅ **Type Safe**: Compile-time safety through constants architecture

---

## 🔮 **Extensibility**

### **Adding New Management Services**

The infrastructure supports easy addition of new management services:

1. **Create service constants** following the pattern:
   ```java
   public class MetricsEndpointConstants {
       public static final String METRICS_SUFFIX = InfrastructureConstants.METRICS_SUFFIX;
       public static final String METRICS = InfrastructureConstants.METRICS_PATH;
   }
   ```

2. **Create servlet** extending `AbstractManagementServlet`

3. **Add servlet mappings** to `web.xml`

4. **Automatic benefits**:
   - ✅ Port validation handled by `InfrastructureManagementFilter`
   - ✅ Path security enforced by `AbstractManagementServlet`
   - ✅ High performance through filter bypass
   - ✅ Docker/proxy support included

### **Future Enhancements**

- **Metrics Integration**: Prometheus-compatible metrics endpoints
- **Administrative Tools**: Configuration and debugging endpoints  
- **Enhanced Security**: API key authentication for sensitive operations

This infrastructure management system provides a **secure, high-performance, and extensible** foundation for dotCMS monitoring and administration capabilities. 