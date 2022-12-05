#!/bin/bash

set -e

source /srv/config.sh

if [[ "$1" == "haproxy" || -z "$1" ]]; then

    /srv/bg-discovery.sh &
    status=$?
    if [ $status -ne 0 ]; then
        echo "ERROR - failed to startup background discovery process: $status"
        exit $status
    fi

    echo "Starting HAProxy..."
    /bin/sh -c 'echo $$ >/srv/pidfile; exec \
        haproxy -W -db \
        -f /srv/config/haproxy.cfg \
        -f /srv/config/haproxy-backend-http.cfg \
        -f /srv/config/haproxy-backend-https.cfg'

elif [[ "$1" == "showconfig" ]]; then
    cat /srv/config/haproxy.cfg \
        /srv/config/haproxy-backend-http.cfg \
        /srv/config/haproxy-backend-https.cfg

else

    echo "Running user CMD..."
    exec -- "$@"
fi
