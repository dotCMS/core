#!/bin/bash

###
##   Configuration defaults for Docker container configuration and service discovery
##    Please do not alter this file, but set these as environment variables on the container launch if needed.
###

set -e

## Hazelcast config

: ${PROVIDER_HAZELCAST_HEAP_MIN:="128m"}
: ${PROVIDER_HAZELCAST_HEAP_MAX:="1024m"}

# IP or hostname of external Hazelcast service (can be multi-valued RR DNS record). Leave unset for standalone Hazelcast
: ${PROVIDER_HAZELCAST_DNSNAMES:=""}
: ${PROVIDER_HAZELCAST_SVC_DELAY_MIN:="${SERVICE_DELAY_DEFAULT_MIN}"}
: ${PROVIDER_HAZELCAST_SVC_DELAY_STEP:="${SERVICE_DELAY_DEFAULT_STEP}"}
: ${PROVIDER_HAZELCAST_SVC_DELAY_MAX:="${SERVICE_DELAY_DEFAULT_MAX}"}
: ${PROVIDER_HAZELCAST_GROUPNAME:="dotCMS"}
: ${PROVIDER_HAZELCAST_PORT:="5701"}
: ${PROVIDER_HAZELCAST_MANCENTER_URL:=""}

