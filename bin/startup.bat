@echo off

rem -----------------------------------------------------------------------------
rem Start Script for the dotCMS Server
rem -----------------------------------------------------------------------------

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "CURRENT_DIR=%~dp0%"
) else (
  set CURRENT_DIR=.\
)

rem Read an optional configuration file.
if "x%RUN_CONF%" == "x" (
   set "RUN_CONF=%CURRENT_DIR%build.conf.bat"
)
if exist "%RUN_CONF%" (
   echo Calling "%RUN_CONF%"
   call "%RUN_CONF%" %*
) else (
    echo Config file not found "%RUN_CONF%"
)

rem Guess DOTCMS_HOME if not defined
if not "%DOTCMS_HOME%" == "" goto gotDotcmsHome
set "DOTCMS_HOME=%CURRENT_DIR%..\%HOME_FOLDER%"
if exist "%DOTCMS_HOME%" goto okDotcmsHome
set "DOTCMS_HOME=%CURRENT_DIR%\%HOME_FOLDER%"
cd "%CURRENT_DIR%"
:gotDotcmsHome
if exist "%DOTCMS_HOME%" goto okDotcmsHome
echo The DOTCMS_HOME environment variable is not defined correctly: %DOTCMS_HOME%
echo This environment variable is needed to run this program
goto end
:okDotcmsHome

rem Guess CATALINA_HOME if not defined
if not "%CATALINA_HOME%" == "" goto gotHome
set "CATALINA_HOME=%CURRENT_DIR%..\%SERVER_FOLDER%"
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
set "CATALINA_HOME=%CURRENT_DIR%\%SERVER_FOLDER%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
echo The CATALINA_HOME environment variable is not defined correctly: %CATALINA_HOME%
echo This environment variable is needed to run this program
goto end
:okHome

rem Java VM configuration options

if not "%JAVA_OPTS%" == "" goto noDefaultJavaOpts

rem set JAVA_OPTS=-Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -Xmx1G -Djava.endorsed.dirs=%DOTCMS_HOME%/WEB-INF/endorsed_libs -XX:+UseG1GC -javaagent:%DOTCMS_HOME%/WEB-INF/lib/byte-buddy-agent-1.6.12.jar
set JAVA_OPTS=%JAVA_OPTS% -Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -server -XX:+DisableExplicitGC -Dsun.jnu.encoding=UTF-8

rem Set Memory sizing
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxMetaspaceSize=512m -Xmx1G

rem Set GC opts
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC

set JAVA_OPTS=%JAVA_OPTS% -Djava.endorsed.dirs=%DOTCMS_HOME%/WEB-INF/endorsed_libs

rem Set agent opts
set JAVA_OPTS=%JAVA_OPTS% -javaagent:%DOTCMS_HOME%/WEB-INF/lib/byte-buddy-agent-1.6.12.jar

rem Uncomment the next line if you want to enable JMX
rem set JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote.port=7788 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.endorsed.dirs=$DOTCMS_HOME/WEB-INF/endorsed_libs
:noDefaultJavaOpts

if not ""%1"" == ""debug"" goto noDebug
set JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 %JAVA_OPTS%
:noDebug

set "EXECUTABLE=%CATALINA_HOME%\bin\catalina.bat"

rem Check that target executable exists

if exist "%EXECUTABLE%" goto okExec
echo Cannot find "%EXECUTABLE%"
echo This file is needed to run this program
goto end
:okExec


rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

rem Executing Tomcat
cd %CATALINA_HOME%\webapps\ROOT
call "%EXECUTABLE%" start %CMD_LINE_ARGS%
cd "%CURRENT_DIR%"

:end
