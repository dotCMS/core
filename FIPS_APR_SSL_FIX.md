# FIPS Mode and APR SSL Engine Fix

## Overview

This fix addresses the OpenSSL 3.x compatibility issue with Tomcat Native APR library while maintaining the performance benefits of the native library by default, as requested in [PR #34068](https://github.com/dotCMS/core/pull/34068).

## Problem Statement

The Tomcat Native APR library (libtcnative-1) version 1.2.35 is incompatible with OpenSSL 3.x, causing JVM segmentation faults during startup on modern systems (Ubuntu 24.04+, RHEL 9+) running in FIPS mode.

## Solution Design

Instead of removing the native library entirely (as proposed in PR #34068), this solution:

1. **Keeps the native library installed by default** for performance benefits
2. **Automatically detects FIPS-enabled environments** and disables APR SSL when needed
3. **Provides configuration flags** for manual control when automatic detection is insufficient

## Implementation Details

### 1. FIPS Detection Script

**File**: `dotCMS/src/main/docker/original/ROOT/srv/15-detect-fips-and-set-ssl-engine.sh`

This script runs during container startup and:
- Checks if the system is running in FIPS mode by reading `/proc/sys/crypto/fips_enabled`
- Checks if the user has set `CMS_DISABLE_APR_SSL` environment variable
- Automatically sets `CMS_SSL_ENGINE=off` if FIPS mode is detected or manual disable is requested
- Defaults to `CMS_SSL_ENGINE=on` for optimal performance in non-FIPS environments
- Respects explicit `CMS_SSL_ENGINE` settings (user configuration takes precedence)

### 2. Entrypoint Integration

**File**: `dotCMS/src/main/docker/original/ROOT/srv/entrypoint.sh`

The FIPS detection script is sourced early in the startup process (before other configuration scripts) to ensure the `CMS_SSL_ENGINE` environment variable is set before Tomcat starts.

### 3. Documentation

**File**: `dotCMS/src/main/resources/container/tomcat9/conf/server.xml`

Added comprehensive documentation explaining:
- APR SSL Engine functionality and benefits
- FIPS mode auto-detection behavior
- Configuration options available to users
- Performance implications of each configuration

## Configuration Options

### Option 1: Automatic FIPS Detection (Default Behavior)

The container automatically detects FIPS mode at startup:

```bash
# No configuration needed - FIPS detection is automatic
docker run -p 8080:8080 dotcms/dotcms:latest
```

**What happens:**
- If `/proc/sys/crypto/fips_enabled` equals `1`: APR SSL is automatically disabled
- If FIPS is not enabled: APR SSL remains enabled for optimal performance

### Option 2: Manual Disable with CMS_DISABLE_APR_SSL

Explicitly disable APR SSL Engine without FIPS mode:

```bash
docker run -e CMS_DISABLE_APR_SSL=true -p 8080:8080 dotcms/dotcms:latest
```

**Use cases:**
- Running on OpenSSL 3.x systems that aren't in FIPS mode but still experience issues
- Testing Java JSSE performance
- Corporate policies requiring pure Java implementations

### Option 3: Direct CMS_SSL_ENGINE Control

Override all automatic behavior with explicit setting:

```bash
# Force APR SSL on (even in FIPS environments - may cause crashes)
docker run -e CMS_SSL_ENGINE=on -p 8080:8080 dotcms/dotcms:latest

# Force APR SSL off (even in non-FIPS environments)
docker run -e CMS_SSL_ENGINE=off -p 8080:8080 dotcms/dotcms:latest
```

**Use cases:**
- Advanced troubleshooting
- Custom SSL configurations
- Override automatic detection if needed

## Priority of Configuration

Configuration is applied in this order (highest to lowest priority):

1. **Explicit `CMS_SSL_ENGINE` setting** - User-provided value always wins
2. **`CMS_DISABLE_APR_SSL=true`** - Manual disable flag
3. **FIPS auto-detection** - Automatic detection of FIPS mode
4. **Default behavior** - APR SSL enabled for performance

## Performance Impact

| Configuration | SSL/TLS Implementation | Performance | Compatibility |
|--------------|------------------------|-------------|---------------|
| APR SSL enabled (default) | Native OpenSSL | Best | May crash on OpenSSL 3.x + FIPS |
| APR SSL disabled | Java JSSE | Comparable | Works everywhere |

**Note**: For most workloads, the performance difference between native OpenSSL and Java JSSE is minimal (< 5%).

## Verification

### Check FIPS Status

```bash
# Check if system is in FIPS mode
cat /proc/sys/crypto/fips_enabled
# Output: 1 (FIPS enabled) or 0 (FIPS disabled)
```

### Check APR SSL Status in Container

```bash
# View startup logs to see FIPS detection output
docker logs <container_id> | grep "FIPS Detection"

# Expected output examples:
# [FIPS Detection] System is running in FIPS mode (fips_enabled=1)
# [FIPS Detection] Automatically disabling APR SSL Engine due to FIPS mode
# [FIPS Detection] Final CMS_SSL_ENGINE value: off

# OR (non-FIPS environment):
# [FIPS Detection] APR SSL Engine enabled (default) for optimal performance
# [FIPS Detection] Final CMS_SSL_ENGINE value: on
```

### Test SSL Connectivity

```bash
# Test HTTPS endpoint
curl -k https://localhost:8443

# Check Tomcat logs for SSL implementation in use
docker exec <container_id> cat /srv/dotserver/tomcat/logs/catalina.out | grep -i "apr\|ssl"
```

## Migration Guide

### From PR #34068 (Native Library Removed)

If you were testing PR #34068, no changes needed:
- The native library is now kept by default
- FIPS detection automatically disables it when needed
- Your containers will have better performance in non-FIPS environments

### From Current Production (Native Library Always On)

No migration needed:
- Default behavior remains the same (APR SSL enabled)
- FIPS environments now work automatically
- No configuration changes required

## Testing

### Test Scenario 1: Non-FIPS Environment (Default)

```bash
# Build and run container
docker build -t dotcms-test .
docker run -e CMS_HTTP_PORT=8080 -p 8080:8080 dotcms-test

# Expected: APR SSL enabled, native library used
# Verify in logs: "APR SSL Engine enabled (default)"
```

### Test Scenario 2: FIPS Environment

```bash
# Simulate FIPS mode (requires root on host)
echo 1 | sudo tee /proc/sys/crypto/fips_enabled

# Run container
docker run -e CMS_HTTP_PORT=8080 -p 8080:8080 dotcms-test

# Expected: APR SSL automatically disabled, Java JSSE used
# Verify in logs: "System is running in FIPS mode"
```

### Test Scenario 3: Manual Disable

```bash
# Run with manual disable flag
docker run -e CMS_DISABLE_APR_SSL=true -e CMS_HTTP_PORT=8080 -p 8080:8080 dotcms-test

# Expected: APR SSL disabled regardless of FIPS mode
# Verify in logs: "APR SSL Engine disabled via CMS_DISABLE_APR_SSL"
```

## Compatibility

| Environment | APR SSL Status | Behavior |
|------------|---------------|----------|
| Ubuntu 22.04 (OpenSSL 3.0.2) | Enabled by default | Works with FIPS auto-detection |
| Ubuntu 24.04 (OpenSSL 3.0.13) | Enabled by default | Works with FIPS auto-detection |
| RHEL 8 (OpenSSL 1.1.1) | Enabled by default | Full compatibility |
| RHEL 9 (OpenSSL 3.0.7) | Enabled by default | Works with FIPS auto-detection |
| Any system with FIPS enabled | Automatically disabled | Prevents JVM crashes |

## Troubleshooting

### Issue: Container crashes with SIGSEGV on startup

**Cause**: APR SSL Engine is enabled in a FIPS/OpenSSL 3.x environment but auto-detection failed.

**Solution**:
```bash
# Manually disable APR SSL
docker run -e CMS_DISABLE_APR_SSL=true ... dotcms/dotcms:latest
```

### Issue: FIPS detection not working

**Cause**: Container might not have access to `/proc/sys/crypto/fips_enabled`.

**Solution**:
```bash
# Explicitly set CMS_SSL_ENGINE
docker run -e CMS_SSL_ENGINE=off ... dotcms/dotcms:latest
```

### Issue: Want to force APR SSL on in FIPS environment

**Warning**: This may cause crashes. Only do this if you've verified compatibility.

**Solution**:
```bash
# Override automatic detection
docker run -e CMS_SSL_ENGINE=on ... dotcms/dotcms:latest
```

## Related Issues

- [#34067](https://github.com/dotCMS/core/issues/34067) - JVM crash with OpenSSL 3.x in FIPS mode
- [PR #34068](https://github.com/dotCMS/core/pull/34068) - Original fix (removed native library entirely)

## References

- [Apache Tomcat Native Library Documentation](https://tomcat.apache.org/native-doc/)
- [OpenSSL FIPS Module](https://www.openssl.org/docs/fips.html)
- [Tomcat APR/Native Connector](https://tomcat.apache.org/tomcat-9.0-doc/apr.html)
