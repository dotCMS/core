#!/bin/bash

###
##   Configuration defaults for Docker container configuration and service discovery
##    Please do not alter this file, but set these as environment variables on the container launch if needed.
###

set -e

## Discovery note
#   The following are defined per-image upstream in the build Dockerfile:
#   - SERVICE_DELAY_DEFAULT_MIN
#   - SERVICE_DELAY_DEFAULT_STEP
#   - SERVICE_DELAY_DEFAULT_MAX

## HAproxy config

: ${HAPROXY_SERVICE_DELAY_MIN:="${SERVICE_DELAY_DEFAULT_MIN}"}
: ${HAPROXY_SERVICE_DELAY_STEP:="${SERVICE_DELAY_DEFAULT_STEP}"}
: ${HAPROXY_SERVICE_DELAY_MAX:="${SERVICE_DELAY_DEFAULT_MAX}"}
: ${HAPROXY_SERVICE_REFRESH:='10'}


# IP or hostname of dotCMS service (should be multi-valued RR DNS record). 
: ${CMS_DNSNAMES:='dotcms'}
: ${CMS_PORT_HTTP:='8081'}
: ${CMS_PORT_HTTPS:='8082'}

# Path to HAproxy certificate(s). Cert file format must be concatenated subject cert + intermediates cert(s) + private key.
: ${HAPROXY_CERT_PATH:=''}
# HA proxy admin stats interface password. Username is 'admin', empty password string disables
: ${HAPROXY_ADMIN_PASSWORD:=''}
# Path to raw HTTP response for 503 Server Unavailable maintennce page. Empty string disables
: ${HAPROXY_MAINTENANCE_PAGE:=''}
# Redirect all http requests to https. Boolean ["true"|"false"]
: ${HAPROXY_REDIRECT_HTTPS_ALL:='false'}
