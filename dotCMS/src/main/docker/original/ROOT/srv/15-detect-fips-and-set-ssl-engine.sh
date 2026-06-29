#!/bin/bash

# FIPS Mode Detection and APR SSL Engine Configuration
# =====================================================
# This script automatically detects FIPS-enabled environments and prevents
# libtcnative-1 from loading to avoid JVM crashes with OpenSSL 3.x in FIPS mode.
#
# Root cause: libtcnative-1 links against libcrypto.so.3. On a FIPS-enabled kernel,
# OpenSSL 3.x requires the FIPS provider (fips.so) to be present before allowing
# any crypto operation. Ubuntu 24.04 does not ship fips.so, so the first OpenSSL
# crypto call (e.g. EVP_MD_get0_provider for random number generation) segfaults.
# This happens regardless of SSLEngine or AprLifecycleListener configuration because
# setenv.sh sets java.library.path to /usr/lib/<arch>-linux-gnu/ and Tomcat auto-
# detects and loads libtcnative-1 from there even without an AprLifecycleListener.
#
# Fix: The Dockerfile moves libtcnative-1.so.0* to /srv/native-libs/ (owned by the
# dotcms user) and leaves symlinks in /usr/lib. When FIPS is detected, this script
# removes the files in /srv/native-libs/, making the symlinks dangling. dlopen() then
# fails to load the library regardless of java.library.path or server.xml config.
#
# Configuration Options:
# ----------------------
# 1. Automatic FIPS Detection (default behavior):
#    - The script checks /proc/sys/crypto/fips_enabled
#    - If FIPS is enabled, libtcnative-1 is removed and CMS_SSL_ENGINE is set to 'off'
#
# 2. Manual Override with CMS_DISABLE_APR_SSL:
#    - Set CMS_DISABLE_APR_SSL=true to disable APR SSL Engine
#    - Set CMS_DISABLE_APR_SSL=false to enable APR SSL Engine (default)
#
# 3. Direct CMS_SSL_ENGINE Control:
#    - If CMS_SSL_ENGINE is already set, it takes precedence
#    - This allows users to explicitly control the SSL engine behavior
#
# Performance Impact:
# ------------------
# - APR SSL Engine enabled: Uses native OpenSSL (better performance)
# - APR SSL Engine disabled: Uses Java JSSE (comparable performance, better compatibility)

# Check if CMS_SSL_ENGINE is already explicitly set by user
if [[ -n "${CMS_SSL_ENGINE}" ]]; then
    echo "[FIPS Detection] CMS_SSL_ENGINE already set to '${CMS_SSL_ENGINE}' - respecting user configuration"
    return 0
fi

# Initialize FIPS detection flag
FIPS_ENABLED=false

# Check if system is running in FIPS mode
if [[ -f /proc/sys/crypto/fips_enabled ]]; then
    FIPS_MODE=$(cat /proc/sys/crypto/fips_enabled 2>/dev/null || echo "0")
    if [[ "${FIPS_MODE}" == "1" ]]; then
        FIPS_ENABLED=true
        echo "[FIPS Detection] System is running in FIPS mode (fips_enabled=1)"
    fi
fi

# Check if user explicitly requested to disable APR SSL
if [[ "${CMS_DISABLE_APR_SSL}" == "true" || "${CMS_DISABLE_APR_SSL}" == "1" ]]; then
    echo "[FIPS Detection] APR SSL Engine disabled via CMS_DISABLE_APR_SSL environment variable"
    export CMS_SSL_ENGINE="off"
elif [[ "${FIPS_ENABLED}" == "true" ]]; then
    echo "[FIPS Detection] Automatically disabling APR SSL Engine due to FIPS mode"
    echo "[FIPS Detection] This prevents JVM crashes with OpenSSL 3.x in FIPS environments"
    echo "[FIPS Detection] Tomcat will use Java JSSE for SSL/TLS instead"
    # Remove libtcnative-1 from /srv/native-libs (writable by dotcms user).
    # The Dockerfile placed the library there and left symlinks in /usr/lib.
    # Removing the target makes the symlinks dangling so dlopen() cannot load
    # the library regardless of java.library.path or server.xml configuration.
    if rm -f /srv/native-libs/libtcnative-1.so.0* && \
       ! ls /srv/native-libs/libtcnative-1.so.0* >/dev/null 2>&1; then
        echo "[FIPS Detection] libtcnative-1 removed from /srv/native-libs — library cannot be loaded"
    else
        echo "[FIPS Detection] WARNING: Failed to remove libtcnative-1 from /srv/native-libs — JVM may still crash"
    fi
    export CMS_SSL_ENGINE="off"
else
    # Default: Keep APR SSL Engine enabled for performance benefits
    echo "[FIPS Detection] APR SSL Engine enabled (default) for optimal performance"
    echo "[FIPS Detection] To disable APR SSL Engine, set CMS_DISABLE_APR_SSL=true or CMS_SSL_ENGINE=off"
    export CMS_SSL_ENGINE="on"
fi

echo "[FIPS Detection] Final CMS_SSL_ENGINE value: ${CMS_SSL_ENGINE}"