#!/bin/bash
 
set -e

source /srv/utils/config-defaults.sh

echo "Installing osgi plugins for Tomcat ${TOMCAT_VERSION}"
if [[ $(find /plugins/osgi/ -mindepth 1 -maxdepth 1 -name *.jar | wc -l) -gt 0 ]]; then
    echo "Found $(find /plugins/osgi/ -mindepth 1 -maxdepth 1 -name *.jar | wc -l) plugins"
    cp /plugins/osgi/*.jar /data/shared/felix/load/
fi

echo "Installing static plugins with JAVA_HOME=${JAVA_HOME}"
echo `id`
if [[ -d /plugins/static ]]; then
	if [[ $(find /plugins/static/ -mindepth 1 -maxdepth 1 -type d | wc -l) -gt 0 ]]; then
	    echo "Found $(find /plugins/static/ -mindepth 1 -maxdepth 1 -type d | wc -l) plugins"
	    cat /srv/plugins/common.xml > /tmp/plugins-common.xml
	    cat /srv/plugins/plugins.xml > /tmp/plugins-plugins.xml

	    cp -r /plugins/static/* /srv/plugins/
	    cat /tmp/plugins-common.xml > /srv/plugins/common.xml 
	    cat /tmp/plugins-plugins.xml > /srv/plugins/plugins.xml 

	    cd /srv && ./bin/deploy-plugins.sh || exit 1
	fi    
fi

