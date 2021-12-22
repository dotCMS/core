#!/bin/bash

set -e

umask 007

source /srv/00-config-defaults.sh
source /srv/20-copy-overriden-files.sh
source /srv/95-custom-starter-zip.sh



echo "Starting dotCMS ..."
echo "___________________"
echo ""
set
echo ""


${TOMCAT_HOME}/bin/catalina.sh run
