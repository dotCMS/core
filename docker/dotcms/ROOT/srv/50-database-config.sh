#!/bin/bash

set -e

source /srv/utils/config-defaults.sh


echo "Database config ...."
[[ -z "${PROVIDER_DB_DNSNAME}" ]] && PROVIDER_DB_DRIVER="POSTGRES"
PROVIDER_DB_DRIVER=$(echo $PROVIDER_DB_DRIVER | tr '[:lower:]' '[:upper:]')

case "$PROVIDER_DB_DRIVER" in 

    POSTGRES)
        : ${PROVIDER_DB_DBNAME:="dotcms"}
        [[ -z "$PROVIDER_DB_PORT" ]] && PROVIDER_DB_PORT="5432"
        ;;

    MYSQL)
        : ${PROVIDER_DB_DBNAME:="dotcms"}
        [[ -z "$PROVIDER_DB_PORT" ]] && PROVIDER_DB_PORT="3306"
        ;;

    ORACLE)
        : ${PROVIDER_DB_DBNAME:="XE"}
        [[ -z "$PROVIDER_DB_PORT" ]] && PROVIDER_DB_PORT="1521"
        ;;

    MSSQL)
        : ${PROVIDER_DB_DBNAME:="dotcms"}
        [[ -z "$PROVIDER_DB_PORT" ]] && PROVIDER_DB_PORT="1433"
        ;;

    *)
        echo "Invalid DB driver specified!!"
        exit 1

esac


touch /tmp/DB_CONNECT_TEST
[[ "$PROVIDER_DB_DRIVER" != "H2" ]] && echo "${PROVIDER_DB_DNSNAME}:${PROVIDER_DB_PORT}" >/tmp/DB_CONNECT_TEST


echo "PROVIDER_DB_DRIVER=${PROVIDER_DB_DRIVER}" >>/srv/config/settings.ini
[[ -n "$PROVIDER_DB_URL" ]] && echo "PROVIDER_DB_URL=${PROVIDER_DB_URL}" >>/srv/config/settings.ini
echo "PROVIDER_DB_DBNAME=${PROVIDER_DB_DBNAME}" >>/srv/config/settings.ini
echo "PROVIDER_DB_DNSNAME=${PROVIDER_DB_DNSNAME}" >>/srv/config/settings.ini
echo "PROVIDER_DB_PORT=${PROVIDER_DB_PORT}" >>/srv/config/settings.ini
echo "PROVIDER_DB_USERNAME=${PROVIDER_DB_USERNAME}" >>/srv/config/settings.ini
echo "PROVIDER_DB_PASSWORD=${PROVIDER_DB_PASSWORD}" >>/srv/config/settings.ini
echo "PROVIDER_DB_MAXCONNS=${PROVIDER_DB_MAXCONNS}" >>/srv/config/settings.ini




