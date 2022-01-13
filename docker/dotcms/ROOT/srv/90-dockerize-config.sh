#!/bin/bash

set -e

echo "Dockerize config ...."
[[ -f /srv/TOMCAT_VERSION ]] && TOMCAT_VERSION=$( cat /srv/TOMCAT_VERSION )

find /srv/plugins -mindepth 2 -maxdepth 2 -type f -name config-templatable.txt -exec cat {} >>/srv/config/templatable.txt \;

for template in $( cat /srv/config/templatable.txt |sort -u ); do
    template_evaled=$( eval echo "${template}" )
    echo "Adding template \"${template_evaled}\""
    DOCKERIZE_TEMPLATES="${DOCKERIZE_TEMPLATES} -template ${template_evaled}:${template_evaled}"
done

if [[ -e /srv/config/settings.ini ]]; then
    while read -r val
    do
        export "${val}"
    done < "/srv/config/settings.ini"
fi

echo "Applying config:"
cat /srv/config/settings.ini | sed 's/^/  /g' | sed 's/PASSWORD=.*/PASSWORD=<hidden>/g'

rm /srv/config/settings.ini

dockerize ${DOCKERIZE_TEMPLATES}