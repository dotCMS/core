@echo off

set "CURRENT_DIR=%cd%"
set "BUILD_HOME=%CURRENT_DIR%\.."

ant -buildfile %CURRENT_DIR%\build.xml custom-dist-war
