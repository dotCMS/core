# Performance Analysis: Log Rotation Impact

## Issue Investigation

During high-load testing (400-800 EPS), we observed:
- Connection failures (`HttpHostConnectException: Address not available`)
- Pod restart coinciding with testing period
- Performance degradation at sustained loads >500 EPS

## Root Cause Analysis

### 1. **Log Rotation Frequency Issue**
- **Current Setting**: `rotation_min: 1` (every 1 minute)
- **Impact at High Load**: 
  - 400-800 events processed per rotation cycle
  - Frequent file I/O operations during sustained load
  - Potential blocking of request processing during rotation

### 2. **Resource Configuration Issues**  
- **Current Setting**: `resources: {}` (no limits)
- **Risk**: Unlimited memory usage leading to:
  - Pod OOM kills under sustained load
  - System instability
  - Connection pool exhaustion

### 3. **Coordinated Restart Observed**
- Multiple pods restarted 20 minutes ago during testing
- Suggests deployment update or configuration change
- Timing coincides with high-load test execution

## Performance Test Results Summary

| Load Level | EPS Target | EPS Actual | Error Rate | Status |
|------------|------------|------------|------------|---------|
| 200 EPS | 200 | ~198 | 0.00% | ✅ Stable |
| 400 EPS | 400 | ~393 | 0.00% | ✅ Stable | 
| 500 EPS | 500 | ~387-495 | 0-21% | 🔄 Variable |
| 600 EPS | 600 | ~531 | 12.33% | ⚠️ Unstable |
| 800 EPS | 800 | ~592 | 14.12% | ❌ Failing |

## Recommended Optimizations

### **High Priority**

#### 1. Log Rotation Tuning
```yaml
log:
  rotation_min: 5  # Change from 1 to 5 minutes
```
**Rationale**: 
- Reduces I/O overhead from 60 operations/hour to 12 operations/hour
- Allows sustained processing without frequent interruptions
- Better batching efficiency

#### 2. Resource Limits
```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi" 
    cpu: "1000m"
```
**Rationale**:
- Prevents unlimited memory growth
- Enables proper scheduling and resource planning
- Reduces risk of OOM kills

### **Medium Priority**

#### 3. Connection Pool Optimization
- Increase HTTP connection pool size in jitsu configuration
- Add connection timeout and retry configurations

#### 4. ClickHouse Async Inserts  
- Enable async insert mode for better throughput
- Reduce blocking operations during high load

## Testing Recommendations

### Before Optimization
1. Test current sustainable load: **400 EPS**
2. Document baseline performance metrics

### After Log Rotation Optimization  
1. Retest at 500 EPS for sustained periods (10+ minutes)
2. Verify no connection failures or pod restarts
3. Test new sustainable limit (potentially 600+ EPS)

### Monitoring Points
- File I/O operations per minute
- Memory usage trends during sustained load
- Connection pool utilization
- Pod restart frequency

## Expected Improvements

With optimized log rotation:
- **Sustained Load**: 500-600 EPS (up from 400 EPS)
- **Peak Load**: 700-800 EPS with manageable error rates
- **Stability**: Reduced connection failures and pod restarts
- **Efficiency**: Lower I/O overhead and better resource utilization

## Implementation Steps

1. **Update jitsu configuration**:
   ```bash
   kubectl edit configmap jitsu-configmap -n analytics-dev
   # Change rotation_min from 1 to 5
   ```

2. **Add resource limits**:
   ```bash
   kubectl edit deployment jitsu-api -n analytics-dev
   # Add resources section with limits
   ```

3. **Restart pods** to apply changes:
   ```bash
   kubectl rollout restart deployment/jitsu-api -n analytics-dev
   ```

4. **Retest performance** with same load patterns

5. **Monitor and validate** improvements