#!/bin/sh

# Set default environment variables
export LANG=${LANG:-"C.UTF-8"}

export JAVA_OPTS_BASE=${JAVA_OPTS_BASE:-"-Djava.awt.headless=true -Dfile.encoding=UTF8 -server -Dpdfbox.fontcache=/data/local/dotsecure -Dlog4j2.formatMsgNoLookups=true -Djava.library.path=/usr/lib/$( uname -m )-linux-gnu/ -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:+ZGenerational "}

export JAVA_OPTS_MEMORY=${JAVA_OPTS_MEMORY:-"-Xmx1G"}

# $CMS_JAVA_OPTS is last so it trumps them all
export JAVA_OPTS=${JAVA_OPTS:-"$JAVA_OPTS_BASE $JAVA_OPTS_AGENT $JAVA_OPTS_MEMORY $CMS_JAVA_OPTS"}

# Asset and Internal Paths
export DOT_ASSET_REAL_PATH=${DOT_ASSET_REAL_PATH:-"/data/shared/assets"}
export DOT_DYNAMIC_CONTENT_PATH=${DOT_DYNAMIC_CONTENT_PATH:-"/data/local/dotsecure"}
export DOT_TAIL_LOG_LOG_FOLDER=${DOT_TAIL_LOG_LOG_FOLDER:-"$CATALINA_HOME/logs"}

# OSGi felix directories
export DOT_FELIX_FELIX_UNDEPLOYED_DIR=${DOT_FELIX_FELIX_UNDEPLOYED_DIR:-"/data/shared/felix/undeployed"}
export DOT_FELIX_FELIX_FILEINSTALL_DIR=${DOT_FELIX_FELIX_FILEINSTALL_DIR:-"/data/shared/felix/load"}
export DOT_FELIX_FELIX_UPLOAD_DIR=${DOT_FELIX_FELIX_UPLOAD_DIR:-"/data/shared/felix/upload"}

# Database Configuration
export DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS="com.dotmarketing.db.SystemEnvDataSourceStrategy"
export DB_DRIVER=${DB_DRIVER:-"org.postgresql.Driver"}
export DB_BASE_URL=${DB_BASE_URL:-"jdbc:postgresql://db.dotcms.site/dotcms"}
export DB_USERNAME=${DB_USERNAME:-"dotcmsdbuser"}
export DB_PASSWORD=${DB_PASSWORD:-"password"}
export DB_HOST=${DB_HOST:-"db.dotcms.site"}
export DB_NAME=${DB_NAME:-"dotcms"}
export DB_MAX_WAIT=${DB_MAX_WAIT:-"180000"}
export DB_MAX_TOTAL=${DB_MAX_TOTAL:-"200"}
export DB_CONNECTION_TIMEOUT=${DB_CONNECTION_TIMEOUT:-"5000"}
export DB_MIN_IDLE=${DB_MIN_IDLE:-"10"}
export DB_VALIDATION_QUERY=${DB_VALIDATION_QUERY:-""}
export DB_LEAK_DETECTION_THRESHOLD=${DB_LEAK_DETECTION_THRESHOLD:-"300000"}
export DB_DEFAULT_TRANSACTION_ISOLATION=${DB_DEFAULT_TRANSACTION_ISOLATION:-""}
export DB_LOAD_DUMP_SQL=${DB_LOAD_DUMP_SQL:-""}

# Elasticsearch Configuration
export DOT_ES_AUTH_TYPE=${DOT_ES_AUTH_TYPE:-"BASIC"}
export DOT_ES_AUTH_BASIC_USER=${DOT_ES_AUTH_BASIC_USER:-"admin"}
export DOT_ES_AUTH_BASIC_PASSWORD=${DOT_ES_AUTH_BASIC_PASSWORD:-"admin"}
export DOT_ES_AUTH_JWT_TOKEN=${DOT_ES_AUTH_JWT_TOKEN:-""}
export DOT_ES_ENDPOINTS=${DOT_ES_ENDPOINTS:-"https://es.dotcms.site:9200"}

# Misc Settings
export DOT_PREVENT_SESSION_FIXATION_ON_LOGIN=${DOT_PREVENT_SESSION_FIXATION_ON_LOGIN:-"false"}
export CUSTOM_STARTER_URL=${CUSTOM_STARTER_URL:-""}

# Tomcat connector and compression settings
export CMS_CONNECTOR_THREADS=${CMS_CONNECTOR_THREADS:-"600"}
export CMS_COMPRESSION=${CMS_COMPRESSION:-"on"}
export CMS_NOCOMPRESSIONSTRONGETAG=${CMS_NOCOMPRESSIONSTRONGETAG:-"false"}
export CMS_COMPRESSIBLEMIMETYPE=${CMS_COMPRESSIBLEMIMETYPE:-"text/html,text/xml,text/csv,text/css,text/javascript,text/json,application/javascript,application/json,application/xml,application/x-javascript,font/eot,font/otf,font/ttf,image/svg+xml"}

# Redis Session Configuration
if [ "${TOMCAT_REDIS_SESSION_ENABLED}" = 'true' ]; then
  export TOMCAT_REDIS_SESSION_HOST=${TOMCAT_REDIS_SESSION_HOST:-"redis"}
  export TOMCAT_REDIS_SESSION_PORT=${TOMCAT_REDIS_SESSION_PORT:-"6379"}
  export TOMCAT_REDIS_SESSION_USERNAME=${TOMCAT_REDIS_SESSION_USERNAME:-""}
  export TOMCAT_REDIS_SESSION_PASSWORD=${TOMCAT_REDIS_SESSION_PASSWORD:-""}
  export TOMCAT_REDIS_SESSION_SSL_ENABLED=${TOMCAT_REDIS_SESSION_SSL_ENABLED:-"false"}
  export TOMCAT_REDIS_SESSION_SENTINEL_MASTER=${TOMCAT_REDIS_SESSION_SENTINEL_MASTER}
  export TOMCAT_REDIS_SESSION_SENTINELS=${TOMCAT_REDIS_SESSION_SENTINELS}
  export TOMCAT_REDIS_SESSION_DATABASE=${TOMCAT_REDIS_SESSION_DATABASE:-"0"}
  export TOMCAT_REDIS_SESSION_TIMEOUT=${TOMCAT_REDIS_SESSION_TIMEOUT:-"2000"}
  export TOMCAT_REDIS_SESSION_PERSISTENT_POLICIES=${TOMCAT_REDIS_SESSION_PERSISTENT_POLICIES}
  export TOMCAT_REDIS_MAX_CONNECTIONS=${TOMCAT_REDIS_MAX_CONNECTIONS:-"128"}
  export TOMCAT_REDIS_MAX_IDLE_CONNECTIONS=${TOMCAT_REDIS_MAX_IDLE_CONNECTIONS:-"100"}
  export TOMCAT_REDIS_MIN_IDLE_CONNECTIONS=${TOMCAT_REDIS_MIN_IDLE_CONNECTIONS:-"32"}
  export TOMCAT_REDIS_ENABLED_FOR_ANON_TRAFFIC=${TOMCAT_REDIS_ENABLED_FOR_ANON_TRAFFIC:-"false"}
  export TOMCAT_REDIS_UNDEFINED_SESSION_TYPE_TIMEOUT=${TOMCAT_REDIS_UNDEFINED_SESSION_TYPE_TIMEOUT:-"15"}
fi

# Access Log settings
export CMS_ACCESSLOG_PATTERN=${CMS_ACCESSLOG_PATTERN:-'%{Host}i %{org.apache.catalina.AccessLog.RemoteAddr}r %l %u %t "%r" %s %b %D %{Referer}i %{User-Agent}i'}
export CMS_ACCESSLOG_FILEDATEFORMAT=${CMS_ACCESSLOG_FILEDATEFORMAT:-".yyyy-MM-dd"}
export CMS_ACCESSLOG_MAXDAYS=${CMS_ACCESSLOG_MAXDAYS:-"-1"}
export CMS_ACCESSLOG_DIRECTORY=${CMS_ACCESSLOG_DIRECTORY:-"logs"}
export CMS_ACCESSLOG_RENAMEONROTATE=${CMS_ACCESSLOG_RENAMEONROTATE:-"false"}
export CMS_ACCESSLOG_ROTATABLE=${CMS_ACCESSLOG_ROTATABLE:-"true"}

# Remote IP Valve settings
export CMS_REMOTEIP_REMOTEIPHEADER=${CMS_REMOTEIP_REMOTEIPHEADER:-"x-forwarded-for"}
export CMS_REMOTEIP_INTERNALPROXIES=${CMS_REMOTEIP_INTERNALPROXIES:-"10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|169\\.254\\.\\d{1,3}\\.\\d{1,3}|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|0:0:0:0:0:0:0:1"}
# Cookie settings
export DOT_SAMESITE_COOKIES=${DOT_SAMESITE_COOKIES:-"lax"}

# SMTP settings
export DOT_MAIL_SMTP_HOST=${DOT_MAIL_SMTP_HOST:-"smtp.dotcms.site"}
export DOT_MAIL_SMTP_SSL_PROTOCOLS=${DOT_MAIL_SMTP_SSL_PROTOCOLS:-"TLSv1.2"}

# Set environment variable for mimalloc
export LD_PRELOAD=${LD_PRELOAD:-"/usr/lib/`uname -m`-linux-gnu/libmimalloc.so.2"}

# This needs to be set in order for catalina to read environmental properties
export CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource"

export CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF8"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.io=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.lang.ref=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.net=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.nio=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.nio.charset=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.time=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.time.zone=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util.concurrent=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util.concurrent.locks=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util.regex=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/javax.crypto.spec=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.management/javax.management=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/sun.nio.cs=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/sun.util.calendar=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/sun.util.locale=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/jdk.internal.misc=ALL-UNNAMED"

export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"

# Set Log4j properties if not already set
if echo "$CATALINA_OPTS" | grep -q '\-Dlog4j2\.configurationFile'; then
  echo "Log4j configuration already set"
else
  echo "Setting log4j2.configurationFile=$CATALINA_HOME/webapps/ROOT/WEB-INF/log4j/log4j2.xml"
  export CATALINA_OPTS="$CATALINA_OPTS -Dlog4j2.configurationFile=$CATALINA_HOME/webapps/ROOT/WEB-INF/log4j/log4j2.xml"
fi

if echo "$CATALINA_OPTS" | grep -q '\-DLog4jContextSelector'; then
  echo "Log4jContextSelector already set"
else
  echo "Setting Log4jContextSelector to org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector"
  export CATALINA_OPTS="$CATALINA_OPTS -DLog4jContextSelector=org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector"
fi

# Disable log4j automatic shutdown hook to allow dotCMS to control shutdown order
if echo "$CATALINA_OPTS" | grep -q '\-Dlog4j2\.shutdownHookEnabled'; then
  echo "Log4j shutdown hook setting already configured"
else
  echo "Disabling log4j automatic shutdown hook - dotCMS will control logging shutdown"
  export CATALINA_OPTS="$CATALINA_OPTS -Dlog4j2.shutdownHookEnabled=false"
fi

ADDITIONAL_CLASSPATH="$CATALINA_HOME/log4j2/lib/*"

# Set CLASSPATH with additional path if necessary
if [ -n "$CLASSPATH" ]; then
  CLASSPATH="$CLASSPATH:$ADDITIONAL_CLASSPATH"
else
  CLASSPATH="$ADDITIONAL_CLASSPATH"
fi


# CATALINA_OPTS: Used to pass options to the JVM running Tomcat. This script appends various options to CATALINA_OPTS to configure encoding, module access, XML parser implementations, and Log4j settings.
# GLOWROOT_ENABLED: If set to "true", the Glowroot agent is added to CATALINA_OPTS.
# GLOWROOT_CONF_DIR: Directory for Glowroot configuration files. Defaults to $CATALINA_HOME/glowroot/local-web if GLOWROOT_WEB_UI_ENABLED is "true", otherwise defaults to $GLOWROOT_SHARED_FOLDER.
# GLOWROOT_LOG_DIR: Directory for Glowroot log files. Defaults to $GLOWROOT_CONF_DIR/logs if not set.
# GLOWROOT_TMP_DIR: Directory for Glowroot temporary files. Defaults to $GLOWROOT_CONF_DIR/tmp if not set.
# GLOWROOT_DATA_DIR: Directory for Glowroot data files. Defaults to $GLOWROOT_SHARED_FOLDER/data if not set.
# GLOWROOT_AGENT_ID: If set, specifies the agent ID for Glowroot and enables multi-directory mode.
# GLOWROOT_COLLECTOR_ADDRESS: If set, specifies the collector address for Glowroot.

if [ -z "$CATALINA_TMPDIR" ]; then
      CATALINA_TMPDIR="$CATALINA_HOME/temp"
fi

# Clean up temp directory before startup to prevent accumulation of old files
# Can be disabled by setting DOTCMS_DISABLE_TEMP_CLEANUP=true
# Age threshold can be configured via DOTCMS_TEMP_CLEANUP_AGE_MINUTES (default: 60 minutes)
if [ "${DOTCMS_DISABLE_TEMP_CLEANUP}" != "true" ]; then
  if [ -d "${CATALINA_TMPDIR}" ]; then
    CLEANUP_AGE_MINUTES=${DOTCMS_TEMP_CLEANUP_AGE_MINUTES:-60}
    echo "Cleaning up temp directory (${CATALINA_TMPDIR}) before dotCMS startup..."
    echo "Removing files older than ${CLEANUP_AGE_MINUTES} minutes..."
    find "${CATALINA_TMPDIR}" -type f -user $(id -u) -mmin +${CLEANUP_AGE_MINUTES} -delete 2>/dev/null || true
    echo "Temp directory cleanup completed"
  fi
fi

add_glowroot_agent() {
    if ! echo "$CATALINA_OPTS" | grep -q '\-javaagent:.*glowroot\.jar'; then
        if [ "$GLOWROOT_ENABLED" = "true" ]; then
          echo "Adding Glowroot agent to CATALINA_OPTS"
          export CATALINA_OPTS="$CATALINA_OPTS -javaagent:$CATALINA_HOME/glowroot/glowroot.jar"

          export GLOWROOT_SHARED_FOLDER="/data/shared/glowroot"

          # Set GLOWROOT_CONF_DIR based on GLOWROOT_WEB_UI_ENABLED
          if [ -z "$GLOWROOT_CONF_DIR" ]; then
              GLOWROOT_CONF_DIR="$([ "$GLOWROOT_WEB_UI_ENABLED" = "true" ] && echo "$CATALINA_HOME/glowroot/local-web" || echo "$GLOWROOT_SHARED_FOLDER")"
          fi
          CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.conf.dir=$GLOWROOT_CONF_DIR"
          # We may want to modify these defaults
          CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.tmp.dir=${GLOWROOT_TMP_DIR:=/tmp}"
          # Only used if when not using a collector
          CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.data.dir=${GLOWROOT_DATA_DIR:=$GLOWROOT_SHARED_FOLDER/data}"

          # Set GLOWROOT_AGENT_ID and enable multi-directory mode if defined
          [ -n "$GLOWROOT_AGENT_ID" ] && CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.agent.id=$GLOWROOT_AGENT_ID -Dglowroot.multi.dir=true"
          [ -n "$GLOWROOT_COLLECTOR_ADDRESS" ] && CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.collector.address=$GLOWROOT_COLLECTOR_ADDRESS"

        fi
    else
      echo "Using Legacy Glowroot agent settings from CATALINA_OPTS"
    fi
}

# Run the function to add Glowroot agent settings to CATALINA_OPTS if enabled
add_glowroot_agent
export CATALINA_OPTS
export CLASSPATH
