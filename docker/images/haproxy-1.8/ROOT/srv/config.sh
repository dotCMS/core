#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh
source /srv/utils/haproxy-discover.sh

source /srv/10-haproxy-config.sh
