@echo off

rem -----------------------------------------------------------------------------
rem Stop Script for the dotCMS Server
rem -----------------------------------------------------------------------------

if "%OS%" == "Windows_NT" setlocal

rem Guess DOTCMS_HOME if not defined
set "CURRENT_DIR=%cd%"
if not "%DOTCMS_HOME%" == "" goto gotHome
set "DOTCMS_HOME=%CURRENT_DIR%"
if exist "%DOTCMS_HOME%\bin\startup.bat" goto okHome
cd ..
set "DOTCMS_HOME=%cd%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%DOTCMS_HOME%\bin\startup.bat" goto okHome
echo The DOTCMS_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

set "EXECUTABLE=%DOTCMS_HOME%\tomcat\bin\catalina.bat"

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

cd %DOTCMS_HOME%\tomcat\bin
call "%EXECUTABLE%" stop %CMD_LINE_ARGS%
cd "%CURRENT_DIR%"

:end