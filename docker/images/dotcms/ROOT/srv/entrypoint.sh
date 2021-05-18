#!/bin/bash

set -e

umask 007

source /srv/config.sh

if [[ "${1}" == "dotcms" || -z "${1}" ]]; then

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

    if [ ! -z "${WAIT_DB_FOR}" ]; then
      echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
      echo "            Requested sleep of [${WAIT_DB_FOR}], waiting for the db?"
      echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
      echo ""
      sleep ${WAIT_DB_FOR}
    fi

    DB_CONNECT_TEST="$(cat /tmp/DB_CONNECT_TEST | tr -d [:space:])"
    echo "DB Connect Test: ${DB_CONNECT_TEST}"

    if [[ ${DEBUG} == true ]]; then
      export CATALINA_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000'
    fi

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