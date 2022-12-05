#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

if [[ ! -d /data/shared/assets ]]; then
    mkdir -p /data/shared/assets
    #chown $CMS_RUNAS_UID:$CMS_RUNAS_GID /data/shared/assets
fi

if [[ ! -d /data/shared/felix/load ]]; then
    mkdir -p /data/shared/felix/load
    #chown $CMS_RUNAS_UID:$CMS_RUNAS_GID /data/shared/felix/load
fi

if [[ ! -d /data/shared/felix/undeployed ]]; then
    mkdir -p /data/shared/felix/undeployed
    #chown $CMS_RUNAS_UID:$CMS_RUNAS_GID /data/shared/felix/undeployed
fi

[[ ! -d  "${TOMCAT_HOME}/webapps/ROOT/WEB-INF/H2_DATABASE" ]] && mkdir -p "${TOMCAT_HOME}/webapps/ROOT/WEB-INF/H2_DATABASE"
[[ ! -d  "${TOMCAT_HOME}/temp" ]] && mkdir -p "${TOMCAT_HOME}/temp"
[[ ! -d  "${TOMCAT_HOME}/conf/Catalina" ]] && mkdir -p "${TOMCAT_HOME}/conf/Catalina"
[[ ! -d /data/local/esdata ]] && mkdir -p /data/local/esdata
[[ ! -d /data/local/dotsecure ]] && mkdir -p /data/local/dotsecure
[[ ! -d /data/local/felix ]] && mkdir -p /data/local/felix

mkdir -p /srv/home
