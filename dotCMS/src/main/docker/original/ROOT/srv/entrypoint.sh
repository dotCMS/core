#!/bin/bash

set -e

umask 007

export TOMCAT_HOME=/srv/dotserver/tomcat

/srv/10-import-env.sh
exit_status=$?
# Check the value
if [ $exit_status -eq 13 ]; then
    echo "Import completed, init signing off"
    exit 0;
fi

source /srv/20-copy-overriden-files.sh
source /srv/25-generate-dev-ssl-cert.sh
source /srv/30-override-config-props.sh
source /srv/40-custom-starter-zip.sh
source /srv/50-load-dump-sql.sh


[[ -n "${WAIT_FOR_DEPS}" ]] && echo "Waiting ${WAIT_FOR_DEPS} seconds for DotCMS dependencies to load..." && sleep ${WAIT_FOR_DEPS}

if [[ "$#" -eq 0 || "$1" == "dotcms" ]]; then
  shift
  echo ""
  echo "Starting dotCMS ..."
  echo "-------------------"
  echo ""
  exec -- ${TOMCAT_HOME}/bin/catalina.sh run
else
  echo starting "$@"
  exec -- "$@"
fi
