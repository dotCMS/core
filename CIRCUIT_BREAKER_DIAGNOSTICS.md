# Circuit Breaker Recovery Diagnostics

## Current Issue
Circuit breaker opens correctly when database is paused, but doesn't recover when database comes back online.

## Diagnostic Steps

### 1. Check Health Monitoring Status
The recovery depends on background health monitoring. Check if it's running:

```bash
# Look for health monitoring logs
grep "Database health monitoring started" logs/dotcms.log
grep "database-health-monitor" logs/dotcms.log
```

### 2. Check DataSource Initialization
Recovery requires the monitored datasource to be set:

```bash
# Look for datasource initialization
grep "HikariCP pool monitoring initialized" logs/dotcms.log
grep "Failed to initialize HikariCP pool monitoring" logs/dotcms.log
```

### 3. Check Recovery Attempt Logs
Look for these specific log messages:

```bash
# Recovery attempt logs
grep "Circuit breaker transitioning to HALF_OPEN" logs/dotcms.log
grep "waiting.*more seconds before recovery attempt" logs/dotcms.log
grep "Recovery attempt" logs/dotcms.log
grep "Database connectivity test" logs/dotcms.log
```

### 4. Check Configuration
Default configuration values:
- `DATABASE_CIRCUIT_BREAKER_FAILURE_THRESHOLD=5`
- `DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS=30`
- `DATABASE_CIRCUIT_BREAKER_MAX_BACKOFF_DELAY_SECONDS=60`
- `DATABASE_HEALTH_CHECK_INTERVAL_SECONDS=30`

## Potential Issues

### Issue 1: DataSource Not Initialized
If you see `"Cannot perform connectivity test - monitored datasource is null"`, the datasource wasn't properly initialized.

**Fix**: Ensure DbConnectionFactory.initializeDataSource() is called during startup.

### Issue 2: Health Monitoring Not Running
If background health monitoring isn't running, recovery won't happen automatically.

**Fix**: Check `DATABASE_HEALTH_CHECK_ENABLED=true` configuration.

### Issue 3: Backoff Delay Too Long
Exponential backoff may delay recovery attempts beyond your test window.

**Fix**: Reduce configuration values for testing:
```properties
DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS=10
DATABASE_CIRCUIT_BREAKER_MAX_BACKOFF_DELAY_SECONDS=20
DATABASE_HEALTH_CHECK_INTERVAL_SECONDS=10
```

### Issue 4: Connection Pool Issues
If HikariCP connection pool has stale connections, new connectivity tests may fail even when DB is back.

**Fix**: The circuit breaker should handle this, but check pool metrics in logs.

## Manual Recovery Commands

If automatic recovery isn't working, you can manually force recovery:

### Via REST API (if available):
```bash
curl -X POST http://localhost:8080/api/v1/database/circuit-breaker/close \
  -d "reason=Manual recovery after database restored"
```

### Via JMX/Management Interface:
Check if there's a management bean available for circuit breaker control.

## Testing Recovery

1. **Pause database** (circuit should open)
2. **Wait for logs** showing circuit opened
3. **Restore database** 
4. **Wait 30-60 seconds** (initial recovery timeout)
5. **Check logs** for recovery attempt messages
6. **Verify recovery** with test database operations

## Enhanced Logging

To get more detailed recovery information, add this to your log4j configuration:

```xml
<Logger name="com.dotmarketing.db.DatabaseConnectionHealthManager" level="DEBUG" additivity="false">
    <AppenderRef ref="CONSOLE"/>
</Logger>
```

This will show detailed recovery attempt information.