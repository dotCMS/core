@echo off

rem -----------------------------------------------------------------------------
rem Stop Script for the dotCMS Server
rem -----------------------------------------------------------------------------

if "%OS%" == "Windows_NT" setlocal

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
set "CATALINA_HOME=%CURRENT_DIR%.."
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
set "CATALINA_HOME=%CURRENT_DIR%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
echo The CATALINA_HOME environment variable is not defined correctly: %CATALINA_HOME%
echo This environment variable is needed to run this program
goto end
:okHome

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

cd %CATALINA_HOME%\bin
call "%EXECUTABLE%" stop %CMD_LINE_ARGS%
cd "%CURRENT_DIR%"

:end