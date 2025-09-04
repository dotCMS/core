#!/bin/bash

set -e

umask 007
export JAVA_HOME=/usr/lib/jvm/java
export PATH=$PATH:/usr/lib/jvm/java/bin
source /srv/00-config-defaults.sh
source /srv/20-copy-overriden-files.sh
source /srv/40-custom-starter-zip.sh


echo ""
echo "Starting dotCMS ..."
echo "-------------------"
echo ""

if [[ ! -z "${ES_NETWORK_PUBLISH_HOST}" ]]; then
    if  grep -q "# Network Publish Host" "/srv/dotserver/tomcat/webapps/ROOT/WEB-INF/elasticsearch/config/elasticsearch.yml"; then
        echo "ES_NETWORK_PUBLISH_HOST already found"
    else
        echo "Network Publish Host"
        echo "" >> /srv/dotserver/tomcat/webapps/ROOT/WEB-INF/elasticsearch/config/elasticsearch.yml
        echo "# Network Publish Host" >> /srv/dotserver/tomcat/webapps/ROOT/WEB-INF/elasticsearch/config/elasticsearch.yml
        echo "" >> /srv/dotserver/tomcat/webapps/ROOT/WEB-INF/elasticsearch/config/elasticsearch.yml
        echo "network.publish_host: $ES_NETWORK_PUBLISH_HOST" >> /srv/dotserver/tomcat/webapps/ROOT/WEB-INF/elasticsearch/config/elasticsearch.yml
        echo "" >> /srv/dotserver/tomcat/webapps/ROOT/WEB-INF/elasticsearch/config/elasticsearch.yml
        echo "" >> /srv/config/elasticsearch.yml
    fi
fi




[[ -n "${WAIT_FOR_DEPS}" ]] && echo "Waiting ${WAIT_FOR_DEPS} seconds for DotCMS dependencies to load..." && sleep ${WAIT_FOR_DEPS}
export MIMALLOC_SHOW_STATS=1
export LD_PRELOAD=/usr/lib64/libmimalloc.so.2
exec -- ${TOMCAT_HOME}/bin/catalina.sh run
