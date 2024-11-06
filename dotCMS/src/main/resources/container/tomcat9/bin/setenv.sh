#!/bin/sh

export CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF8"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.io=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.lang.ref=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.net=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.nio=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.nio.charset=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.time=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.time.zone=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util.concurrent=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util.concurrent.locks=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.util.regex=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/javax.crypto.spec=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.management/javax.management=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/sun.nio.cs=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/sun.util.calendar=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/sun.util.locale=ALL-UNNAMED"
export CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/jdk.internal.misc=ALL-UNNAMED"

export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource"

# Set Log4j properties if not already set
if echo "$CATALINA_OPTS" | grep -q '\-Dlog4j2\.configurationFile'; then
  echo "Log4j configuration already set"
else
  echo "Setting log4j2.configurationFile=$CATALINA_HOME/webapps/ROOT/WEB-INF/log4j/log4j2.xml"
  export CATALINA_OPTS="$CATALINA_OPTS -Dlog4j2.configurationFile=$CATALINA_HOME/webapps/ROOT/WEB-INF/log4j/log4j2.xml"
fi

if echo "$CATALINA_OPTS" | grep -q '\-DLog4jContextSelector'; then
  echo "Log4jContextSelector already set"
else
  echo "Setting Log4jContextSelector to org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector"
  export CATALINA_OPTS="$CATALINA_OPTS -DLog4jContextSelector=org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector"
fi

ADDITIONAL_CLASSPATH="$CATALINA_HOME/log4j2/lib/*"

# Set CLASSPATH with additional path if necessary
if [ -n "$CLASSPATH" ]; then
  CLASSPATH="$CLASSPATH:$ADDITIONAL_CLASSPATH"
else
  CLASSPATH="$ADDITIONAL_CLASSPATH"
fi


# CATALINA_OPTS: Used to pass options to the JVM running Tomcat. This script appends various options to CATALINA_OPTS to configure encoding, module access, XML parser implementations, and Log4j settings.
# GLOWROOT_ENABLED: If set to "true", the Glowroot agent is added to CATALINA_OPTS.
# GLOWROOT_CONF_DIR: Directory for Glowroot configuration files. Defaults to $CATALINA_HOME/glowroot/local-web if GLOWROOT_WEB_UI_ENABLED is "true", otherwise defaults to $GLOWROOT_SHARED_FOLDER.
# GLOWROOT_LOG_DIR: Directory for Glowroot log files. Defaults to $GLOWROOT_CONF_DIR/logs if not set.
# GLOWROOT_TMP_DIR: Directory for Glowroot temporary files. Defaults to $GLOWROOT_CONF_DIR/tmp if not set.
# GLOWROOT_DATA_DIR: Directory for Glowroot data files. Defaults to $GLOWROOT_SHARED_FOLDER/data if not set.
# GLOWROOT_AGENT_ID: If set, specifies the agent ID for Glowroot and enables multi-directory mode.
# GLOWROOT_COLLECTOR_ADDRESS: If set, specifies the collector address for Glowroot.

if [ -z "$CATALINA_TMPDIR" ]; then
      CATALINA_TMPDIR="$CATALINA_HOME/temp"
fi

add_glowroot_agent() {
    if ! echo "$CATALINA_OPTS" | grep -q '\-javaagent:.*glowroot\.jar'; then
        if [ "$GLOWROOT_ENABLED" = "true" ]; then
          echo "Adding Glowroot agent to CATALINA_OPTS"
          export CATALINA_OPTS="$CATALINA_OPTS -javaagent:$CATALINA_HOME/glowroot/glowroot.jar"

          export GLOWROOT_SHARED_FOLDER="/data/shared/glowroot"

          # Set GLOWROOT_CONF_DIR based on GLOWROOT_WEB_UI_ENABLED
          if [ -z "$GLOWROOT_CONF_DIR" ]; then
              GLOWROOT_CONF_DIR="$([ "$GLOWROOT_WEB_UI_ENABLED" = "true" ] && echo "$CATALINA_HOME/glowroot/local-web" || echo "$GLOWROOT_SHARED_FOLDER")"
          fi
          CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.conf.dir=$GLOWROOT_CONF_DIR"
          # We may want to modify these defaults
          CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.tmp.dir=${GLOWROOT_TMP_DIR:=$CATALINA_TMPDIR}"
          # Only used if when not using a collector
          CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.data.dir=${GLOWROOT_DATA_DIR:=$GLOWROOT_SHARED_FOLDER/data}"

          # Set GLOWROOT_AGENT_ID and enable multi-directory mode if defined
          [ -n "$GLOWROOT_AGENT_ID" ] && CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.agent.id=$GLOWROOT_AGENT_ID -Dglowroot.multi.dir=true"
          [ -n "$GLOWROOT_COLLECTOR_ADDRESS" ] && CATALINA_OPTS="$CATALINA_OPTS -Dglowroot.collector.address=$GLOWROOT_COLLECTOR_ADDRESS"

        fi
    else
      echo "Using Legacy Glowroot agent settings from CATALINA_OPTS"
    fi
}

# Run the function to add Glowroot agent settings to CATALINA_OPTS if enabled
add_glowroot_agent
export CATALINA_OPTS
export CLASSPATH