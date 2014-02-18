@ECHO OFF

REM Some constants
SET JARFILE=autoUpdater.jar
SET NEWFILE=autoUpdater.new
SET OLDFILE=autoUpdater.old

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

rem Guess AUTO_UPDATER_HOME if not defined
if not "%AUTO_UPDATER_HOME%" == "" goto gotHome
set "AUTO_UPDATER_HOME=%CURRENT_DIR%..\autoupdater"
if exist "%AUTO_UPDATER_HOME%\autoUpdater.jar" goto okHome
set "AUTO_UPDATER_HOME=%CURRENT_DIR%\autoupdater"
cd "%CURRENT_DIR%"
:gotHome
if exist "%AUTO_UPDATER_HOME%\autoUpdater.jar" goto okHome
echo The AUTO_UPDATER_HOME environment variable is not defined correctly: %AUTO_UPDATER_HOME%
echo This environment variable is needed to run this program
goto end
:okHome

REM save the parameters for later
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs


echo %CMD_LINE_ARGS%|findstr /i "url" >nul:
if %errorlevel%==1 goto :url_not_found
:url_not_found
set CMD_LINE_ARGS= ""%CMD_LINE_ARGS% -url http://www.dotcms.com/app/servlets/upgrade2x""


echo %CMD_LINE_ARGS%|findstr /i "home" >nul:
if %errorlevel%==1 goto :home_not_found
:home_not_found
:: Set the location of this batch file as the current folder
cd "%CURRENT_DIR%"
SET HOME_PATH=%CURRENT_DIR%..
cd "%HOME_PATH%"
SET HOME_PATH=%CD%
cd "%CURRENT_DIR%"
set CMD_LINE_ARGS= ""%CMD_LINE_ARGS% -home %HOME_PATH%""


echo %CMD_LINE_ARGS%|findstr /i "dotcms_home" >nul:
if %errorlevel%==1 goto :dotcms_home_not_found
:dotcms_home_not_found
set CMD_LINE_ARGS= ""%CMD_LINE_ARGS% -dotcms_home %HOME_FOLDER%""

IF NOT EXIST "%JAVA_HOME%\bin\java.exe" GOTO nojava


REM Get directory where we need to run from
SET PRGDIR=%CD%%CURRENT_DIR%
IF EXIST %AUTO_UPDATER_HOME%\%JARFILE% GOTO gotprg

ECHO Couldn't find %JARFILE%. Aborting.
GOTO end


:gotprg
ECHO Running from: %PRGDIR%

REM Make sure we're clear to start
IF EXIST %AUTO_UPDATER_HOME%\%NEWFILE% GOTO newjar


GOTO run



:run
REM RUN
"%JAVA_HOME%\bin\java" -jar "%AUTO_UPDATER_HOME%\%JARFILE%" %CMD_LINE_ARGS%
IF EXIST %AUTO_UPDATER_HOME%\%NEWFILE% GOTO updatejar
GOTO end

:updatejar
ECHO %NEWFILE% exists, updating agent.
DEL "%AUTO_UPDATER_HOME%\%OLDFILE%"
COPY "%AUTO_UPDATER_HOME%\%JARFILE%" "%AUTO_UPDATER_HOME%\%OLDFILE%"
DEL "%AUTO_UPDATER_HOME%\%JARFILE%"
COPY "%AUTO_UPDATER_HOME%\%NEWFILE%" "%AUTO_UPDATER_HOME%\%JARFILE%"
DEL "%AUTO_UPDATER_HOME%\%NEWFILE%"
ECHO "autoUpdater upgraded, restarting process."
"%JAVA_HOME%\bin\java" -jar "%AUTO_UPDATER_HOME%\%JARFILE%" %CMD_LINE_ARGS%
GOTO end

:newjar
ECHO %NEWFILE% exists, it was most likely left by a failed attempt.  Please remove before starting again. Aborting.
GOTO end

:nojava
ECHO "JAVA_HOME needs to be set.  Aborting."
GOTO end

:end
