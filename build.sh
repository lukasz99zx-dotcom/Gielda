#!/usr/bin/env bash
# Builds and signs dist/cGielda-1.0.apk from the sources in app/.
#
# This project targets Android but is built WITHOUT Android Studio, Gradle's
# Android plugin or the official Android SDK, because those all require
# dl.google.com / maven.google.com which may not be reachable in restricted
# network environments. Instead it uses:
#   - com.google.android:android (Maven Central) as a compile-time android.jar
#   - the AOSP `dx` tool, built from source, to convert .class -> .dex
#   - REAndroid/APKEditor (+ ARSCLib) to assemble the manifest/resources/APK
#   - the JDK's own jarsigner to sign the APK (JAR/v1 signature scheme)
#
# The APK is always signed with the committed signing/cgielda-release.keystore
# so that every build - local or CI - produces the same signing identity.
# Without this, Android refuses to install an update over an existing copy
# ("app not installed - conflicts with an existing package") because it
# treats differently-signed builds as different apps.
#
# Requires: java (JDK 17+), javac, jarsigner, keytool, curl, unzip, git.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$ROOT/.build"
LIBS="$WORK/libs"
mkdir -p "$WORK" "$LIBS"

KEYSTORE="$ROOT/signing/cgielda-release.keystore"
KEYSTORE_PASS="cgielda123"
KEY_ALIAS="cgielda"
if [ ! -f "$KEYSTORE" ]; then
  echo "ERROR: missing $KEYSTORE - this must stay committed so all builds share one signing identity." >&2
  exit 1
fi

VERSION_CODE="$(cd "$ROOT" && git rev-list --count HEAD 2>/dev/null || echo 1)"
VERSION_NAME="1.0"
echo "== Version: versionCode=$VERSION_CODE versionName=$VERSION_NAME =="

echo "== Fetching build tools (first run only) =="

if [ ! -f "$LIBS/android.jar" ]; then
  curl -sSL -o "$LIBS/android.jar" \
    "https://repo.maven.apache.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar"
fi

if [ ! -f "$LIBS/APKEditor.jar" ]; then
  curl -sSL -o "$LIBS/APKEditor.jar" \
    "https://github.com/REAndroid/APKEditor/releases/download/V1.4.9/APKEditor-1.4.9.jar"
fi

if [ ! -d "$WORK/dx-src" ]; then
  curl -sSL -o "$WORK/dalvik.tar.gz" \
    "https://codeload.github.com/aosp-mirror/platform_dalvik/tar.gz/refs/heads/master"
  mkdir -p "$WORK/dx-src"
  tar -xzf "$WORK/dalvik.tar.gz" -C "$WORK/dx-src" --strip-components=1
fi

if [ ! -d "$WORK/dx-classes" ]; then
  echo "== Building dx (class -> dex compiler) from AOSP source =="
  mkdir -p "$WORK/dx-classes"
  find "$WORK/dx-src/dx/src" -name "*.java" > "$WORK/dx-sources.txt"
  javac -nowarn -d "$WORK/dx-classes" @"$WORK/dx-sources.txt"
fi

echo "== Compiling app sources =="
rm -rf "$WORK/classes"
mkdir -p "$WORK/classes"
javac --release 8 -encoding UTF-8 -cp "$LIBS/android.jar" -d "$WORK/classes" \
  "$ROOT"/app/src/pl/cgielda/app/*.java

echo "== Converting to classes.dex =="
java -cp "$WORK/dx-classes" com.android.dx.command.Main --dex \
  --output="$WORK/classes.dex" "$WORK/classes"

echo "== Generating app icon =="
mkdir -p "$WORK/icon-classes"
javac -d "$WORK/icon-classes" "$ROOT/tools/IconGen.java"
ICON_RES="$WORK/icon-res"
rm -rf "$ICON_RES"
java -cp "$WORK/icon-classes" IconGen "$ICON_RES"

echo "== Assembling unsigned APK =="
PROJ="$WORK/apkproj"
rm -rf "$PROJ"
mkdir -p "$PROJ/dex" "$PROJ/resources/package_1/res/values"
cp "$WORK/classes.dex" "$PROJ/dex/classes.dex"
sed -e "s/__VERSION_CODE__/$VERSION_CODE/" -e "s/__VERSION_NAME__/$VERSION_NAME/" \
  "$ROOT/app/AndroidManifest.xml" > "$PROJ/AndroidManifest.xml"
cat > "$PROJ/resources/package_1/res/values/public.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources package="pl.cgielda.app" id="0x7f">
  <public type="mipmap" name="ic_launcher" id="0x7f010000" />
</resources>
EOF
cp -r "$ICON_RES"/mipmap-* "$PROJ/resources/package_1/res/"

mkdir -p "$ROOT/dist"
java -jar "$LIBS/APKEditor.jar" b -t xml -i "$PROJ" \
  -o "$WORK/cGielda-unsigned.apk" -framework "$LIBS/android.jar" -f

echo "== Signing APK =="
cp "$WORK/cGielda-unsigned.apk" "$ROOT/dist/cGielda-1.0.apk"
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
  "$ROOT/dist/cGielda-1.0.apk" "$KEY_ALIAS"

echo "== Done: dist/cGielda-1.0.apk (versionCode=$VERSION_CODE) =="
