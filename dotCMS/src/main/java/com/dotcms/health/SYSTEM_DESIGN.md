# dotCMS Health Check System - System Design & Requirements

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [System Requirements](#system-requirements)
3. [Architecture Design](#architecture-design)
4. [API Specification](#api-specification)
5. [Security Model](#security-model)
6. [Configuration Management](#configuration-management)
7. [Testing Strategy](#testing-strategy)

---

## Executive Summary

The dotCMS Health Check System is a production-ready, Kubernetes-compatible health monitoring solution that provides comprehensive application health visibility while maintaining strict security boundaries and operational safety.

### Key Features
- **Kubernetes-native health probes** with minimal response format
- **Startup-aware behavior** with proper DOWN status during initialization  
- **Configurable failure tolerance** with circuit breaker-like functionality
- **Enhanced logging** distinguishing startup vs operational failures
- **Comprehensive health monitoring** across application, database, cache, and infrastructure
- **Security-first design** with authentication-protected detailed information
- **Safety-first configuration** preventing accidental deployment failures
- **RFC-compliant health responses** for standard monitoring tool integration
- **Extensible architecture** supporting custom health checks via CDI

---

## System Requirements

### Functional Requirements

#### FR1: Health Check Execution
- **FR1.1**: System SHALL execute health checks for application, database, cache, and infrastructure components
- **FR1.2**: System SHALL support custom health checks via CDI discovery
- **FR1.3**: System SHALL provide consistent health check result format with status, message, timing, and metadata
- **FR1.4**: System SHALL execute health checks in background with configurable intervals
- **FR1.5**: System SHALL support synchronous health check execution for real-time status

#### FR2: Kubernetes Integration
- **FR2.1**: System SHALL provide `/livez` endpoint returning text "alive" or "unhealthy"
- **FR2.2**: System SHALL provide `/readyz` endpoint returning text "ready" or "not ready"
- **FR2.3**: System SHALL provide `/health` endpoint with detailed JSON health information
- **FR2.4**: System SHALL distinguish between liveness checks (core application) and readiness checks (application + dependencies)
- **FR2.5**: System SHALL support startup phase detection to avoid false positive failures

#### FR3: Safety Modes
- **FR3.1**: System SHALL support PRODUCTION mode with normal UP/DOWN behavior
- **FR3.2**: System SHALL support MONITOR_MODE that converts DOWN to DEGRADED to prevent probe failures
- **FR3.3**: System SHALL support DISABLED mode to skip checks entirely
- **FR3.4**: System SHALL apply safety mode conversions consistently across all checks
- **FR3.5**: System SHALL log safety mode conversions for observability

#### FR4: Configuration Management
- **FR4.1**: System SHALL use naming convention `health.check.{check-name}.{property}` for configuration
- **FR4.2**: System SHALL support per-check configuration including mode, timeouts, and custom properties
- **FR4.3**: System SHALL support global configuration for system-wide settings
- **FR4.4**: System SHALL provide configuration validation and error reporting
- **FR4.5**: System SHALL support runtime configuration changes without restart

#### FR5: Monitoring and Observability
- **FR5.2**: System SHALL include RFC-compliant health check response format
- **FR5.3**: System SHALL support filtering health responses by liveness/readiness categorization
- **FR5.4**: System SHALL provide comprehensive logging with structured messages
- **FR5.5**: System SHALL include performance metrics and timing information

### Non-Functional Requirements

#### NFR1: Performance
- **NFR1.1**: Health check execution SHALL complete within 5 seconds by default
- **NFR1.2**: Health check endpoints SHALL respond within 2 seconds under normal load
- **NFR1.3**: Background health checks SHALL not consume more than 5% CPU
- **NFR1.4**: System SHALL support concurrent health check execution
- **NFR1.5**: Memory overhead SHALL be less than 50MB for health check system

#### NFR2: Reliability
- **NFR2.1**: System SHALL continue operating if individual health checks fail
- **NFR2.2**: System SHALL provide graceful degradation during resource constraints
- **NFR2.3**: System SHALL recover automatically from transient failures
- **NFR2.4**: System SHALL prevent cascading failures between health checks
- **NFR2.5**: System SHALL maintain 99.9% uptime for health endpoints

#### NFR3: Security
- **NFR3.1**: Detailed health information SHALL require authentication
- **NFR3.2**: System SHALL not expose sensitive system information in error messages
- **NFR3.3**: Kubernetes probe endpoints SHALL be publicly accessible for orchestration
- **NFR3.4**: System SHALL support network access controls for detailed endpoints
- **NFR3.5**: System SHALL log security-relevant health check access

#### NFR4: Scalability
- **NFR4.1**: System SHALL support up to 100 concurrent health checks
- **NFR4.2**: System SHALL support dynamic health check registration at runtime
- **NFR4.3**: System SHALL scale health check frequency based on system load
- **NFR4.4**: System SHALL support distributed health check execution
- **NFR4.5**: Memory usage SHALL scale linearly with number of health checks

#### NFR5: Maintainability
- **NFR5.1**: System SHALL provide clear API for extending with custom health checks
- **NFR5.2**: System SHALL use standard logging frameworks and patterns
- **NFR5.3**: System SHALL provide comprehensive documentation for developers
- **NFR5.4**: System SHALL follow dotCMS coding standards and conventions
- **NFR5.5**: System SHALL support hot deployment of new health checks

---

## Architecture Design

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Health Check System                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐                        ┌─────────────────┐ │
│  │   Kubernetes    │                        │   Admin REST    │ │
│  │   Endpoints     │                        │   Endpoints     │ │
│  │                 │                        │                 │ │
│  │ /livez          │                        │ /api/v1/health  │ │
│  │ /readyz         │                        │ (authenticated) │ │
│  │                 │                        │                 │ │
│  └─────────────────┘                        └─────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                Health State Manager                        │ │
│  │  • Background execution    • Startup phase detection       │ │
│  │  • Health categorization   • Tolerance management          │ │
│  │  • Result caching         • Circuit breaker logic          │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   Health Check Registry                    │ │
│  │  • CDI-based discovery    • Manual registration            │ │
│  │  • Provider management    • Lifecycle control              │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────────────┐ │
│  │ Core Health    │ │ Dependency     │ │    Custom Health       │ │
│  │ Checks         │ │ Health Checks  │ │    Checks (CDI)        │ │
│  │                │ │                │ │                        │ │
│  │ • Application  │ │ • Database     │ │ • Module-specific      │ │
│  │ • System       │ │ • Cache        │ │ • Integration tests    │ │
│  │ • Servlet      │ │ • Search       │ │ • Custom components    │ │
│  │ • Threads      │ │ • External API │ │                        │ │
│  │ • GC           │ │                │ │                        │ │
│  └────────────────┘ └────────────────┘ └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Health Check Categorization

The system uses explicit interface methods to categorize health checks:

```java
public interface HealthCheck {
    boolean isLivenessCheck();    // Safe for K8s liveness probes
    boolean isReadinessCheck();   // Required for K8s readiness probes
}
```

**Liveness Checks (Core Application Only):**
- ApplicationHealthCheck - JVM memory usage
- SystemHealthCheck - OS resources, disk space  
- ServletContainerHealthCheck - Web container health
- ThreadHealthCheck - JVM deadlock detection
- GarbageCollectionHealthCheck - GC pressure monitoring

**Readiness Checks (Application + Dependencies):**
- All liveness checks PLUS:
- CdiInitializationHealthCheck - CDI container ready
- DatabaseHealthCheck - Database connectivity
- CacheHealthCheck - Cache operations  
- ElasticsearchHealthCheck - Search functionality

### Safety Mode Implementation

```java
public enum HealthCheckMode {
    PRODUCTION,     // Normal UP/DOWN behavior, can fail probes
    MONITOR_MODE,   // Monitor health but never fail K8s probes (DOWN->DEGRADED)
    DISABLED        // Skip check entirely
}
```

The system applies safety mode conversions at the health check level, ensuring consistent behavior across all endpoints.

---

## API Specification

### Kubernetes Endpoints (Public)

#### GET /livez
**Purpose**: Liveness probe endpoint for Kubernetes
**Response**: Text response ("alive" or "unhealthy")
**Status Codes**: 200 (alive), 503 (unhealthy)
**Content-Type**: text/plain

#### GET /readyz  
**Purpose**: Readiness probe endpoint for Kubernetes
**Response**: Text response ("ready" or "not ready")
**Status Codes**: 200 (ready), 503 (not ready)
**Content-Type**: text/plain


### Monitoring Endpoints (Public)


### Admin REST Endpoints (Authenticated)

#### GET /api/v1/health
**Purpose**: Administrative health status access
**Authentication**: Required (CMS Admin role)
**Response**: Complete health status with admin metadata
**Status Codes**: 200, 401 (unauthorized), 503 (unhealthy)

#### GET /api/v1/health/check/{checkName}
**Purpose**: Individual health check status
**Authentication**: Required (CMS Admin role)  
**Parameters**: checkName - Name of specific health check
**Response**: Individual health check result
**Status Codes**: 200, 401 (unauthorized), 404 (check not found)

### Health Response Format (RFC-Compliant)

```json
{
  "status": "pass|fail|warn",
  "version": "24.01.1",
  "releaseId": "build-12345",
  "serviceId": "dotcms-health",
  "description": "dotCMS Application Health Status",
  "timestamp": "2024-01-15T10:30:00Z",
  "checks": [
    {
      "name": "application",
      "status": "pass|fail|warn", 
      "message": "Health check message",
      "time": "2024-01-15T10:30:00Z",
      "duration": "15ms",
      "mode": "PRODUCTION|MONITOR_MODE|DISABLED",
      "safetyModeApplied": false
    }
  ],
  "links": {
    "self": "/health",
    "about": "/api/v1/system/status"
  }
}
```

---

## Security Model

### Public Endpoints (No Authentication)
- `/livez`, `/readyz` - Minimal text responses for Kubernetes

### Authenticated Endpoints (Admin Role Required)
- `/api/v1/health/*` - Complete administrative access to health system

### Information Disclosure Controls
- **HEALTH_INCLUDE_SYSTEM_DETAILS**: Controls detailed system information inclusion
- **HEALTH_INCLUDE_PERFORMANCE_METRICS**: Controls performance metrics inclusion
- Error messages exclude sensitive information (connection strings, internal paths)
- Network access controls recommended for detailed endpoints in production

### Audit and Logging
- All administrative health check access logged
- Failed authentication attempts logged  
- Health check failures logged with appropriate detail level
- Security-relevant configuration changes logged

---

## Configuration Management

### Global Configuration Properties
```properties
# System-wide health check settings
health.include.system-details=true              # Include detailed system information
health.include.performance-metrics=true         # Include performance metrics
health.interval-seconds=30                      # Background check interval
health.thread-pool-size=3                       # Thread pool size for background execution
health.startup.grace.period.minutes=2           # Startup phase duration
health.stable.operation.threshold=3             # Checks for stable operation

# Failure tolerance settings  
health.tolerance.enabled=true                   # Enable failure tolerance
health.tolerance.readiness.minutes=2            # Readiness tolerance window
health.tolerance.liveness.minutes=5             # Liveness tolerance window (longer)
health.tolerance.use.different.liveness=true    # Use different liveness tolerance
```

### Per-Check Configuration Pattern
```properties
# Pattern: health.check.{check-name}.{property}
health.check.{checkName}.mode=PRODUCTION|MONITOR_MODE|DISABLED
health.check.{checkName}.timeout-ms=2000
health.check.{checkName}.{customProperty}=value
```

### Configuration Validation
- Unknown mode values default to PRODUCTION
- Invalid timeout values use system defaults
- Configuration errors logged with corrective guidance
- Runtime configuration changes validated before application

---

## Testing Strategy

### Unit Testing Coverage
- **HealthCheckBase**: Mode handling, configuration access, timing measurement
- **HealthCheckToleranceManager**: Startup vs operational behavior, failure windows
- **Individual Health Checks**: Status detection, mode conversions, error handling  
- **HealthStateManager**: Framework coordination, categorization, concurrent access
- **Servlet Endpoints**: Response formatting, status code mapping, authentication

### Integration Testing
- End-to-end health check execution
- Kubernetes probe endpoint validation
- CDI discovery and registration
- Configuration management and validation
- Security boundary enforcement

### Performance Testing
- Health check execution timing under load
- Concurrent endpoint access
- Background execution resource usage
- Memory usage with multiple health checks
- Probe endpoint response time under stress

### Security Testing
- Authentication bypass attempts
- Information disclosure verification
- Network access control validation
- Audit log completeness
- Error message information leakage

### Deployment Testing
- Safe deployment configuration validation
- Progressive enablement scenarios
- Emergency disable procedures
- Configuration rollback testing
- Production monitoring integration

This design provides a comprehensive, secure, and operationally safe health check system that meets both development and production requirements while maintaining compatibility with Kubernetes orchestration patterns.