#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

source /srv/05-jvm-config.sh
source /srv/10-hazelcast-config.sh
source /srv/99-dockerize-config.sh
