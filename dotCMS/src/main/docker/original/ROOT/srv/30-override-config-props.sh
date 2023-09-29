#!/bin/bash

set -e

# If enabled, add the required config for Tomcat Session Management with Redis
sed -i "s,\${TOMCAT_REDIS_SESSION_CONFIG},${TOMCAT_REDIS_SESSION_CONFIG},g" $TOMCAT_HOME/conf/context.xml
