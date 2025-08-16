#!/bin/bash

###
##   Configuration defaults for Docker container configuration
##   Please do not alter this file, but set these as environment 
##   variables on the container launch if needed.
###

set -e

## Tomcat config

export TOMCAT_HOME=/srv/dotserver/tomcat
# JAVA args to pass to the Tomcat JVM
export JAVA_OPTS_BASE=${JAVA_OPTS_BASE:-"-Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -server -Dpdfbox.fontcache=/data/local/dotsecure -Dlog4j2.formatMsgNoLookups=true -Djava.library.path=/usr/lib/$( uname -m )-linux-gnu/ -XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:ShenandoahUncommitDelay=1000 -XX:ShenandoahGuaranteedGCInterval=10000  "}
export JAVA_OPTS_MEMORY=${JAVA_OPTS_MEMORY:-"-Xmx1G"}

# $CMS_JAVA_OPTS is last so it trumps them all
export JAVA_OPTS=${JAVA_OPTS:-"$JAVA_OPTS_BASE $JAVA_OPTS_AGENT $JAVA_OPTS_MEMORY $CMS_JAVA_OPTS"}


# Maximum number of Tomcat Connector threadpool threads (shared across Connectors)
export CMS_CONNECTOR_THREADS=${CMS_CONNECTOR_THREADS:-"600"}

# SMTP hostname for CMS
export DOT_MAIL_SMTP_HOST=${DOT_MAIL_SMTP_HOST:-"smtp.dotcms.site"}
export DOT_MAIL_SMTP_SSL_PROTOCOLS=${DOT_MAIL_SMTP_SSL_PROTOCOLS:-"TLSv1.2"}

# Cookie Args
export DOT_SAMESITE_COOKIES=${DOT_SAMESITE_COOKIES:-"lax"}


# tomcat gzip compression
export CMS_COMPRESSION=${CMS_COMPRESSION:-"on"}
export CMS_NOCOMPRESSIONSTRONGETAG=${CMS_NOCOMPRESSIONSTRONGETAG:-"false"}
export CMS_COMPRESSIBLEMIMETYPE=${CMS_COMPRESSIBLEMIMETYPE:-"text/html,text/xml,text/csv,text/css,text/javascript,text/json,application/javascript,application/json,application/xml,application/x-javascript,font/eot,font/otf,font/ttf,image/svg+xml"}

# Access Log and Remote IP Valve
export CMS_ACCESSLOG_PATTERN=${CMS_ACCESSLOG_PATTERN:-"%{Host}i %{org.apache.catalina.AccessLog.RemoteAddr}r %l %u %t &quot;%r&quot; %s %b %D %{Referer}i %{User-Agent}i"}
export CMS_ACCESSLOG_FILEDATEFORMAT=${CMS_ACCESSLOG_FILEDATEFORMAT:-".yyyy-MM-dd"}
export CMS_ACCESSLOG_MAXDAYS=${CMS_ACCESSLOG_MAXDAYS:-"-1"}
export CMS_REMOTEIP_REMOTEIPHEADER=${CMS_REMOTEIP_REMOTEIPHEADER:-"x-forwarded-for"}
export CMS_REMOTEIP_INTERNALPROXIES=${CMS_REMOTEIP_INTERNALPROXIES:-"10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|169\.254\.\d{1,3}\.\d{1,3}|127\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.1[6-9]{1}\.\d{1,3}\.\d{1,3}|172\.2[0-9]{1}\.\d{1,3}\.\d{1,3}|172\.3[0-1]{1}\.\d{1,3}\.\d{1,3}|0:0:0:0:0:0:0:1"}


# ASSET/INTERNAL PATHS (SHOULD NOT CHANGE)
export DOT_ASSET_REAL_PATH=${DOT_ASSET_REAL_PATH:-"/data/shared/assets"}
export DOT_DYNAMIC_CONTENT_PATH=${DOT_DYNAMIC_CONTENT_PATH:-"/data/local/dotsecure"}
export DOT_TAIL_LOG_LOG_FOLDER=${DOT_TAIL_LOG_LOG_FOLDER:-"$TOMCAT_HOME/logs"}

# OSGi felix install directory 
export DOT_FELIX_FELIX_UNDEPLOYED_DIR=${DOT_FELIX_FELIX_UNDEPLOYED_DIR:-"/data/shared/felix/undeployed"}
export DOT_FELIX_FELIX_FILEINSTALL_DIR=${DOT_FELIX_FELIX_FILEINSTALL_DIR:-"/data/shared/felix/load"}
export DOT_FELIX_FELIX_UPLOAD_DIR=${DOT_FELIX_FELIX_UPLOAD_DIR:-"/data/shared/felix/upload"}
export DOT_FELIX_FELIX_CACHE_LOCKING=${DOT_FELIX_FELIX_CACHE_LOCKING:-"false"}
export DOT_FELIX_FELIX_LOG_LEVEL=${DOT_FELIX_FELIX_LOG_LEVEL:-"3"}

# MISC
export DOT_PREVENT_SESSION_FIXATION_ON_LOGIN=${DOT_PREVENT_SESSION_FIXATION_ON_LOGIN:-"false"}

## Database config
export DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS="com.dotmarketing.db.SystemEnvDataSourceStrategy"
export DB_DRIVER=${DB_DRIVER:-"org.postgresql.Driver"}
export DB_BASE_URL=${DB_BASE_URL:-"jdbc:postgresql://db.dotcms.site/dotcms"}
export DB_USERNAME=${DB_USERNAME:-"dotcmsdbuser"}
export DB_PASSWORD=${DB_PASSWORD:-"password"}
export DB_MAX_WAIT=${DB_MAX_WAIT:-"180000"}
export DB_MAX_TOTAL=${DB_MAX_TOTAL:-"200"}
export DB_MIN_IDLE=${DB_MIN_IDLE:-"10"}
export DB_VALIDATION_QUERY=${DB_VALIDATION_QUERY:-""}
export DB_LEAK_DETECTION_THRESHOLD=${DB_LEAK_DETECTION_THRESHOLD:-"300000"}
export DB_DEFAULT_TRANSACTION_ISOLATION=${DB_DEFAULT_TRANSACTION_ISOLATION:-""}

## Elasticsearch config
# ES Auth Type = BASIC|JWT
export DOT_ES_AUTH_TYPE=${DOT_ES_AUTH_TYPE:-"BASIC"}
export DOT_ES_AUTH_BASIC_USER=${DOT_ES_AUTH_BASIC_USER:-"admin"}
export DOT_ES_AUTH_BASIC_PASSWORD=${DOT_ES_AUTH_BASIC_PASSWORD:-"admin"}
export DOT_ES_AUTH_JWT_TOKEN=${DOT_ES_AUTH_JWT_TOKEN:-""}
export DOT_ES_ENDPOINTS=${DOT_ES_ENDPOINTS:-"https://es.dotcms.site:9200"}
export DOT_ES_INDEX_MAPPING_TOTAL_FIELD_LIMITS=${DOT_ES_INDEX_MAPPING_TOTAL_FIELD_LIMITS:-"10000"}

## if you want to provide a custom starter for the initial data load, specify
# a url here and dotCMS will download it before starting up
export CUSTOM_STARTER_URL=${CUSTOM_STARTER_URL:-""}

## This needs to be set in order for catalina to read environmental properties
export CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource"


