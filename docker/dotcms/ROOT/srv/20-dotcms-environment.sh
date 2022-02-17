#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

echo "dotCMS environment ...."
# Default opts
JAVA_OPTS="-Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -server -XX:+DisableExplicitGC -Dsun.reflect.inflationThreshold=2147483647 -XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:ShenandoahUncommitDelay=5000 -XX:ShenandoahGuaranteedGCInterval=10000"
# Memory opts
JAVA_OPTS="$JAVA_OPTS -Xmx${CMS_HEAP_SIZE}"

# Agent opts
JAVA_OPTS="$JAVA_OPTS -javaagent:${TOMCAT_HOME}/webapps/ROOT/WEB-INF/lib/byte-buddy-agent-1.9.0.jar"
# PDFbox cache location
JAVA_OPTS="$JAVA_OPTS -Dpdfbox.fontcache=/data/local/dotsecure"

# CVE-2021-44228 mitigation https://nvd.nist.gov/vuln/detail/CVE-2021-44228
JAVA_OPTS="$JAVA_OPTS -Dlog4j2.formatMsgNoLookups=true"


# Finally, add user-provided JAVA_OPTS
JAVA_OPTS="$JAVA_OPTS ${CMS_JAVA_OPTS}"

echo "HOSTNAME=${HOSTNAME}" >>/srv/config/settings.ini
echo "LANG=${LANG}" >>/srv/config/settings.ini
echo "JAVA_HOME=${JAVA_HOME}" >>/srv/config/settings.ini
echo "JAVA_OPTS=${JAVA_OPTS}" >>/srv/config/settings.ini
