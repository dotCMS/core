#!/bin/bash

set -e

if [[ -e /srv/config.ini ]]; then
    for val in $(cat /srv/config.ini); do
        export ${val}
    done
fi

echo "Applying config:"
cat /srv/config.ini

rm /srv/config.ini

dockerize -template /srv/templates/hazelcast.xml.tmpl:/srv/hazelcast.xml
