#!/bin/bash

set -e

if [[ ! -z "${ES_NETWORK_PUBLISH_HOST}" ]]; then
    if  grep -q "# Network Publish Host" "/srv/config/elasticsearch.yml"; then
        echo "ES_NETWORK_PUBLISH_HOST already found"
    else
        echo "Add ES_NETWORK_PUBLISH_HOST"
        echo "" >> /srv/config/elasticsearch.yml
        echo "# Network Publish Host" >> /srv/config/elasticsearch.yml
        echo "" >> /srv/config/elasticsearch.yml
        echo "network.publish_host: $ES_NETWORK_PUBLISH_HOST" >> /srv/config/elasticsearch.yml
        echo "" >> /srv/config/elasticsearch.yml
    fi
fi

bin/elasticsearch

