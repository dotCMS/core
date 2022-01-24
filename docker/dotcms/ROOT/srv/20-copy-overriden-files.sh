#!/bin/bash

set -e

[[ -f /srv/TOMCAT_VERSION ]] && TOMCAT_VERSION=$( cat /srv/TOMCAT_VERSION )

cp /srv/OVERRIDE/tomcat/conf/*      $TOMCAT_HOME/conf/
mv $TOMCAT_HOME/conf/server-${TOMCAT_VERSION}.xml $TOMCAT_HOME/conf/server.xml && \
rm -f $TOMCAT_HOME/conf/server-*.xml
cp /srv/OVERRIDE/WEB-INF/log4j/*    $TOMCAT_HOME/webapps/ROOT/WEB-INF/log4j/