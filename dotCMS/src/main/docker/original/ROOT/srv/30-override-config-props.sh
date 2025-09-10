#!/bin/bash

set -e
if [[ "${TOMCAT_REDIS_SESSION_ENABLED}" == 'true' ]]; then
  export TOMCAT_REDIS_SESSION_CONFIG=${TOMCAT_REDIS_SESSION_CONFIG:-'<Valve className="com.dotcms.tomcat.redissessions.RedisSessionHandlerValve" /><Manager className="com.dotcms.tomcat.redissessions.RedisSessionManager" />'}
  echo "config=$TOMCAT_REDIS_SESSION_CONFIG"
fi
# If enabled, add the required config for Tomcat Session Management with Redis
sed -i "s,\${TOMCAT_REDIS_SESSION_CONFIG},${TOMCAT_REDIS_SESSION_CONFIG},g" $TOMCAT_HOME/conf/context.xml
