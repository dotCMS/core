#!/bin/bash

set -e

# If enabled, add the required config for Tomcat Session Management with Redis
sed -i "s/\${REDIS_SESSION_VALVE}/${REDIS_SESSION_VALVE}/g" $TOMCAT_HOME/conf/context.xml