#!/bin/bash
    
set -e
    
source /srv/config.sh

if [[ "$1" == "hazelcast" || -z "$1" ]]; then
    echo "Starting Hazelcast..."
    cd /srv/ && ./server.sh

elif [[ "$1" == "showconfig" ]]; then
    cat /srv/hazelcast.xml

else
    echo "Running user CMD..."
    exec -- "$@"
fi
