#!/bin/bash

set -e

sleep ${HAPROXY_SERVICE_DELAY_MIN}

echo "Trying discovery for ${HAPROXY_SERVICE_DELAY_MAX} seconds"
count=0
while [[ $count -le ${HAPROXY_SERVICE_DELAY_MAX} ]]; do

    server_count=$(discoverBackends "${CMS_DNSNAMES}")

    if [[ ${server_count} -gt 0 ]]; then

        break
    else
        echo "No backend servers found, waiting ${HAPROXY_SERVICE_DELAY_STEP} seconds..."
        sleep ${HAPROXY_SERVICE_DELAY_STEP}
        (( count+=${HAPROXY_SERVICE_DELAY_STEP} )) || :
    fi

done

echo "Initial discovery completed"
if [[  ${#server_count[@]} -eq 0 ]]; then
    echo "No backend servers found after ${HAPROXY_SERVICE_DELAY_MAX} seconds, continuing with background discovery"
fi

if [ -e "$HAPROXY_CERT_PATH" ]; then
    HAPROXY_TLS_ENABLE="true"
else
    HAPROXY_TLS_ENABLE="false"
fi

export CMS_PORT_HTTP
export CMS_PORT_HTTPS
export HAPROXY_TLS_ENABLE
export HAPROXY_ADMIN_PASSWORD
export HAPROXY_REDIRECT_HTTPS_ALL
export HAPROXY_MAINTENANCE_PAGE

export CMS_BACKEND_SERVERS=$(cat /srv/config/backend_members.txt |awk -vORS=, '{ print $1 }' | sed 's/,$//' )

cat /srv/config/backend_members.txt

dockerize -template /srv/templates/haproxy.cfg.tmpl:/srv/config/haproxy.cfg
dockerize -template /srv/templates/haproxy-backend-http.cfg.tmpl:/srv/config/haproxy-backend-http.cfg
dockerize -template /srv/templates/haproxy-backend-https.cfg.tmpl:/srv/config/haproxy-backend-https.cfg

