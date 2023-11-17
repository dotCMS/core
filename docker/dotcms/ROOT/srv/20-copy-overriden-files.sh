#!/bin/bash

set -e

cp /srv/OVERRIDE/tomcat/conf/* $TOMCAT_HOME/conf/
cp /srv/OVERRIDE/WEB-INF/web.xml $TOMCAT_HOME/webapps/ROOT/WEB-INF/web.xml
cp /srv/OVERRIDE/WEB-INF/log4j/* $TOMCAT_HOME/webapps/ROOT/WEB-INF/log4j/
