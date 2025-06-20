#!/bin/bash

# Container Log Diagnostic Commands
# Run these to check if the recovery mechanism is working

CONTAINER_ID="df919f8a3ed5"

echo "=== 1. Check for Health Monitoring Initialization Logs ==="
echo "Looking for startup logs to verify health monitoring started..."
docker logs $CONTAINER_ID 2>&1 | grep -E "(Database health monitoring|DatabaseConnectionHealthManager.*initialized)"

echo
echo "=== 2. Check for Circuit Breaker State Changes ==="
echo "Looking for circuit breaker opening/closing events..."
docker logs $CONTAINER_ID 2>&1 | grep -E "(Circuit breaker.*OPEN|Circuit breaker.*CLOSED|Circuit breaker.*HALF_OPEN)"

echo
echo "=== 3. Check for Recovery Attempts ==="
echo "Looking for recovery attempt logs..."
docker logs $CONTAINER_ID 2>&1 | grep -E "(recovery attempt|transitioning to HALF_OPEN|Database connectivity test)"

echo
echo "=== 4. Check Recent Database Failures ==="
echo "Looking for recent database operation failures..."
docker logs $CONTAINER_ID 2>&1 | tail -50 | grep -E "(Database.*failed|circuit breaker.*OPEN|Database operation failed)"

echo
echo "=== 5. Check if Background Health Checks are Running ==="
echo "Looking for periodic health check logs..."
docker logs $CONTAINER_ID 2>&1 | grep -E "Performing health check"

echo
echo "=== 6. Get Last 100 Lines for Context ==="
echo "Recent logs to see current activity..."
docker logs $CONTAINER_ID 2>&1 | tail -100

echo
echo "=== 7. Manual Recovery Test Commands ==="
echo "If recovery isn't happening automatically, try these:"
echo
echo "# Force immediate recovery test:"
echo "docker exec $CONTAINER_ID curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/test-recovery -H 'Content-Type: application/x-www-form-urlencoded' -d 'reason=Manual recovery test'"
echo
echo "# Check current health status:"
echo "docker exec $CONTAINER_ID curl http://localhost:8080/api/v1/database/health/simple"
echo
echo "# Log current circuit breaker state:"
echo "docker exec $CONTAINER_ID curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/log-state -H 'Content-Type: application/x-www-form-urlencoded' -d 'context=Manual diagnostic check'"