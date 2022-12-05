#!/bin/bash

set -e

echo "Starting background discovery..."

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

source /srv/utils/haproxy-discover.sh

sleep $HAPROXY_SERVICE_REFRESH

touch /srv/pidfile

if [ -e "$HAPROXY_CERT_PATH" ]; then
    HAPROXY_TLS_ENABLE=true
else
    HAPROXY_TLS_ENABLE=false
fi

export CMS_PORT_HTTP
export CMS_PORT_HTTPS
export HAPROXY_TLS_ENABLE
export HAPROXY_ADMIN_PASSWORD
export HAPROXY_REDIRECT_HTTPS_ALL

# Loop as long as HAProxy is running
while kill -s 0 $(cat /srv/pidfile) 2>/dev/null; do

    hash_orig=$(cat /srv/config/backend_members.txt |sort -u |md5sum |cut -f 1 -d ' ')

    server_count=$(discoverBackends "${CMS_DNSNAMES}")

    hash_new=$(cat /srv/config/backend_members.txt |sort -u |md5sum |cut -f 1 -d ' ')

    if [[ "${hash_orig}" != "${hash_new}" ]]; then
        echo "BACKEND MEMBERS CHANGED: ${server_count} members"
        cat /srv/config/backend_members.txt

        export CMS_BACKEND_SERVERS=$(cat /srv/config/backend_members.txt |awk -vORS=, '{ print $1 }' | sed 's/,$//' )

        dockerize -template /srv/templates/haproxy-backend-http.cfg.tmpl:/srv/config/haproxy-backend-http.cfg
        dockerize -template /srv/templates/haproxy-backend-https.cfg.tmpl:/srv/config/haproxy-backend-https.cfg

        pid=$(cat /srv/pidfile)
        kill -SIGUSR2 $pid

    fi

    sleep $HAPROXY_SERVICE_REFRESH
    
done

echo "ERROR: HAProxy pid not found!! Quitting"
