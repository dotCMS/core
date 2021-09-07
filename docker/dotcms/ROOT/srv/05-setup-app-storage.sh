#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

[[ ! -d /data/local/dotsecure ]] && mkdir -p /data/local/dotsecure
[[ ! -d /data/local/felix ]] && mkdir -p /data/local/felix
[[ ! -d /data/shared/assets ]] && mkdir -p /data/shared/assets
[[ ! -d /data/shared/felix/load ]] && mkdir -p /data/shared/felix/load
[[ ! -d /data/shared/felix/undeployed ]] && mkdir -p /data/shared/felix/undeployed

ASSET_USER_ID=$( stat -c %u /data/shared/assets )
FELIX_USER_ID=$( stat -c %u /data/shared/felix/load )



echo "found /data/shared/assets directory owner: $ASSET_USER_ID"
if [[ ! "$CMS_RUNAS_UID" -eq $ASSET_USER_ID ]]; then
    echo "Updating asset directory owner to $CMS_RUNAS_UID:$CMS_RUNAS_GID"
    chown $CMS_RUNAS_UID:$CMS_RUNAS_GID /data/shared/assets &
fi


if [[ ! "$CMS_RUNAS_UID" -eq $FELIX_USER_ID ]]; then
    echo "Updating felix directory owner to $CMS_RUNAS_UID:$CMS_RUNAS_GID"
    chown $CMS_RUNAS_UID:$CMS_RUNAS_GID /data/shared/felix/load &
    chown $CMS_RUNAS_UID:$CMS_RUNAS_GID /data/shared/felix/undeployed &
fi



mkdir -p /srv/home
