#!/bin/bash

###
##   Configuration defaults for Docker container configuration
##    Please do not alter this file, but set these as environment variables on the container launch if needed.
###

set -e

## Tomcat config

# Auto-set Tomcat version from container build var, used for proper pathing
TOMCAT_HOME=$( find /srv/dotserver/ -type d -name "tomcat-*" )

# JAVA args to pass to the Tomcat JVM
JAVA_OPTS_BASE=${JAVA_OPTS_BASE:-"-Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -server -XX:+DisableExplicitGC -Dpdfbox.fontcache=/data/local/dotsecure"}
JAVA_OPTS_AGENT=${JAVA_OPTS_AGENT:-"-javaagent:${TOMCAT_HOME}/webapps/ROOT/WEB-INF/lib/byte-buddy-agent-1.9.0.jar"}
JAVA_OPTS_MEMORY=${JAVA_OPTS_MEMORY:-"-Xmx1G"}
JAVA_OPTS=${JAVA_OPTS:-"$JAVA_OPTS_BASE $JAVA_OPTS_AGENT $JAVA_OPTS_MEMORY"}


# Maximum number of Tomcat Connector threadpool threads (shared across Connectors)
CMS_CONNECTOR_THREADS=${CMS_CONNECTOR_THREADS:-"600"}

# SMTP hostname for CMS
CMS_SMTP_HOST=${CMS_SMTP_HOST:-"smtp.dotcms.site"}
DOT_MAIL_SMTP_SSL_PROTOCOLS=${DOT_MAIL_SMTP_SSL_PROTOCOLS:-"TLSv1.2"}

# tomcat gzip compression
CMS_COMPRESSION=${CMS_COMPRESSION:-"on"}
CMS_NOCOMPRESSIONSTRONGETAG=${CMS_NOCOMPRESSIONSTRONGETAG:-"false"}
CMS_COMPRESSIBLEMIMETYPE=${CMS_COMPRESSIBLEMIMETYPE:-"text/html,text/xml,text/csv,text/css,text/javascript,text/json,application/javascript,application/json,application/xml,application/x-javascript,font/eot,font/otf,font/ttf,image/svg+xml"}

# Access Log and Remote IP Valve
CMS_ACCESSLOG_PATTERN=${CMS_ACCESSLOG_PATTERN:-"%{Host}i %{org.apache.catalina.AccessLog.RemoteAddr}r %l %u %t &quot;%r&quot; %s %b %D %{Referer}i %{User-Agent}i"}
CMS_ACCESSLOG_FILEDATEFORMAT=${CMS_ACCESSLOG_FILEDATEFORMAT:-".yyyy-MM-dd"}
CMS_ACCESSLOG_MAXDAYS=${CMS_ACCESSLOG_MAXDAYS:-"-1"}
CMS_REMOTEIP_REMOTEIPHEADER=${CMS_REMOTEIP_REMOTEIPHEADER:-"x-forwarded-for"}
CMS_REMOTEIP_INTERNALPROXIES=${CMS_REMOTEIP_INTERNALPROXIES:-"10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|169\.254\.\d{1,3}\.\d{1,3}|127\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.1[6-9]{1}\.\d{1,3}\.\d{1,3}|172\.2[0-9]{1}\.\d{1,3}\.\d{1,3}|172\.3[0-1]{1}\.\d{1,3}\.\d{1,3}|0:0:0:0:0:0:0:1"}


# ASSET/INTERNAL PATHS (SHOULD NOT CHANGE)
DOT_ASSET_REAL_PATH=${DOT_ASSET_REAL_PATH:-"/data/shared/assets"}
DOT_DYNAMIC_CONTENT_PATH=${DOT_DYNAMIC_CONTENT_PATH:-"/data/local/dotsecure"}
DOT_TAIL_LOG_LOG_FOLDER=${DOT_TAIL_LOG_LOG_FOLDER:-"$TOMCAT_HOME/logs"}

# OSGi felix install directory 
DOT_FELIX_FELIX_UNDEPLOYED_DIR=${DOT_FELIX_FELIX_UNDEPLOYED_DIR:-"/data/shared/felix/undeployed"}
DOT_FELIX_FELIX_FILE_INSTALL_DIR=${DOT_FELIX_FELIX_FILE_INSTALL_DIR:-"/data/shared/felix/load"}
DOT_FELIX_FELIX_UPLOAD_DIR=${DOT_FELIX_FELIX_UPLOAD_DIR:-"/data/shared/felix/upload"}

# MISC
DOT_PREVENT_SESSION_FIXATION_ON_LOGIN=${DOT_PREVENT_SESSION_FIXATION_ON_LOGIN:-"false"}

## Database config
DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS="com.dotmarketing.db.SystemEnvDataSourceStrategy"
DB_DRIVER=${DB_DRIVER:-"org.postgresql.Driver"}
DB_BASE_URL=${DB_BASE_URL:-"jdbc:postgresql://db.dotcms.site/dotcms"}
DB_USERNAME=${DB_USERNAME:-"dotcmsdbuser"}
DB_PASSWORD=${DB_PASSWORD:-"password"}
DB_MAX_WAIT=${DB_MAX_WAIT:-"180000"}
DB_MAX_TOTAL=${DB_MAX_TOTAL:-"200"}
DB_MIN_IDLE=${DB_MIN_IDLE:-"10"}
DB_VALIDATION_QUERY=${DB_VALIDATION_QUERY:-""}
DB_LEAK_DETECTION_THRESHOLD=${DB_LEAK_DETECTION_THRESHOLD:-"300000"}
DB_DEFAULT_TRANSACTION_ISOLATION=${DB_DEFAULT_TRANSACTION_ISOLATION:-""}

## Elasticsearch config
# ES Auth Type = BASIC|JWT
DOT_ES_AUTH_TYPE=${DOT_ES_AUTH_TYPE:-"BASIC"}
DOT_ES_AUTH_BASIC_USER=${DOT_ES_AUTH_BASIC_USER:-"admin"}
DOT_ES_AUTH_BASIC_PASSWORD=${DOT_ES_AUTH_BASIC_PASSWORD:-"admin"}
DOT_ES_AUTH_JWT_TOKEN=${DOT_ES_AUTH_JWT_TOKEN:-""}
DOT_ES_ENDPOINTS=${DOT_ES_ENDPOINTS:-"https://es.dotcms.site:9200"}

## if you want to provide a custom starter for the initial data load, specify
# a url here and dotCMS will download it before starting up
CUSTOM_STARTER_URL=${CUSTOM_STARTER_URL:-""}

