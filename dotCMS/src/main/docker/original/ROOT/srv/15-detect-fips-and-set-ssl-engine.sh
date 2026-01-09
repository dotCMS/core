#!/bin/bash

set -e

# FIPS Mode Detection and APR SSL Engine Configuration
# =====================================================
# This script automatically detects FIPS-enabled environments and disables the
# Tomcat Native APR SSL Engine to prevent JVM crashes with OpenSSL 3.x.
#
# The Tomcat Native APR library (libtcnative-1) version 1.2.35 is incompatible
# with OpenSSL 3.x when running in FIPS mode, causing segmentation faults.
#
# Configuration Options:
# ----------------------
# 1. Automatic FIPS Detection (default behavior):
#    - The script checks /proc/sys/crypto/fips_enabled
#    - If FIPS is enabled, CMS_SSL_ENGINE is automatically set to 'off'
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
    exit 0
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
    export CMS_SSL_ENGINE="off"
else
    # Default: Keep APR SSL Engine enabled for performance benefits
    echo "[FIPS Detection] APR SSL Engine enabled (default) for optimal performance"
    echo "[FIPS Detection] To disable APR SSL Engine, set CMS_DISABLE_APR_SSL=true or CMS_SSL_ENGINE=off"
    export CMS_SSL_ENGINE="on"
fi

echo "[FIPS Detection] Final CMS_SSL_ENGINE value: ${CMS_SSL_ENGINE}"
