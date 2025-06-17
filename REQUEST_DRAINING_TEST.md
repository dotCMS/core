# Request Draining Test Guide

This guide explains how to test the request draining functionality during dotCMS shutdown.

## Overview

Request draining ensures that active HTTP requests are allowed to complete before the system shuts down completely. This is critical for:

- **Kubernetes deployments** - Graceful pod termination
- **Load balancer scenarios** - Clean traffic routing
- **User experience** - No interrupted requests during deployments

## Test Setup

I've created several tools to help test request draining:

### 1. REST Endpoints (Production Testing)

**Base URL:** `/api/v1/system/request-draining-test`

#### Long-Running Request
```bash
# Start a 10-second request
curl "http://localhost:8080/api/v1/system/request-draining-test/long-request?duration=10000"

# Response example:
{
  "requestedDuration": 10000,
  "actualDuration": 10023,
  "shutdownDetected": true,
  "shutdownDetectedAfter": 3456,
  "activeRequestsAtStart": 3,
  "shutdownStatus": {...},
  "message": "Request completed after shutdown signal detected"
}
```

#### Shutdown Status
```bash
# Check current shutdown status
curl "http://localhost:8080/api/v1/system/request-draining-test/status"

# Response example:
{
  "shutdownInProgress": true,
  "requestDrainingInProgress": true,
  "shutdownCompleted": false,
  "activeRequestCount": 2,
  "timestamp": 1703123456789
}
```

#### Simulate Active Requests
```bash
# Simulate 5 active requests (for testing without HTTP)
curl "http://localhost:8080/api/v1/system/request-draining-test/simulate-active-requests?count=5"
```

### 2. Test Script (Automated Testing)

Use the provided script for comprehensive testing:

```bash
# Basic test with 3 requests of 10 seconds each
./scripts/test-request-draining.sh test

# Test timeout scenario (requests longer than drain timeout)
./scripts/test-request-draining.sh timeout

# Check current status
./scripts/test-request-draining.sh status

# Simulate requests without HTTP
./scripts/test-request-draining.sh simulate 5

# Custom configuration
DURATION=15000 NUM_REQUESTS=5 ./scripts/test-request-draining.sh test
```

### 3. Unit Test (Development Testing)

Run the unit test for programmatic testing:

```bash
# Run the test class
./mvnw test -Dtest=RequestDrainingTest -pl :dotcms-core

# Or run individual test methods
./mvnw test -Dtest=RequestDrainingTest#testRequestDrainingManual -pl :dotcms-core
./mvnw test -Dtest=RequestDrainingTest#testRequestDrainingTimeout -pl :dotcms-core
```

## Test Scenarios

### Scenario 1: Normal Request Draining

**Setup:**
1. Start dotCMS
2. Start 3 long-running requests (10 seconds each)
3. Send SIGTERM while requests are active
4. Observe the draining behavior

**Expected Behavior:**
```
INFO  shutdown.ShutdownCoordinator - Initial active request count: 3
INFO  shutdown.ShutdownCoordinator - Waiting for requests to complete: 3 active requests, 0 busy threads (elapsed: 250ms)
INFO  shutdown.ShutdownCoordinator - Waiting for requests to complete: 2 active requests, 0 busy threads (elapsed: 3456ms)
INFO  shutdown.ShutdownCoordinator - Waiting for requests to complete: 1 active requests, 0 busy threads (elapsed: 6789ms)
INFO  shutdown.ShutdownCoordinator - Request draining completed - no active requests (9876ms)
```

### Scenario 2: Request Draining Timeout

**Setup:**
1. Start dotCMS
2. Start 2 long-running requests (20 seconds each - longer than 15s timeout)
3. Send SIGTERM while requests are active
4. Observe timeout behavior

**Expected Behavior:**
```
INFO  shutdown.ShutdownCoordinator - Initial active request count: 2
INFO  shutdown.ShutdownCoordinator - Waiting for requests to complete: 2 active requests, 0 busy threads (elapsed: 250ms)
...
WARN  shutdown.ShutdownCoordinator - Request draining timeout reached after 15000ms - proceeding with 2 active requests, 0 busy threads
```

### Scenario 3: No Active Requests

**Setup:**
1. Start dotCMS
2. Send SIGTERM with no active requests
3. Observe fast shutdown

**Expected Behavior:**
```
INFO  shutdown.ShutdownCoordinator - Initial active request count: 0
INFO  shutdown.ShutdownCoordinator - No active requests or busy threads detected - skipping request draining
```

## Health Check Integration

During request draining, the health checks will show:

**`/readyz` (Readiness Probe):**
- Returns HTTP 503 (Service Unavailable)
- Stops new traffic from being routed to the instance

**`/livez` (Liveness Probe):**
- Continues to return HTTP 200 (OK)
- Allows existing requests to complete

**`/health` (Full Health Check):**
```json
{
  "name": "shutdown",
  "status": "DOWN",
  "message": "System shutdown in progress - draining 3 active requests",
  "data": {
    "shutdownInProgress": true,
    "requestDrainingInProgress": true,
    "shutdownCompleted": false,
    "activeRequestCount": 3,
    "readinessImpact": "FAIL",
    "livenessImpact": "PASS"
  }
}
```

## Configuration

You can configure request draining behavior:

```properties
# Request draining timeout (default: 15 seconds)
shutdown.request.drain.timeout.seconds=20

# Check interval during draining (default: 250ms)
shutdown.request.drain.check.interval.ms=500

# Overall shutdown timeout (default: 30 seconds)
shutdown.timeout.seconds=45

# Component shutdown timeout (default: 10 seconds)
shutdown.component.timeout.seconds=15
```

## Docker Testing

For Docker environments:

```bash
# Start dotCMS in Docker
docker run -d --name dotcms-test -p 8080:8080 dotcms/dotcms:latest

# Start test requests
./scripts/test-request-draining.sh test

# Trigger graceful shutdown (sends SIGTERM)
docker stop dotcms-test

# Check logs
docker logs dotcms-test
```

## Kubernetes Testing

For Kubernetes environments:

```bash
# Start test requests
kubectl port-forward pod/dotcms-pod 8080:8080 &
./scripts/test-request-draining.sh test

# Trigger graceful shutdown
kubectl delete pod dotcms-pod

# Check logs
kubectl logs dotcms-pod
```

## Troubleshooting

### Request Tracking Filter Not Working

If requests aren't being tracked, verify the filter is configured in `web.xml`:

```xml
<filter>
    <filter-name>RequestTrackingFilter</filter-name>
    <filter-class>com.dotcms.filters.RequestTrackingFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>RequestTrackingFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

### Health Checks Not Failing

If `/readyz` doesn't fail during shutdown, check:

1. `ShutdownHealthCheck` is registered
2. Health check mode is not `DISABLED`
3. The health probe servlet is working

### Logs Not Showing Request Draining

Enable debug logging:

```properties
shutdown.debug=true
```

This will show detailed shutdown coordination logs.

## Expected Timeline

For a typical shutdown with active requests:

```
T+0ms:    SIGTERM received
T+10ms:   Shutdown coordinator starts
T+15ms:   Request draining begins
T+15ms:   /readyz starts failing (stops new traffic)
T+15ms:   /livez continues passing (allows existing requests)
T+5000ms: Active requests complete
T+5010ms: Request draining completes
T+5015ms: Component shutdown begins
T+8500ms: Component shutdown completes
T+8505ms: Shutdown coordinator completes
```

The entire process should complete within the configured timeout (default: 30 seconds). 