#!/bin/bash

set -e

source /srv/config.sh

if [[ "$1" == "elasticsearch" || -z "$1" ]]; then

    #echo "Starting background discovery..."
    #/srv/utils/es-bg-discovery.sh &

    echo "Starting Elasticsearch server..."
    touch /srv/INIT_STARTED_ELASTICSEARCH

    cd /srv && mkdir run
    /bin/sh -c 'echo $$ >/srv/pid; exec elasticsearch'

elif [[ "$1" == "showconfig" ]]; then

    echo "File jvm.options:"
    cat /srv/config/jvm.options

    echo "File elasticsearch.yml:"
    cat /srv/config/elasticsearch.yml

    echo "File unicast_hosts.txt:"
    cat /srv/config/discovery-file/unicast_hosts.txt

else

    echo "Running user CMD..."
    exec -- "$@"

fi
