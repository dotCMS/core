@ECHO OFF

REM Some constants
SET JARFILE=autoUpdater.jar
SET NEWFILE=autoUpdater.new
SET OLDFILE=autoUpdater.old

REM save the parameters for later
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs



echo %CMD_LINE_ARGS%|findstr /i "-url" >nul:
if %errorlevel%==1 goto :url_not_found
:url_not_found
set CMD_LINE_ARGS= ""%CMD_LINE_ARGS% -url http://www.dotcms.com:8080/servlets/upgrade2x""


echo %CMD_LINE_ARGS%|findstr /i "-home" >nul:
if %errorlevel%==1 goto :home_not_found
:home_not_found
SET CurrDir=%CD%
CD..
SET InstPath=%CD%
CD..
SET RootPath=%CD%
CD %CurrDir%
set CMD_LINE_ARGS= ""%CMD_LINE_ARGS% -home %RootPath%""

IF NOT EXIST "%JAVA_HOME%\bin\java.exe" GOTO nojava


REM Get directory where we need to run from
IF EXIST %JARFILE% SET PRGDIR=%CD%\bin
IF EXIST %PRGDIR%\%JARFILE% GOTO gotprg
IF EXIST %CD%\%JARFILE% SET PRGDIR=%CD%
IF EXIST %PRGDIR%\%JARFILE% GOTO gotprg

ECHO Couldn't find %JARFILE%. Aborting.
GOTO end


:gotprg
ECHO Running from: %PRGDIR%

REM Make sure we're clear to start
IF EXIST %PRGDIR%\%NEWFILE% GOTO newjar


GOTO run



:run
REM RUN
"%JAVA_HOME%\bin\java" -jar "%PRGDIR%\%JARFILE%" %CMD_LINE_ARGS%
IF EXIST "%PRGDIR%\%NEWFILE%" GOTO updatejar
GOTO end

:updatejar
ECHO %NEWFILE% exists, updating agent.
DEL "%PRGDIR%\%OLDFILE%"
COPY "%PRGDIR%\%JARFILE%" "%PRGDIR%\%OLDFILE%"
DEL "%PRGDIR%\%JARFILE%"
COPY "%PRGDIR%\%NEWFILE%" "%PRGDIR%\%JARFILE%"
DEL "%PRGDIR%\%NEWFILE%"
ECHO "autoUpdater upgraded, restarting process."
"%JAVA_HOME%\bin\java" -jar "%PRGDIR%\%JARFILE%" %CMD_LINE_ARGS%
GOTO end

:newjar
ECHO %NEWFILE% exists, it was most likely left by a failed attempt.  Please remove before starting again. Aborting.
GOTO end

:nojava
ECHO "JAVA_HOME needs to be set.  Aborting."
GOTO end

:end
