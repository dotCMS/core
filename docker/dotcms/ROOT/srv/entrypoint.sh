#!/bin/bash

set -e

umask 007


source /srv/20-dotcms-environment.sh
source /srv/50-database-config.sh
source /srv/60-hazelcast-config.sh
source /srv/70-elasticsearch-config.sh
source /srv/95-custom-starter-zip.sh



echo "Starting dotCMS ..."
${TOMCAT_HOME}/bin/catalina.sh run
