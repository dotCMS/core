# Circuit Breaker Recovery Fixes

## Issues Fixed

### 1. Recovery Timeout Too Long
**Problem**: Default recovery timeout was 30 seconds, making testing difficult.
**Solution**: Reduced default to 10 seconds for faster recovery attempts.

### 2. Health Check Interval Too Long  
**Problem**: Health checks every 30 seconds delayed recovery detection.
**Solution**: Reduced default to 10 seconds for more responsive monitoring.

### 3. Missing Force Recovery Capability
**Problem**: No way to immediately test recovery when you know database is back.
**Solution**: Added `forceRecoveryTest()` method to bypass backoff delays.

### 4. Limited Logging
**Problem**: Insufficient logging to diagnose recovery issues.
**Solution**: Enhanced logging throughout the recovery process.

## New Configuration Options

You can now override these defaults in your properties:

```properties
# Faster recovery for testing (in seconds)
DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS=10
DATABASE_HEALTH_CHECK_INTERVAL_SECONDS=10

# Optional: Reduce max backoff for testing
DATABASE_CIRCUIT_BREAKER_MAX_BACKOFF_DELAY_SECONDS=30
```

## Testing the Recovery

### Option 1: Wait for Automatic Recovery (10 seconds)
1. Pause your database
2. Wait for circuit breaker to open
3. Restore your database  
4. Wait 10-20 seconds and check logs for recovery

### Option 2: Force Immediate Recovery Test
When you restore the database, immediately force a recovery test:

```bash
# Via REST API (when dotCMS is running)
curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/test-recovery \
  -d "reason=Database restored - testing recovery"

# Or programmatically
DbConnectionFactory.forceRecoveryTest("Database restored - manual test");
```

### Option 3: Manual Circuit Close (Immediate)
If you're certain the database is healthy:

```bash
# Via REST API  
curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/close \
  -d "reason=Database confirmed healthy"

# Or programmatically
DbConnectionFactory.closeDatabaseCircuitBreaker("Database confirmed healthy");
```

## Enhanced Logging

Look for these log messages to track recovery:

```
INFO  DatabaseConnectionHealthManager - Database health monitoring datasource initialized
INFO  DatabaseConnectionHealthManager - Database health monitoring started with 10s interval
WARN  DatabaseConnectionHealthManager - Database operation failed (X/5 failures)
ERROR DatabaseConnectionHealthManager - Circuit breaker OPENED - Database appears to be down
INFO  DatabaseConnectionHealthManager - Circuit breaker transitioning to HALF_OPEN for recovery attempt
INFO  DatabaseConnectionHealthManager - Database connectivity test successful  
INFO  DatabaseConnectionHealthManager - Circuit breaker CLOSED - Database connection recovered
```

## Testing Script

Here's a complete test sequence:

```bash
# 1. Pause database (however you do this)
echo "Pausing database..."

# 2. Wait for circuit to open (check logs)
echo "Waiting for circuit breaker to open..."
sleep 15

# 3. Restore database  
echo "Restoring database..."

# 4. Force immediate recovery test
echo "Forcing recovery test..."
curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/test-recovery \
  -d "reason=Manual recovery test"

# 5. Check if recovery succeeded
echo "Checking circuit breaker status..."
curl http://localhost:8080/api/v1/database/health/simple
```

The circuit breaker should now recover much more quickly and reliably when your database comes back online!