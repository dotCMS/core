#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh
#source /srv/utils/es-bg-discovery.sh

source /srv/10-filesystem-config.sh
source /srv/20-jvm-config.sh
source /srv/50-elasticsearch-config.sh

