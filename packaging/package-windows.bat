@echo off
REM Build a Windows installer (.msi) for UnsubscribeOS.
REM Run this ON Windows. Requires: JDK 25 (with jpackage) + WiX Toolset v3 on PATH.

setlocal
set APP_NAME=UnsubscribeOS
set APP_VERSION=1.0.0
set MAIN_JAR=unsubscribeos-%APP_VERSION%.jar
set MAIN_CLASS=com.unsubscribeos.Main

REM 1. Build the fat jar with Windows JavaFX natives.
call mvn -Djavafx.platform=win clean package || exit /b 1

REM 2. Stage just the runnable jar in a clean input folder.
if exist target\app rmdir /s /q target\app
mkdir target\app
copy /Y target\%MAIN_JAR% target\app\ || exit /b 1

REM 3. Build the installer.
jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version %APP_VERSION% ^
  --vendor "UnsubscribeOS" ^
  --input target\app ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --icon packaging\windows\icon.ico ^
  --win-menu --win-shortcut --win-dir-chooser --win-per-user-install ^
  --dest dist || exit /b 1

echo.
echo Done. Installer is in:  dist\%APP_NAME%-%APP_VERSION%.msi
endlocal
