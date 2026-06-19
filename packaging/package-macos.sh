#!/usr/bin/env bash
# Build a macOS installer (.dmg) for UnsubscribeOS.
# Run this ON macOS. Requires: JDK 25 (with jpackage). Build on each arch you target
# (Apple Silicon vs Intel) for a native runtime.
set -euo pipefail

APP_NAME="UnsubscribeOS"
APP_VERSION="1.0.0"
MAIN_JAR="unsubscribeos-${APP_VERSION}.jar"
MAIN_CLASS="com.unsubscribeos.Main"

# 1. Build the fat jar with macOS JavaFX natives.
mvn -Djavafx.platform=mac clean package

# 2. Stage just the runnable jar in a clean input folder.
rm -rf target/app && mkdir -p target/app
cp "target/${MAIN_JAR}" target/app/

# 3. (Optional) generate an .icns from the PNG if it isn't present:
#    mkdir icon.iconset && sips -z 512 512 ../src/main/resources/icon.png --out icon.iconset/icon_512x512.png
#    iconutil -c icns icon.iconset -o packaging/macos/icon.icns
# 4. Build the installer. Collect args in one (always non-empty) array and only append the icon
#    when it exists — expanding an *empty* array under `set -u` aborts on macOS's bash 3.2.
JPACKAGE_ARGS=(
  --type dmg
  --name "${APP_NAME}"
  --app-version "${APP_VERSION}"
  --vendor "UnsubscribeOS"
  --input target/app
  --main-jar "${MAIN_JAR}"
  --main-class "${MAIN_CLASS}"
  --dest dist
)
if [[ -f packaging/macos/icon.icns ]]; then JPACKAGE_ARGS+=(--icon packaging/macos/icon.icns); fi

jpackage "${JPACKAGE_ARGS[@]}"

echo
echo "Done. Installer is in: dist/"
ls -1 dist
