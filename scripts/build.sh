#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD="$ROOT/build"
CLASSES="$BUILD/classes"
LIBS="$BUILD/libs"
VERSION="1.0.6"
JAR_NAME="enchanted-book-search-${VERSION}.jar"

rm -rf "$CLASSES" "$LIBS"
mkdir -p "$CLASSES" "$LIBS"

mapfile -t SOURCES < <(find "$ROOT/stubs/src" "$ROOT/src/main/java" -name '*.java' -print | sort)
javac --release 17 -d "$CLASSES" "${SOURCES[@]}"

# API stubs are compile-time only and must not be included in the mod JAR.
rm -rf "$CLASSES/mezz" "$CLASSES/net" "$CLASSES/dev"

cp -R "$ROOT/src/main/resources/." "$CLASSES/"
cp "$ROOT/LICENSE" "$CLASSES/LICENSE_enchantedbooksearch"

cat > "$BUILD/MANIFEST.MF" <<MANIFEST
Manifest-Version: 1.0
Implementation-Title: Enchanted Book Search
Implementation-Version: ${VERSION}
Created-By: Enchanted Book Search build script
MANIFEST

jar --create \
    --file "$LIBS/$JAR_NAME" \
    --manifest "$BUILD/MANIFEST.MF" \
    -C "$CLASSES" .

echo "Built $LIBS/$JAR_NAME"
