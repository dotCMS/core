#!/bin/bash

###
##   Configuration defaults for Docker container configuration and service discovery
##    Please do not alter this file, but set these as environment variables on the container launch if needed.
###

set -e

## Tomcat config

# Auto-set Tomcat version from container build var, used for proper pathing
TOMCAT_HOME=$( find /srv/ -type d -name "/srv/dotserver/tomcat-*" )

# Extra JVM args to pass to the Tomcat JVM
BASE_JAVA_OPTS=${BASE_JAVA_OPTS:"-Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -server -XX:+DisableExplicitGC -Dpdfbox.fontcache=/data/local/dotsecure"}
AGENT_JAVA_OPTS=${AGENT_JAVA_OPS:"-javaagent:${TOMCAT_HOME}/webapps/ROOT/WEB-INF/lib/byte-buddy-agent-1.9.0.jar"}
MEMORY_JAVA_OPTS=${MEMORY_JAVA_OPTS:"-Xmx1G"}
JAVA_OPTS=${JAVA_OPTS:"$BASE_JAVA_OPTS $AGENT_JAVA_OPTS $MEMORY_JAVA_OPTS"}


# Maximum number of Tomcat Connector threadpool threads (shared across Connectors)
CMS_CONNECTOR_THREADS=${CMS_CONNECTOR_THREADS:"600"}

# SMTP hostname for CMS
CMS_SMTP_HOST=${CMS_SMTP_HOST:"smtp.dotcms.site"}

# PATHS (SHOULD NOT CHANGE)
DOT_ASSET_REAL_PATH=${DOT_ASSET_REAL_PATH:"/data/shared/assets"}
DOT_DYNAMIC_CONTENT_PATH=${DOT_DYNAMIC_CONTENT_PATH:"/data/local/dotsecure"}


## Database config

# IP/hostname of database server
PROVIDER_DB_DNSNAME=${PROVIDER_DB_DNSNAME:"db.dotcms.site"}

# Database type, one of ["POSTGRES","MYSQL","ORACLE","MSSQL"]
PROVIDER_DB_DRIVER=${PROVIDER_DB_DRIVER:"POSTGRES"}

# Database name
PROVIDER_DB_DBNAME=${PROVIDER_DB_DBNAME:"dotcms"}

# Database tcp port number. If unset, will use default per database type
PROVIDER_DB_PORT=${PROVIDER_DB_PORT:"5432"} 

# JDBC-compliant URL to connect to DB (only needed to set custom options, PROVIDER_DB_DNSNAME & PROVIDER_DB_PORT must also be set or use defaults)
PROVIDER_DB_URL=${PROVIDER_DB_URL:""}

# Database username
PROVIDER_DB_USERNAME=${PROVIDER_DB_USERNAME:"dotcmsdbuser"}

# Database password
PROVIDER_DB_PASSWORD=${PROVIDER_DB_PASSWORD:"password"}

# Maximum number of database connections
PROVIDER_DB_MAXCONNS=${PROVIDER_DB_MAXCONNS:"200"}

## Elasticsearch config
DOT_ES_AUTH_TYPE=${DOT_ES_AUTH_TYPE:"BASIC"}
DOT_ES_AUTH_BASIC_USER=${DOT_ES_AUTH_BASIC_USER:"admin"}
DOT_ES_AUTH_BASIC_PASSWORD=${DOT_ES_AUTH_BASIC_PASSWORD:"admin"}
DOT_ES_ENDPOINTS=${DOT_ES_ENDPOINTS:"https://es.dotcms.site:9200"}

## External Hazelcast service
# IP or hostname of external Hazelcast service (can be multi-valued RR DNS record). Leave unset for embedded Hazelcast
PROVIDER_HAZELCAST_DNSNAMES=${PROVIDER_HAZELCAST_DNSNAMES:""}
PROVIDER_HAZELCAST_GROUPNAME=${PROVIDER_HAZELCAST_GROUPNAME:"dotCMS"}
PROVIDER_HAZELCAST_GROUPPASSWORD=${PROVIDER_HAZELCAST_GROUPPASSWORD:""}
PROVIDER_HAZELCAST_PORT=${PROVIDER_HAZELCAST_PORT:"5701"}

## if you want to provide a custom starter for the initial data load, specify
# a url here and dotCMS will download it before starting up
CUSTOM_STARTER_URL=${CUSTOM_STARTER_URL:""}





