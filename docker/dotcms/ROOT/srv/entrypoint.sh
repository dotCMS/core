#!/bin/bash

set -e

umask 007

source /srv/00-config-defaults.sh
source /srv/95-custom-starter-zip.sh



echo "Starting dotCMS ..."
${TOMCAT_HOME}/bin/catalina.sh run
