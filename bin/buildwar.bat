@echo off

if "%OS%" == "Windows_NT" setlocal

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "CURRENT_DIR=%~dp0%"
) else (
  set CURRENT_DIR=.\
)

set "BUILD_HOME=%CURRENT_DIR%.."

ant -buildfile %BUILD_HOME%\build.xml custom-dist-war
