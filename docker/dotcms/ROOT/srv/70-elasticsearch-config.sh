#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

echo "Elasticsearch Config ...."
if [[ -z "${PROVIDER_ELASTICSEARCH_DNSNAMES}" ]]; then

    echo "PROVIDER_ELASTICSEARCH_DNSNAMES=" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_CLUSTER_NAME=${PROVIDER_ELASTICSEARCH_CLUSTER_NAME}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT=127.0.0.1" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_PORT_TRANSPORT=${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ADDR_HTTP=${PROVIDER_ELASTICSEARCH_ADDR_HTTP}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_PORT_HTTP=${PROVIDER_ELASTICSEARCH_PORT_HTTP}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ENABLE_HTTP=${PROVIDER_ELASTICSEARCH_ENABLE_HTTP}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_AUTH_BASIC_USER=${PROVIDER_ELASTICSEARCH_AUTH_BASIC_USER}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_AUTH_BASIC_PASSWORD=${PROVIDER_ELASTICSEARCH_AUTH_BASIC_PASSWORD}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ENDPOINTS=${PROVIDER_ELASTICSEARCH_ENDPOINTS}" >>/srv/config/settings.ini
    echo "Elasticsearch configuration set"
fi
