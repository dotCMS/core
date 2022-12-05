#!/bin/bash

###
##   Configuration defaults for Docker container configuration and service discovery
##    Please do not alter this file, but set these as environment variables on the container launch if needed.
###

set -e

: ${PROVIDER_ELASTICSEARCH_HEAP_SIZE:="1024m"}

# IP or hostname of external Elasticsearch service (can be multi-valued RR DNS record). Leave unset for standalone Elasticsearch
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
: ${PROVIDER_ELASTICSEARCH_ENABLE_HTTP:="true"}

: ${PROVIDER_ELASTICSEARCH_NODE_MASTER:="true"}
: ${PROVIDER_ELASTICSEARCH_NODE_DATA:="true"}

