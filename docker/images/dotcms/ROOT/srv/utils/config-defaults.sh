#!/bin/bash

###
##   Configuration defaults for Docker container configuration and service discovery
##    Please do not alter this file, but set these as environment variables on the container launch if needed.
###

set -e


## Tomcat config

# Auto-set Tomcat version from container build var, used for proper pathing
[[ -f /srv/TOMCAT_VERSION ]] && TOMCAT_VERSION=$( cat /srv/TOMCAT_VERSION )
TOMCAT_HOME=/srv/dotserver/tomcat-${TOMCAT_VERSION}

# Java heap size used for Tomcat/dotCMS app
: ${CMS_HEAP_SIZE:="1G"}

# Extra JVM args to pass to the Tomcat JVM
: ${CMS_JAVA_OPTS:=""}

# Maximum number of Tomcat Connector thread pool threads (shared across Connectors)
: ${CMS_CONNECTOR_THREADS:="200"}

# SMTP hostname for CMS
: ${CMS_SMTP_HOST:="smtp"}

# dotCMS runas user UID
: ${CMS_RUNAS_UID:="999999"}

# dotCMS runas group GID
: ${CMS_RUNAS_GID:="999999"}

## Plugins init (not implemented yet!!)
: ${CMS_PLUGINS_OSGI_OVERWRITE_SHARED:="false"}
: ${CMS_PLUGINS_OSGI_FIX_OWNER:="true"}

## Database config

# IP or hostname of database server
: ${PROVIDER_DB_DNSNAME:=""}

# Database type, one of ["H2","POSTGRES","MYSQL","ORACLE","MSSQL"]
: ${PROVIDER_DB_DRIVER:="POSTGRES"}

# JDBC-compliant URL to connect to DB (only needed to set custom options, PROVIDER_DB_DNSNAME & PROVIDER_DB_PORT must also be set or use defaults)
: ${PROVIDER_DB_URL:=""}

# Database tcp port number. If unset, will use default per database type
: ${PROVIDER_DB_PORT:=""} 

# Database username
: ${PROVIDER_DB_USERNAME:="dotcmsdbuser"}

# Database password
: ${PROVIDER_DB_PASSWORD:="password"}

# Maximum number of database connections
: ${PROVIDER_DB_MAXCONNS:="200"}



## Discovery note
#   The following are defined per-image upstream in the build Dockerfile:
#   - SERVICE_DELAY_DEFAULT_MIN
#   - SERVICE_DELAY_DEFAULT_STEP
#   - SERVICE_DELAY_DEFAULT_MAX


## Hazelcast config

# IP or hostname of external Hazelcast service (can be multi-valued RR DNS record). Leave unset for embedded Hazelcast
: ${PROVIDER_HAZELCAST_DNSNAMES:=""}
: ${PROVIDER_HAZELCAST_SVC_DELAY_MIN:="${SERVICE_DELAY_DEFAULT_MIN}"}
: ${PROVIDER_HAZELCAST_SVC_DELAY_STEP:="${SERVICE_DELAY_DEFAULT_STEP}"}
: ${PROVIDER_HAZELCAST_SVC_DELAY_MAX:="${SERVICE_DELAY_DEFAULT_MAX}"}
: ${PROVIDER_HAZELCAST_GROUPNAME:="dotCMS"}
: ${PROVIDER_HAZELCAST_GROUPPASSWORD:=""}
: ${PROVIDER_HAZELCAST_PORT:="5701"}



## Elasticsearch discovery

# IP or hostname of external Elasticsearch service (can be multi-valued RR DNS record). Leave unset for embedded Elasticsearch
: ${PROVIDER_ELASTICSEARCH_DNSNAMES:=""}
: ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MIN:="${SERVICE_DELAY_DEFAULT_MIN}"}
: ${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP:="${SERVICE_DELAY_DEFAULT_STEP}"}
: ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MAX:="${SERVICE_DELAY_DEFAULT_MAX}"}
# Background ES discovery refresh rate (in seconds)
: ${PROVIDER_ELASTICSEARCH_SVC_REFRESH:='10'}
: ${PROVIDER_ELASTICSEARCH_CLUSTER_NAME:="dotCMSContentIndex"}
: ${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT:="9300"}
: ${PROVIDER_ELASTICSEARCH_PORT_HTTP:="9200"}
# Elasticsearch HTTP server will be bound to localhost by default (this is not used by dotCMS). Change address binding to make accessible.
: ${PROVIDER_ELASTICSEARCH_ADDR_HTTP:="127.0.0.1"}
# Elasticsearch HTTP server is disabled by default. Set to "true" to enable.
: ${PROVIDER_ELASTICSEARCH_ENABLE_HTTP:="false"}


## Load balancer config

# Default PORT 
: ${PROVIDER_LOADBALANCER_DNSNAMES:=""}
: ${PROVIDER_LOADBALANCER_PORT_HTTP:="80"}
: ${PROVIDER_LOADBALANCER_SVC_DELAY_MIN:="${SERVICE_DELAY_DEFAULT_MIN}"}
: ${PROVIDER_LOADBALANCER_SVC_DELAY_STEP:="${SERVICE_DELAY_DEFAULT_STEP}"}
: ${PROVIDER_LOADBALANCER_SVC_DELAY_MAX:="${SERVICE_DELAY_DEFAULT_MAX}"}





