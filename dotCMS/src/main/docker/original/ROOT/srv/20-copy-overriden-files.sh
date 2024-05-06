#!/bin/bash

set -e

cp /srv/OVERRIDE/tomcat/conf/*      $TOMCAT_HOME/conf/
cp /srv/OVERRIDE/WEB-INF/log4j/*    $TOMCAT_HOME/webapps/ROOT/WEB-INF/log4j/
