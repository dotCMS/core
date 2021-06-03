#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

echo "Merge Docker Config ...."
[[ -f /srv/TOMCAT_VERSION ]] && TOMCAT_VERSION=$( cat /srv/TOMCAT_VERSION )
# Overwrite Tomcat files
cd /srv/templates/tomcat/OVERRIDE
[[ -f ./conf/server-${TOMCAT_VERSION}.xml ]] && \
  mv ./conf/server-${TOMCAT_VERSION}.xml ./conf/server.xml && \
  rm ./conf/server-*.xml
for OVERRIDEFILE in $(find . -type f); do
    OVERRIDE_FOLDER=$(dirname $OVERRIDEFILE)
    [[ ! -d "${TOMCAT_HOME}/${OVERRIDE_FOLDER}" ]] && mkdir -p "${TOMCAT_HOME}/${OVERRIDE_FOLDER}"
    cp "$OVERRIDEFILE" "${TOMCAT_HOME}/$OVERRIDEFILE"
    
    # TODO: Hazelcast session store
    #[[ "$(basename $file)" == "hazelcast-client.xml" ]] && cp $file /srv/bin/system/src-conf/

    # feed to Dockerize for templating
    echo "${TOMCAT_HOME}/$OVERRIDEFILE" >>/srv/config/templatable.txt

done

# Overwrite dotCMS app files
cd /srv/templates/dotcms/OVERRIDE
for OVERRIDEFILE in $(find . -type f); do
    echo $OVERRIDEFILE
    [[ ! -d "${TOMCAT_HOME}/webapps/ROOT/$(dirname $OVERRIDEFILE)" ]] && mkdir -p "${TOMCAT_HOME}/webapps/ROOT/$(dirname $OVERRIDEFILE)"

    if [[ "$(dirname $OVERRIDEFILE)" == "./WEB-INF/classes" ]]; then
        cp "$OVERRIDEFILE" /srv/bin/system/src-conf/"$(basename $OVERRIDEFILE)"
        echo "/srv/bin/system/src-conf/$(basename $OVERRIDEFILE)" >>/srv/config/templatable.txt
    fi

    cp "$OVERRIDEFILE" "${TOMCAT_HOME}/webapps/ROOT/$OVERRIDEFILE"

    # feed to Dockerize for templating
    echo "${TOMCAT_HOME}/webapps/ROOT/$OVERRIDEFILE" >>/srv/config/templatable.txt

done


# Merge dotCMS properties diffs
cd /srv/templates/dotcms/CONF
for MERGEFILE in $(find . -type f); do
    echo "Merging $MERGEFILE"
    RUNFILE="${TOMCAT_HOME}/webapps/ROOT/WEB-INF/classes/$(basename $MERGEFILE)"
    SRCFILE="/srv/bin/system/src-conf/$(basename $MERGEFILE)"

    for varname in $(grep -oP "^\K[[:alnum:]].*(?=\=)" "$MERGEFILE"); do
        escaped_varname=$(escapeRegexChars "$varname")
        echo "Resetting '$varname'"
        sed -ri "s/^(${escaped_varname})\s*=(.*)$/#\1=\2/" "$RUNFILE"
        sed -ri "s/^(${escaped_varname})\s*=(.*)$/#\1=\2/" "$SRCFILE"
    done
    sed -i 's/\\/\\\\/g' "$RUNFILE"
    sed -i 's/\\/\\\\/g' "$MERGEFILE"

    config_injection=$(<"$MERGEFILE")

    prefile=$(sed '/##\ BEGIN\ PLUGINS/Q' "$RUNFILE")
    postfile=$(sed -ne '/##\ BEGIN\ PLUGINS/,$ p' "$RUNFILE")
    echo -e "${prefile}\n${config_injection}\n\n\n${postfile}" >"$RUNFILE"

    prefile=$(sed '/##\ BEGIN\ PLUGINS/Q' "$SRCFILE")
    postfile=$(sed -ne '/##\ BEGIN\ PLUGINS/,$ p' "$SRCFILE")
    echo -e "${prefile}\n${config_injection}\n\n\n${postfile}" >"$SRCFILE"

    # feed to Dockerize for templating
    echo "$RUNFILE" >>/srv/config/templatable.txt

done


