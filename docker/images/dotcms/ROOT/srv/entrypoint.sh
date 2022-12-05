#!/bin/bash

set -e

umask 007 

source /srv/config.sh

if [[ "${1}" == "dotcms" || -z "${1}" ]]; then

    if [[ -z "${PROVIDER_ELASTICSEARCH_DNSNAMES}" ]]; then
        echo "No external Elasticsearch configured..."
    else
        echo "Starting background Elasticsearch discovery..."
        /srv/utils/es-bg-discovery.sh &
    fi

    echo "Starting dotCMS ..."

    [[ -f /srv/TOMCAT_VERSION ]] && TOMCAT_VERSION=$( cat /srv/TOMCAT_VERSION )
    TOMCAT_HOME=/srv/dotserver/tomcat-${TOMCAT_VERSION}

    export CATALINA_PID="/tmp/dotcms.pid"
    if [ -e "$CATALINA_PID" ]; then
            echo
            echo "Pid file $CATALINA_PID exists! Are you sure dotCMS is not running?"
            echo
            exit 1
    fi

    cd /srv/home

    DB_CONNECT_TEST="$(cat /tmp/DB_CONNECT_TEST | tr -d [:space:])"
    if [[ -n "$DB_CONNECT_TEST" ]]; then
        exec -- \
        /usr/local/bin/dockerize -wait tcp://${DB_CONNECT_TEST} -timeout 60s \
         ${TOMCAT_HOME}/bin/catalina.sh run
    else
        exec -- \
        /usr/local/bin/dockerize \
         ${TOMCAT_HOME}/bin/catalina.sh run
    fi

else

    echo "Running user CMD..."
    exec -- "$@"
fi