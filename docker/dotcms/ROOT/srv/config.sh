#!/bin/bash

set -e

source /srv/05-setup-app-storage.sh
source /srv/10-merge-docker-config.sh
source /srv/20-dotcms-environment.sh
source /srv/30-tomcat-config.sh
source /srv/50-database-config.sh
source /srv/60-hazelcast-config.sh
source /srv/70-elasticsearch-config.sh
source /srv/92-loadbalance-config.sh
source /srv/80-install-plugins.sh
source /srv/90-dockerize-config.sh
source /srv/91-custom-post-template.sh
source /srv/95-custom-starter-zip.sh
