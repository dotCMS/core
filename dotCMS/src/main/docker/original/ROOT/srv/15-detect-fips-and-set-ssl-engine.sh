#!/bin/bash

# FIPS Mode Detection and APR SSL Engine Configuration
# =====================================================
# This script automatically detects FIPS-enabled environments and disables the
# Tomcat Native APR SSL Engine to prevent JVM crashes with OpenSSL 3.x.
#
# The Tomcat Native APR library (libtcnative-1) version 1.2.35 is incompatible
# with OpenSSL 3.x when running in FIPS mode, causing segmentation faults.
# Setting SSLEngine=off alone is insufficient: libtcnative-1 still loads
# libcrypto.so.3 and calls OpenSSL for non-SSL operations (e.g. random number
# generation), which triggers the same FIPS provider crash.
#
# When FIPS mode is detected, the AprLifecycleListener is removed from
# server.xml at runtime (before Tomcat starts) so that libtcnative-1 is never
# loaded by Tomcat at all. server.xml lives under /srv/dotserver/tomcat/conf/
# which is owned by the dotcms user, so no root privileges are needed.
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
    # SSLEngine=off alone does not prevent libtcnative-1 from loading libcrypto.so.3
    # and calling OpenSSL for non-SSL operations. Remove the AprLifecycleListener
    # from server.xml so Tomcat never loads the native library at all.
    # server.xml is under /srv (owned by dotcms user) so no root access is needed.
    TOMCAT_SERVER_XML="${TOMCAT_HOME:-/srv/dotserver/tomcat}/conf/server.xml"
    if [[ -f "${TOMCAT_SERVER_XML}" ]]; then
        sed -i '/AprLifecycleListener/d' "${TOMCAT_SERVER_XML}"
        if ! grep -q 'AprLifecycleListener' "${TOMCAT_SERVER_XML}"; then
            echo "[FIPS Detection] AprLifecycleListener removed from server.xml — libtcnative-1 will not be loaded"
        else
            echo "[FIPS Detection] WARNING: Failed to remove AprLifecycleListener from ${TOMCAT_SERVER_XML} — JVM may still crash"
        fi
    else
        echo "[FIPS Detection] WARNING: server.xml not found at ${TOMCAT_SERVER_XML} — libtcnative-1 may still load"
    fi
    export CMS_SSL_ENGINE="off"
else
    # Default: Keep APR SSL Engine enabled for performance benefits
    echo "[FIPS Detection] APR SSL Engine enabled (default) for optimal performance"
    echo "[FIPS Detection] To disable APR SSL Engine, set CMS_DISABLE_APR_SSL=true or CMS_SSL_ENGINE=off"
    export CMS_SSL_ENGINE="on"
fi

echo "[FIPS Detection] Final CMS_SSL_ENGINE value: ${CMS_SSL_ENGINE}"