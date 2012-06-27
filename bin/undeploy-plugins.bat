@echo off

if "%OS%" == "Windows_NT" setlocal

rem Guess DOTCMS_HOME if not defined
set CURRENT_DIR=%cd%
if not "%DOTCMS_HOME%" == "" goto gotHome
set DOTCMS_HOME=%CURRENT_DIR%
if exist "%DOTCMS_HOME%\bin\startDotCMS.bat" goto okHome
cd ..
set DOTCMS_HOME=%cd%
cd %CURRENT_DIR%
:gotHome
if exist "%DOTCMS_HOME%\bin\startup.bat" goto okHome
echo The DOTCMS_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

rem Get standard environment variables
if exist "%DOTCMS_HOME%\bin\setenv.bat" call "%DOTCMS_HOME%\bin\setenv.bat"

echo Using DOTCMS_HOME:   %DOTCMS_HOME%
echo Using JAVA_HOME:       %JAVA_HOME%

cd "%DOTCMS_HOME%"
%JAVA_HOME%/bin/java -jar "%DOTCMS_HOME%\bin\ant\ant-launcher.jar" undeploy-plugins
cd "%CURRENT_DIR%"

