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



export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"
export CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource"


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

if [ -n "$CLASSPATH" ]; then
  CLASSPATH="$CLASSPATH:$ADDITIONAL_CLASSPATH"
else
  CLASSPATH="$ADDITIONAL_CLASSPATH"
fi

export CLASSPATH

