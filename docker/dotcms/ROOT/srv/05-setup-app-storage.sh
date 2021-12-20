#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

[[ ! -d /data/local/dotsecure ]] && mkdir -p /data/local/dotsecure
[[ ! -d /data/local/felix ]] && mkdir -p /data/local/felix
[[ ! -d /data/shared/assets ]] && mkdir -p /data/shared/assets
[[ ! -d /data/shared/felix/load ]] && mkdir -p /data/shared/felix/load
[[ ! -d /data/shared/felix/undeployed ]] && mkdir -p /data/shared/felix/undeployed




mkdir -p /srv/home
