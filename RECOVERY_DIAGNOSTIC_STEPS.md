# Recovery Diagnostic Steps

## Missing Recovery Logs - Troubleshooting

Based on your logs, the circuit breaker is correctly **blocking** operations (which is good), but we're not seeing **recovery attempt** logs. This suggests the background health monitoring may not be running properly.

## Expected Recovery Logs

When the circuit breaker is trying to recover, you should see these logs every 10 seconds:

```
DEBUG DatabaseConnectionHealthManager - Performing health check - circuit state: OPEN
INFO  DatabaseConnectionHealthManager - Circuit breaker transitioning to HALF_OPEN for recovery attempt
DEBUG DatabaseConnectionHealthManager - Database connectivity test successful
INFO  DatabaseConnectionHealthManager - Circuit breaker CLOSED - Database connection recovered
```

## Diagnostic Steps

### 1. Check if Health Monitoring Started

Look for this log at dotCMS startup:
```
INFO DatabaseConnectionHealthManager - Database health monitoring started with 10s interval
INFO DatabaseConnectionHealthManager - Database health monitoring datasource initialized: HikariDataSource
```

If these are **missing**, the background health monitoring thread never started.

### 2. Enable DEBUG Logging

Add this to your logging configuration to see detailed recovery attempts:

**In log4j2.xml:**
```xml
<Logger name="com.dotmarketing.db.DatabaseConnectionHealthManager" level="DEBUG"/>
```

**Or via system property:**
```bash
-Dcom.dotmarketing.db.DatabaseConnectionHealthManager.level=DEBUG
```

### 3. Force Recovery Test Immediately

Don't wait for automatic recovery - test it manually:

```bash
# Check current state
curl http://localhost:8080/api/v1/database/health/simple

# Force immediate recovery test
curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/test-recovery \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "reason=Manual diagnostic test"

# Check state after test
curl http://localhost:8080/api/v1/database/health/simple
```

### 4. Alternative Java Console Commands

If REST API isn't available, use these Java commands in your dotCMS console:

```java
// Log current state
DbConnectionFactory.logCircuitBreakerState("Diagnostic check");

// Force recovery test
DbConnectionFactory.forceRecoveryTest("Manual diagnostic test");

// Check state after
DbConnectionFactory.logCircuitBreakerState("After recovery test");
```

## Most Likely Issues

### Issue 1: Health Monitoring Not Started
**Symptom**: No startup logs for health monitoring
**Cause**: Configuration property may be disabling it
**Fix**: Check if `DATABASE_HEALTH_CHECK_ENABLED=false` is set

### Issue 2: Datasource Not Initialized for Health Monitoring
**Symptom**: Health monitoring started but no datasource logs
**Cause**: Datasource initialization failed or wasn't called
**Fix**: The datasource initialization happens in `DbConnectionFactory.getDataSource()` on line 116

### Issue 3: Background Thread Died
**Symptom**: Started correctly but no ongoing health checks
**Cause**: Background executor thread crashed
**Fix**: Check for exception logs in the health monitoring thread

## Quick Test Sequence

1. **Enable DEBUG logging** for DatabaseConnectionHealthManager
2. **Restart dotCMS** and look for initialization logs
3. **Pause database** and verify circuit opens
4. **Restore database** 
5. **Immediately run**: `DbConnectionFactory.forceRecoveryTest("Manual test")`
6. **Check logs** for recovery attempt messages

## Expected Timeline

- **Circuit opens**: Immediately after 5 consecutive failures
- **First recovery attempt**: 10 seconds after circuit opens (automatic)
- **Manual recovery**: Immediate when `forceRecoveryTest()` is called
- **Circuit closes**: Immediately after successful recovery test

If you still don't see recovery logs after enabling DEBUG and forcing recovery, there's likely an issue with the datasource initialization or background thread startup.