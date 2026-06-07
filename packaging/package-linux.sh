#!/usr/bin/env bash
# Build a Linux installer (.deb) for UnsubscribeOS.
# Run this ON Linux. Requires: JDK 25 (with jpackage) + dpkg (for .deb) or rpmbuild (use --type rpm).
set -euo pipefail

APP_NAME="unsubscribeos"
APP_VERSION="1.0.0"
MAIN_JAR="unsubscribeos-${APP_VERSION}.jar"
MAIN_CLASS="com.unsubscribeos.Main"

# 1. Build the fat jar with Linux JavaFX natives.
mvn -Djavafx.platform=linux clean package

# 2. Stage just the runnable jar in a clean input folder.
rm -rf target/app && mkdir -p target/app
cp "target/${MAIN_JAR}" target/app/

# 3. Build the installer.
jpackage \
  --type deb \
  --name "${APP_NAME}" \
  --app-version "${APP_VERSION}" \
  --vendor "UnsubscribeOS" \
  --input target/app \
  --main-jar "${MAIN_JAR}" \
  --main-class "${MAIN_CLASS}" \
  --icon packaging/linux/icon.png \
  --linux-shortcut --linux-menu-group "Utility" \
  --dest dist

echo
echo "Done. Installer is in: dist/"
ls -1 dist
