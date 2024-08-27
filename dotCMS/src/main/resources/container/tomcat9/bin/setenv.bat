@echo off

rem Set CATALINA_OPTS with multiple options
set "CATALINA_OPTS=%CATALINA_OPTS% -Dfile.encoding=UTF8"

set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.lang=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.io=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.lang.ref=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.net=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.nio=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.nio.charset=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.time=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.time.zone=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.util=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.util.concurrent=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.util.concurrent.locks=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.util.regex=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/javax.crypto.spec=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.management/javax.management=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/sun.nio.cs=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/sun.util.calendar=ALL-UNNAMED"
set "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/sun.util.locale=ALL-UNNAMED"

set "CATALINA_OPTS=%CATALINA_OPTS% -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"
set "CATALINA_OPTS=%CATALINA_OPTS% -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"
set "CATALINA_OPTS=%CATALINA_OPTS% -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"
set "CATALINA_OPTS=%CATALINA_OPTS% -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource"


rem Check if log4j2.configurationFile is already set
echo %CATALINA_OPTS% | findstr /C:"-Dlog4j2.configurationFile" >nul
if %errorlevel% neq 0 (
  echo Setting log4j2.configurationFile=%CATALINA_HOME%\webapps\ROOT\WEB-INF\log4j\log4j2.xml
  set "CATALINA_OPTS=%CATALINA_OPTS% -Dlog4j2.configurationFile=%CATALINA_HOME%\webapps\ROOT\WEB-INF\log4j\log4j2.xml"
) else (
  echo Log4j configuration already set
)

rem Check if Log4jContextSelector is already set
echo %CATALINA_OPTS% | findstr /C:"-DLog4jContextSelector" >nul
if %errorlevel% neq 0 (
  echo Setting Log4jContextSelector to org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector
  set "CATALINA_OPTS=%CATALINA_OPTS% -DLog4jContextSelector=org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector"
) else (
  echo Log4jContextSelector already set
)

rem Set the CLASSPATH
set "ADDITIONAL_CLASSPATH=%CATALINA_HOME%\log4j2\lib\*"
if "%CLASSPATH%" neq "" (
  set "CLASSPATH=%CLASSPATH%;%ADDITIONAL_CLASSPATH%"
) else (
  set "CLASSPATH=%ADDITIONAL_CLASSPATH%"
)

set CLASSPATH