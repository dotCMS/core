# Circuit Breaker Recovery Troubleshooting

## Issue: UndeclaredThrowableException and No Recovery

The `UndeclaredThrowableException` indicates HikariCP MBean access issues, which was preventing recovery. I've fixed this with the following improvements:

### Fixes Applied

1. **Made Pool Health Check Non-Blocking**: Pool check failures no longer prevent database connectivity tests
2. **Auto-Disable Failing MBean**: If HikariCP MBean consistently fails, it's automatically disabled
3. **Prioritized Recovery**: Database connectivity tests now run regardless of pool check status
4. **Enhanced Logging**: Better diagnostics to track recovery attempts

### Immediate Diagnostic Steps

1. **Check Current State**:
   ```java
   DbConnectionFactory.logCircuitBreakerState("Current state check");
   ```

2. **Force Recovery Test**:
   ```java
   DbConnectionFactory.forceRecoveryTest("Manual recovery after DB restore");
   ```

3. **If Still Failing, Manual Override**:
   ```java
   DbConnectionFactory.closeDatabaseCircuitBreaker("Manual override - DB confirmed healthy");
   ```

### Testing Process

1. **Pause database** (circuit should open)
2. **Restore database** 
3. **Wait 10-20 seconds** OR **run force recovery test**
4. **Check logs for these messages**:

```
INFO  DatabaseConnectionHealthManager - Forcing immediate recovery test
INFO  DatabaseConnectionHealthManager - Circuit breaker transitioned to HALF_OPEN
INFO  DatabaseConnectionHealthManager - Database connectivity test successful
INFO  DatabaseConnectionHealthManager - Circuit breaker CLOSED - Database connection recovered
```

### Key Log Messages to Look For

**Good Recovery Signs**:
- `"Circuit breaker transitioning to HALF_OPEN for recovery attempt"`
- `"Database connectivity test successful"`
- `"Circuit breaker CLOSED - Database connection recovered"`

**Problem Indicators**:
- `"Cannot perform forced recovery test - monitored datasource is null"`
- `"Circuit breaker is OPEN but no monitored datasource available"`
- Repeated `"Failed to check connection pool health"` without recovery attempts

### Configuration for Faster Testing

Add these to your properties for faster recovery during testing:

```properties
# Faster recovery settings
DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS=5
DATABASE_HEALTH_CHECK_INTERVAL_SECONDS=5
DATABASE_CIRCUIT_BREAKER_MAX_BACKOFF_DELAY_SECONDS=15

# Optional: Lower failure threshold for testing
DATABASE_CIRCUIT_BREAKER_FAILURE_THRESHOLD=3
```

### Manual Recovery Commands

If automatic recovery still doesn't work, you can force it:

**Via Java Console/Script**:
```java
// Log current state
DbConnectionFactory.logCircuitBreakerState("Before manual intervention");

// Force recovery test
DbConnectionFactory.forceRecoveryTest("Database restored - manual test");

// Or force close if confirmed healthy
DbConnectionFactory.closeDatabaseCircuitBreaker("Database confirmed healthy - manual override");
```

**Via REST API** (if dotCMS is running):
```bash
# Force recovery test
curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/test-recovery \
  -d "reason=Database restored - manual test"

# Or force close
curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/close \
  -d "reason=Database confirmed healthy"
```

### Root Cause Analysis

The `UndeclaredThrowableException` was likely caused by:
1. HikariCP connection pool being in an inconsistent state after database pause/restore
2. MBean proxy becoming invalid when the underlying pool connections failed
3. This was blocking the recovery mechanism from running

The fixes ensure recovery can proceed even when pool monitoring fails.

### Next Steps

Try the pause/restore test again with these fixes. The circuit breaker should now:
1. **Open correctly** when database is paused
2. **Auto-recover in 10-20 seconds** when database is restored
3. **Not be blocked** by pool monitoring issues
4. **Allow manual override** if automatic recovery fails

If it still doesn't work, run the diagnostic commands above and share the log output - that will help identify any remaining issues.