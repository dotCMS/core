#!/bin/bash

set -e

for path in \
    /data \
    /logs \
; do
    [[ ! -d "${path}" ]] && mkdir -p "${path}"
    #chown -R elasticsearch:elasticsearch "$path"
done

cd /srv

#chgrp -R elasticsearch ./config

